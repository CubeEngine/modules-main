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
package de.cubeisland.engine.module.conomy;

import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import de.cubeisland.engine.logscribe.LogFactory;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.conomy.account.BankAccount;
import de.cubeisland.engine.module.conomy.account.ConomyManager;
import de.cubeisland.engine.module.conomy.account.storage.TableAccount;
import de.cubeisland.engine.module.conomy.account.storage.TableBankAccess;
import de.cubeisland.engine.module.conomy.commands.BankCommands;
import de.cubeisland.engine.module.conomy.commands.BankReader;
import de.cubeisland.engine.module.conomy.commands.EcoBankCommands;
import de.cubeisland.engine.module.conomy.commands.EcoCommands;
import de.cubeisland.engine.module.conomy.commands.MoneyCommand;
import de.cubeisland.engine.service.filesystem.FileManager;
import de.cubeisland.engine.service.i18n.I18n;
import de.cubeisland.engine.service.command.CommandManager;
import de.cubeisland.engine.service.database.Database;
import de.cubeisland.engine.service.user.UserManager;

public class Conomy extends Module
{
    private ConomyConfiguration config;
    private ConomyManager manager;
    private ConomyPermissions perms;

    @Inject private Database db;
    @Inject private I18n i18n;
    @Inject private CommandManager cm;
    @Inject private ThreadFactory tf;
    @Inject private LogFactory lf;
    @Inject private FileManager fm;
    @Inject private UserManager um;

    @Enable
    public void onEnable()
    {
        db.registerTable(TableAccount.class);
        db.registerTable(TableBankAccess.class);

        this.config = fm.loadConfig(this, ConomyConfiguration.class);
        this.manager = new ConomyManager(this, tf, db, lf, fm, um);
        perms = new ConomyPermissions(this);

        i18n.getCompositor().registerMacro(new CurrencyFormatter(manager));

        cm.addCommand(new MoneyCommand(this, um));
        EcoCommands ecoCommands = new EcoCommands(this, um);
        cm.addCommand(ecoCommands);

        cm.getProviderManager().register(this, new BankReader(this.manager, i18n), BankAccount.class);
        cm.addCommand(new BankCommands(this, um));
        ecoCommands.addCommand(new EcoBankCommands(this, um));
    }

    public ConomyConfiguration getConfig()
    {
        return this.config;
    }

    public ConomyManager getManager()
    {
        return manager;
    }

    public ConomyPermissions perms()
    {
        return this.perms;
    }
}
