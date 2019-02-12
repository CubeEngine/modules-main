/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.sql.database.impl;

import static org.cubeengine.module.sql.PluginSql.SQL_ID;
import static org.cubeengine.module.sql.database.TableVersion.TABLE_VERSION;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.ModuleInjector;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.logging.LoggingUtil;
import org.cubeengine.libcube.util.Version;
import org.cubeengine.logscribe.Log;
import org.cubeengine.logscribe.LogFactory;
import org.cubeengine.logscribe.LogLevel;
import org.cubeengine.logscribe.LogTarget;
import org.cubeengine.logscribe.filter.PrefixFilter;
import org.cubeengine.logscribe.target.file.AsyncFileTarget;
import org.cubeengine.module.sql.database.*;
import org.cubeengine.reflect.Reflector;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SQLDialect;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.MappedTable;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.jooq.impl.DefaultVisitListenerProvider;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Singleton
public class SQLDatabase extends AbstractDatabase implements Database, ModuleInjector<ModuleTables>
{
    private final DatabaseConfiguration config;
    private DataSource dataSource;
    private ModuleManager mm;

    private Settings settings;
    private MappedSchema mappedSchema;
    private Log logger;
    private final JooqLogger jooqLogger = new JooqLogger(this);
    private Configuration jooqConfig;

    @Inject
    public SQLDatabase(Reflector reflector, ModuleManager mm, FileManager fm, LogFactory logFactory)
    {
        this.mm = mm;
        this.mm.registerBinding(Database.class, this);
        File pluginFolder = mm.getBasePath();

        // Disable HikariPool Debug ConsoleSpam
        ((Logger)LogManager.getLogger("com.zaxxer.hikari.pool.HikariPool")).setLevel(Level.INFO);
        ((Logger)LogManager.getLogger("com.zaxxer.hikari.pool.PoolBase")).setLevel(Level.INFO); // really? now pkg-private
        ((Logger)LogManager.getLogger("com.zaxxer.hikari.HikariConfig")).setLevel(Level.INFO);

        // Setting up Logger...
        this.logger = mm.getLoggerFor(Database.class);
        AsyncFileTarget target =
                new AsyncFileTarget.Builder(LoggingUtil.getLogFile(fm, "Database").toPath(),
                        LoggingUtil.getFileFormat(true, false)
                ).setAppend(true).setCycler(LoggingUtil.getCycler()).setThreadFactory(threadFactory).build();

        target.setLevel(LogLevel.DEBUG);
        logger.addTarget(target);


        LogTarget parentTarget = logger.addDelegate(logFactory.getLog(LogFactory.class));
        parentTarget.appendFilter(new PrefixFilter("[DB] "));
        parentTarget.setLevel(LogLevel.INFO);


        reflector.getDefaultConverterManager().registerConverter(new SQLDialectConverter(), SQLDialect.class);

        this.config = reflector.load(DatabaseConfiguration.class, new File(pluginFolder, "database.yml"));


    }

    @Override
    public void init()
    {
        // Now go connect to the database:
        this.logger.info("Connecting to the database...");

        SqlService service = Sponge.getServiceManager().provide(SqlService.class).get();
        String url = service.getConnectionUrlFromAlias(SQL_ID).orElse("jdbc:sqlite:cubeengine.db");

        try
        {
            this.dataSource = service.getDataSource(url);
        }
        catch (SQLException e)
        {
            logger.error("Could not establish connection with the database!");
            throw new IllegalStateException("Could not establish connection with the database!", e);
        }

        this.mappedSchema = new MappedSchema();
        this.settings = new Settings();
        this.settings.withRenderMapping(new RenderMapping().withSchemata(this.mappedSchema));
        this.settings.setExecuteLogging(false);

        this.logger.info("connected!");

        this.registerTable(new TableVersion());

        mm.registerClassInjector(ModuleTables.class, this);
        mm.registerBinding(Database.class, this);
    }


    private boolean updateTableStructure(TableUpdateCreator updater)
    {
        Record1<String> result = getDSL().select(TABLE_VERSION.VERSION).from(TABLE_VERSION).where(TABLE_VERSION.NAME.eq(updater.getName())).fetchOne();
        if (result != null)
        {
            try
            {
                Version dbVersion = Version.fromString(result.value1());
                Version version = updater.getTableVersion();
                if (dbVersion.isNewerThan(version))
                {
                    logger.info("table-version is newer than expected! {}: {} expected version: {}", updater.getName(),
                            dbVersion.toString(), version.toString());
                }
                else if (dbVersion.isOlderThan(updater.getTableVersion()))
                {
                    logger.info("table-version is too old! Updating {} from {} to {}", updater.getName(),
                            dbVersion.toString(), version.toString());
                    updater.update(this, dbVersion);
                    DSLContext dsl = getDSL();// TODO .mergeInto(TABLE_VERSION).values(updater.getName(), version.toString());
                    dsl.transaction(cfg -> {
                        DSLContext ctx = DSL.using(cfg);
                        ctx.update(TABLE_VERSION).set(TABLE_VERSION.VERSION, version.toString()).where(TABLE_VERSION.NAME.eq(updater.getName())).execute();
                        ctx.insertInto(TABLE_VERSION).values(updater.getName(), version.toString()).onDuplicateKeyIgnore().execute();
                    });

                    logger.info("{} got updated to {}", updater.getName(), version.toString());
                }
                return true;
            }
            catch (SQLException e)
            {
                logger.warn(e, "Could not execute structure update for the table {}", updater.getName());
            }
        }
        return false;
    }

    /**
     * Creates or updates the table for given entity
     *
     * @param table
     */
    @Override
    public void registerTable(TableCreator<?> table)
    {
        initializeTable(table);
        final String name = table.getName();
        registerTableMapping(name);
        logger.debug("Database-Table {0} registered!", name);
    }

    private void registerTableMapping(String name)
    {
        for (final MappedTable mappedTable : this.mappedSchema.getTables())
        {
            if (name.equals(mappedTable.getInput()))
            {
                return;
            }
        }
        this.mappedSchema.withTables(new MappedTable().withInput(name).withOutput(getTablePrefix() + name));
    }

    protected void initializeTable(TableCreator<?> table)
    {
        if (table instanceof TableUpdateCreator && this.updateTableStructure((TableUpdateCreator)table))
        {
            return;
        }
        try
        {
            table.createTable(this);
        }
        catch (DataAccessException ex)
        {
            throw new IllegalStateException("Cannot create table " + table.getName(), ex);
        }
    }

    @Override
    public void registerTable(Class<? extends org.cubeengine.module.sql.database.Table<?>> clazz)
    {
        try
        {
            this.registerTable(clazz.newInstance());
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Unable to instantiate Table! " + clazz.getName(), e);
        }
    }

    @Override
    public void inject(Object instance, ModuleTables annotation) {
        for (Class<? extends Table<?>> table : annotation.value()) {
            this.registerTable(table);
        }
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return this.dataSource.getConnection();
    }

    @Override
    public DSLContext getDSL()
    {
        Configuration conf = getConfiguration();
        return DSL.using(conf);
    }

    private Configuration getConfiguration() {
        if (jooqConfig == null) {
            jooqConfig = new DefaultConfiguration()
                    .set(config.dialect)
                    .set(new DataSourceConnectionProvider(this.dataSource))
                    .set(new DefaultExecuteListenerProvider(jooqLogger))
                    .set(settings)
                    .set(new DefaultVisitListenerProvider(new TablePrefixer(getTablePrefix())));
        }
        return jooqConfig;
    }

    @Override
    // TODO call on close
    public void shutdown()
    {
        super.shutdown();
    }

    @Override
    public String getName()
    {
        return this.config.dialect.getName();
    }

    @Override
    public DatabaseConfiguration getDatabaseConfig()
    {
        return this.config;
    }

    @Override
    public String getTablePrefix()
    {
        return this.config.tablePrefix;
    }

    @Override
    public Log getLog()
    {
        return logger;
    }
}
