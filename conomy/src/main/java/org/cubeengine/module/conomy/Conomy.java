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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cubeengine.logscribe.Log;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.logging.LogProvider;
import org.cubeengine.module.sql.database.Database;
import org.cubeengine.module.sql.database.ModuleTables;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.cubeengine.module.conomy.bank.BankPermission;
import org.cubeengine.module.conomy.storage.TableAccount;
import org.cubeengine.module.conomy.storage.TableBalance;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.service.economy.EconomyService;

@Singleton
@Module(dependencies = @Dependency("cubeengine-sql"))
@ModuleTables({TableAccount.class, TableBalance.class})
public class Conomy extends CubeEngineModule
{
    @ModuleConfig private ConomyConfiguration config;
    @Inject private ConomyPermission perms;

    @Inject private Database db;
    @Inject private PermissionManager pm;
    @Inject private I18n i18n;
    @Inject private CommandManager cm;
    @Inject private Reflector reflector;
    @Inject private LogProvider logProvider;
    @Inject private ModuleManager mm;
    private Path modulePath;

    private BankPermission bankPerms;
    private ConomyService service;
    private Log log;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        this.log = logProvider.getLogger(Conomy.class, "Conomy", true);
        i18n.getCompositor().registerFormatter(new BaseAccountFormatter());
        this.modulePath = mm.getPathFor(Conomy.class);
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
            service = new BankConomyService(this, config, curencyPath, db, reflector);
            bankPerms = new BankPermission(pm);
        }
        else
        {
            service = new ConomyService(this, config, curencyPath, db, reflector);
        }
        Sponge.getServiceManager().setProvider(this.mm.getPlugin(Conomy.class).get(), EconomyService.class, service);

        service.registerCommands(cm, i18n);

        // TODO logging transactions / can be done via events
        // TODO logging new accounts not! workaround set start value using transaction

        // we're doing this via permissions

    }

    public ConomyConfiguration getConfig()
    {
        return this.config;
    }

    public ConomyPermission perms()
    {
        return perms;
    }
    public BankPermission bankPerms()
    {
        return bankPerms;
    }

    public ConomyService getService()
    {
        return service;
    }

    public Log getLog() {
        return log;
    }
}
