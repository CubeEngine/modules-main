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
package de.cubeisland.engine.module.locker.commands;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.module.locker.Locker;
import de.cubeisland.engine.module.locker.commands.CommandListener.CommandType;
import de.cubeisland.engine.module.locker.storage.LockManager;

import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.locker.commands.CommandListener.CommandType.*;
import static de.cubeisland.engine.module.locker.commands.LockerCommands.isNotUser;

@Command(name = "create", desc = "Creates various protections")
public class LockerCreateCommands extends CommandContainer
{
    private final LockManager manager;

    public LockerCreateCommands(Locker module, LockManager manager)
    {
        super(module);
        this.manager = manager;
    }

    private void setCreateProtection(CommandSender sender, CommandType type, String password, boolean createKeyBook)
    {
        this.manager.commandListener.setCommandType(sender, type, password, createKeyBook);
        if (createKeyBook)
        {
            sender.sendTranslated(POSITIVE, "Right click the item you want to protect with a book in your hand!");
        }
        else
        {
            sender.sendTranslated(POSITIVE, "Right click the item you want to protect!");
        }
    }

    @Alias(names = "cprivate")
    @Command(name = "private", desc = "creates a private protection")
    @Params(positional = @Param(req = false, label = "password"))
    @Flags(@Flag(name = "key", longName = "keybook"))
    public void cPrivate(CommandContext context)
    {
        if (isNotUser(context.getSource())) return;
        this.setCreateProtection(context.getSource(), C_PRIVATE, context.getString(0), context.hasFlag("key"));
    }

    @Alias(names = "cpublic")
    @Command(name = "public", desc = "creates a public protection")
    public void cPublic(CommandContext context)
    {
        if (isNotUser(context.getSource())) return;
        this.setCreateProtection(context.getSource(), C_PUBLIC, null, false);
    }

    @Alias(names = "cdonation")
    @Command(name = "donation", desc = "creates a donation protection")
    @Params(positional = @Param(req = false, label = "password"))
    @Flags(@Flag(name = "key", longName = "keybook"))
    public void cDonation(CommandContext context)
    {
        if (isNotUser(context.getSource())) return;
        this.setCreateProtection(context.getSource(), C_DONATION, context.getString(0), context.hasFlag("key"));
    }

    @Alias(names = "cfree")
    @Command(name = "free", desc = "creates a free protection")
    @Params(positional = @Param(req = false, label = "password"))
    @Flags(@Flag(name = "key", longName = "keybook"))
    public void cFree(CommandContext context)
    {
        if (isNotUser(context.getSource())) return;
        this.setCreateProtection(context.getSource(), C_FREE, context.getString(0), context.hasFlag("key"));
    }

    @Alias(names = "cpassword")
    @Command(name = "password", desc = "creates a donation protection")
    @Params(positional = @Param(label = "password"))
    @Flags(@Flag(name = "key", longName = "keybook"))
    public void cPassword(CommandContext context) // same as private but with pw
    {
        if (isNotUser(context.getSource())) return;
        this.setCreateProtection(context.getSource(), C_PRIVATE, context.getString(0), context.hasFlag("key"));
    }

    @Alias(names = "cguarded")
    @Command(name = "guarded", desc = "creates a guarded protection")
    @Params(positional = @Param(req = false, label = "password"))
    @Flags(@Flag(name = "key", longName = "keybook"))
    public void cguarded(CommandContext context) // same as private but with pw
    {
        if (isNotUser(context.getSource())) return;
        this.setCreateProtection(context.getSource(), C_GUARDED, context.getString(0), context.hasFlag("key"));
    }
}
