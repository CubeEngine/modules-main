/**
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

import de.cubeisland.engine.logscribe.LogFactory;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.cubeengine.module.conomy.bank.BankPermission;
import org.cubeengine.module.conomy.storage.TableAccount;
import org.cubeengine.module.conomy.storage.TableBalance;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.economy.EconomyService;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.concurrent.ThreadFactory;

@ModuleInfo(name = "Conomy", description = "Economy API and basic commands")
public class Conomy extends Module
{
    private ConomyConfiguration config;

    @Inject private Database db;
    @Inject private I18n i18n;
    @Inject private CommandManager cm;
    @Inject private ThreadFactory tf;
    @Inject private LogFactory lf;
    @Inject private FileManager fm;
    @Inject private UserManager um;
    @Inject private Path modulePath;
    @Inject private Reflector reflector;
    @Inject private Game game;

    private ConomyPermission perms;
    private BankPermission bankPerms;
    private ConomyService service;

    @Enable
    public void onEnable()
    {
        db.registerTable(TableAccount.class);
        db.registerTable(TableBalance.class);

        ConomyConfiguration config = fm.loadConfig(this, ConomyConfiguration.class);
        if (config.enableBanks)
        {
            service = new BankConomyService(this, config, modulePath.resolve("currencies"), db, reflector);
        }
        else
        {
            service = new ConomyService(this, config, modulePath.resolve("currencies"), db, reflector);
        }
        Object plugin = game.getPluginManager().getPlugin("CubeEngine").get().getInstance().get();
        game.getServiceManager().setProvider(plugin, EconomyService.class, service);

        service.registerCommands(cm, i18n);


        // TODO logging transactions / can be done via events
        // TODO logging new accounts not! workaround set start value using transaction

        perms = new ConomyPermission(this);

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
}
