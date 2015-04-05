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

import de.cubeisland.engine.butler.alias.Alias;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.locker.Locker;
import de.cubeisland.engine.module.locker.commands.CommandListener.CommandType;
import de.cubeisland.engine.module.locker.storage.LockManager;

import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.locker.commands.CommandListener.CommandType.*;

@Command(name = "create", desc = "Creates various protections")
public class LockerCreateCommands extends ContainerCommand
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

    @Alias(value = "cprivate")
    @Command(name = "private", desc = "creates a private protection")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void cPrivate(User context, @Optional String password, @Flag(name = "key", longName = "keybook") boolean keybook)
    {
        this.setCreateProtection(context, C_PRIVATE, password, keybook);
    }

    @Alias(value = "cpublic")
    @Command(name = "public", desc = "creates a public protection")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void cPublic(User context)
    {
        this.setCreateProtection(context, C_PUBLIC, null, false);
    }

    @Alias(value = "cdonation")
    @Command(name = "donation", desc = "creates a donation protection")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void cDonation(User context, @Optional String password, @Flag(name = "key", longName = "keybook") boolean keybook)
    {
        this.setCreateProtection(context, C_DONATION, password, keybook);
    }

    @Alias(value = "cfree")
    @Command(name = "free", desc = "creates a free protection")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void cFree(User context, @Optional String password, @Flag(name = "key", longName = "keybook") boolean keybook)
    {
        this.setCreateProtection(context, C_FREE, password, keybook);
    }

    @Alias(value = "cpassword")
    @Command(name = "password", desc = "creates a donation protection")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void cPassword(User context, String password, @Flag(name = "key", longName = "keybook") boolean keybook) // same as private but with pw
    {
        this.setCreateProtection(context, C_PRIVATE, password, keybook);
    }

    @Alias(value = "cguarded")
    @Command(name = "guarded", desc = "creates a guarded protection")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void cguarded(User context, @Optional String password, @Flag(name = "key", longName = "keybook") boolean keybook) // same as private but with pw
    {
        this.setCreateProtection(context, C_GUARDED, password, keybook);
    }
}
