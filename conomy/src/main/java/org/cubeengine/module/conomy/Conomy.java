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
package org.cubeengine.module.conomy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.logging.LogProvider;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.cubeengine.module.conomy.bank.BankPermission;
import org.cubeengine.module.conomy.storage.TableAccount;
import org.cubeengine.module.conomy.storage.TableBalance;
import org.cubeengine.module.sql.database.Database;
import org.cubeengine.module.sql.database.ModuleTables;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.Command.Parameterized;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.plugin.PluginContainer;

import static org.cubeengine.module.sql.PluginSql.SQL_ID;
import static org.cubeengine.module.sql.PluginSql.SQL_VERSION;

@Singleton
@Module(dependencies = @Dependency(value = SQL_ID, version = SQL_VERSION))
@ModuleTables({TableAccount.class, TableBalance.class})
public class Conomy
{
    @ModuleConfig private ConomyConfiguration config;

    @Inject private Database db;
    @Inject private PermissionManager pm;
    @Inject private I18n i18n;
    @Inject private Reflector reflector;
    @Inject private LogProvider logProvider;
    @Inject private ModuleManager mm;
    @Inject private PluginContainer plugin;
    private Path modulePath;

    private ConomyService service;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        i18n.getCompositor().registerFormatter(new BaseAccountFormatter());
        this.modulePath = mm.getPathFor(Conomy.class);
        // TODO logging transactions / can be done via events
        // TODO logging new accounts not! workaround set start value using transaction

        // we're doing this via permissions

    }

    @Listener
    public void onRegisterCommands(RegisterCommandEvent<Parameterized> event)
    {
        service.registerCommands(event, mm, i18n);
    }

    @Listener
    public void onRegisterEconomyService(ProvideServiceEvent<EconomyService> event)
    {
        event.suggest(this::getService);
    }

    public ConomyConfiguration getConfig()
    {
        return this.config;
    }

    public ConomyService getService()
    {
        if (service == null)
        {
            Path curencyPath = modulePath.resolve("currencies");
            try
            {
                Files.createDirectories(curencyPath);
            }
            catch (IOException e)
            {
                throw new IllegalStateException(e);
            }
            if (config.enableBanks)
            {
                service = new BankConomyService(this, config, curencyPath, db, reflector, pm, plugin);
            }
            else
            {
                service = new ConomyService(this, config, curencyPath, db, reflector, pm, plugin);
            }
        }
        return service;
    }
}
