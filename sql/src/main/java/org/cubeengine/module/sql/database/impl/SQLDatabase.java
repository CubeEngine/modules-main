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

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.ModuleInjector;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.util.Version;
import org.cubeengine.module.sql.database.AbstractDatabase;
import org.cubeengine.module.sql.database.Database;
import org.cubeengine.module.sql.database.DatabaseConfiguration;
import org.cubeengine.module.sql.database.ModuleTables;
import org.cubeengine.module.sql.database.SQLDialectConverter;
import org.cubeengine.module.sql.database.Table;
import org.cubeengine.module.sql.database.TableCreator;
import org.cubeengine.module.sql.database.TableUpdateCreator;
import org.cubeengine.module.sql.database.TableVersion;
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
import org.spongepowered.api.sql.SqlManager;

import static org.cubeengine.module.sql.PluginSql.SQL_ID;
import static org.cubeengine.module.sql.database.TableVersion.TABLE_VERSION;

@Singleton
public class SQLDatabase extends AbstractDatabase implements Database, ModuleInjector<ModuleTables>
{
    private final DatabaseConfiguration config;
    private DataSource dataSource;
    private ModuleManager mm;
    private final Logger logger;

    private Settings settings;
    private MappedSchema mappedSchema;
    private final JooqLogger jooqLogger;
    private Configuration jooqConfig;

    @Inject
    public SQLDatabase(Reflector reflector, ModuleManager mm, Logger log, FileManager fm)
    {
        this.mm = mm;
        this.logger = log;
        this.jooqLogger = new JooqLogger(this, log);
        this.mm.registerBinding(Database.class, this);

        reflector.getDefaultConverterManager().registerConverter(new SQLDialectConverter(), SQLDialect.class);

        this.config = reflector.load(DatabaseConfiguration.class, fm.getDataPath().resolve("database.yml").toFile());


    }

    @Override
    public void init()
    {
        // Now go connect to the database:
        this.logger.info("Connecting to the database...");

        SqlManager manager = Sponge.sqlManager();
        String url = manager.connectionUrlFromAlias(SQL_ID).orElse("jdbc:sqlite:cubeengine.db");

        try
        {
            this.dataSource = manager.dataSource(url);
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
                logger.warn("Could not execute structure update for the table {}", updater.getName(), e);
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
}
