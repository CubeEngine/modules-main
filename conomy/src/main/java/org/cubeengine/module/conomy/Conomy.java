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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
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
import org.cubeengine.service.database.ModuleTables;
import org.cubeengine.service.filesystem.ModuleConfig;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.ModulePermissions;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.economy.EconomyService;

@ModuleInfo(name = "Conomy", description = "Economy API and basic commands")
@ModuleTables({TableAccount.class, TableBalance.class})
public class Conomy extends Module
{
    @ModuleConfig private ConomyConfiguration config;
    @ModulePermissions private ConomyPermission perms;

    @Inject private Database db;
    @Inject private I18n i18n;
    @Inject private CommandManager cm;
    @Inject private ThreadFactory tf;
    @Inject private LogFactory lf;
    @Inject private Path modulePath;
    @Inject private Reflector reflector;
    @Inject private Game game;

    private BankPermission bankPerms;
    private ConomyService service;

    @Enable
    public void onEnable()
    {
        i18n.getCompositor().registerFormatter(new BaseAccountFormatter());

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
            bankPerms = new BankPermission(this);
        }
        else
        {
            service = new ConomyService(this, config, curencyPath, db, reflector);
        }
        Object plugin = game.getPluginManager().getPlugin("org.cubeengine").get().getInstance().get();
        game.getServiceManager().setProvider(plugin, EconomyService.class, service);

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
}
