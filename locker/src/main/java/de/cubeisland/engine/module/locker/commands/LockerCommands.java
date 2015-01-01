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

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import de.cubeisland.engine.command.CommandInvocation;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.completer.Completer;
import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.module.locker.Locker;
import de.cubeisland.engine.module.locker.commands.CommandListener.CommandType;
import de.cubeisland.engine.module.locker.storage.KeyBook;
import de.cubeisland.engine.module.locker.storage.Lock;
import de.cubeisland.engine.module.locker.storage.LockManager;
import de.cubeisland.engine.module.locker.storage.ProtectionFlag;

import static de.cubeisland.engine.core.util.ChatFormat.GOLD;
import static de.cubeisland.engine.core.util.ChatFormat.GREY;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.locker.commands.CommandListener.CommandType.*;

@Command(name = "locker", desc = "Locker commands", alias = "l")
public class LockerCommands extends CommandContainer
{
    private final Locker module;
    final LockManager manager;

    public LockerCommands(Locker module, LockManager manager)
    {
        super(module);
        this.module = module;
        this.manager = manager;
    }

    @Alias(value = "cinfo")
    @Command(desc = "Shows information about a protection")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void info(CommandContext context, @Flag(longName = "persist", name = "p") boolean persist)
    {
        if (persist)
        {
            this.persist(context);
        }
        User user = (User)context.getSource();
        KeyBook keyBook = KeyBook.getKeyBook(((User)context.getSource()).getItemInHand(), (User)context.getSource(), this.module);
        if (keyBook != null)
        {
            Lock lock = this.manager.getLockById(keyBook.lockID);
            if (lock != null && keyBook.isValidFor(lock))
            {
                context.sendTranslated(POSITIVE, "The strong magic surrounding this KeyBook allows you to access the designated protection");
                if (lock.isBlockLock())
                {
                    Location loc = lock.getFirstLocation();
                    context.sendTranslated(POSITIVE, "The protection corresponding to this book is located at {vector} in {world}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getWorld());
                }
                else
                {
                    for (Entity entity : user.getWorld().getEntities())
                    {
                        if (entity.getUniqueId().equals(lock.getEntityUID()))
                        {
                            Location loc = entity.getLocation();
                            context.sendTranslated(POSITIVE, "The entity protection corresponding to this book is located at {vector} in {world}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getWorld());
                            return;
                        }
                    }
                    context.sendTranslated(POSITIVE, "Your magic is not strong enough to locate the corresponding entity protection!");
                }
            }
            else
            {
                context.sendTranslated(NEUTRAL, "As you inspect the KeyBook closer you realize that its magic power has disappeared!");
                keyBook.invalidate();
            }
            return;
        }
        manager.commandListener.setCommandType(context.getSource(), CommandType.INFO, null, false);
        context.sendTranslated(POSITIVE, "Right click to show protection-info");
    }

    @Alias(value = "cpersist")
    @Command(desc = "persists your last locker command")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void persist(CommandContext context)
    {
        if (this.manager.commandListener.persist((User)context.getSource()))
        {
            context.sendTranslated(POSITIVE, "Your commands will now persist!");
        }
        else
        {
            context.sendTranslated(POSITIVE, "Your commands will now no longer persist!");
        }
    }

    @Alias(value = "cremove")
    @Command(desc = "Shows information about a protection")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void remove(CommandContext context, @Flag(longName = "persist", name = "p") boolean persist)
    {
        if (persist)
        {
            this.persist(context);
        }
        this.manager.commandListener.setCommandType(context.getSource(), CommandType.REMOVE, null);
        context.sendTranslated(POSITIVE, "Right click a protection to remove it!");
    }

    @Alias(value = "cunlock")
    @Command(desc = "Unlocks a password protected chest")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void unlock(CommandContext context, @Label("password") String password, @Flag(longName = "persist", name = "p") boolean persist)
    {
        if (persist)
        {
            this.persist(context);
        }
        this.manager.commandListener.setCommandType(context.getSource(), CommandType.UNLOCK, password);
        context.sendTranslated(POSITIVE, "Right click to unlock a password protected chest!");
    }

    @Alias(value = "cmodify")
    @Command(desc = "adds or removes player from the accesslist")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    @Params(positional = @Param(label = "players"))
    @Flags({@Flag(name = "g", longName = "global"),
            @Flag(longName = "persist", name = "p")})
    public void modify(CommandContext context) // TODO use parameterized cmd
    {
        if (context.hasFlag("p"))
        {
            this.persist(context);
        }
        String[] explode = StringUtils.explode(",", context.getString(0));
        for (String name : explode)
        {
            if (name.startsWith("@"))
            {
                name = name.substring(1);
            }
            if (name.startsWith("-"))
            {
                name = name.substring(1);
            }
            User user = this.module.getCore().getUserManager().findExactUser(name);
            if (user == null)
            {
                context.sendTranslated(NEGATIVE, "Player {user} not found!", name);
                return;
            }
        } // All users do exist!
        if (context.hasFlag("g"))
        {
            this.manager.setGlobalAccess((User)context.getSource(), context.getString(0));
        }
        else
        {
            this.manager.commandListener.setCommandType(context.getSource(), MODIFY, context.getString(0));
            context.sendTranslated(POSITIVE, "Right click a protection to modify it!");
        }
    }

    @Alias(value = "cgive")
    @Command(desc = "gives a protection to someone else")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void give(CommandContext context, @Label("player") User user, @Flag(longName = "persist", name = "p") boolean persist)
    {
        if (persist)
        {
            this.persist(context);
        }
        this.manager.commandListener.setCommandType(context.getSource(), GIVE, user.getName());
    }

    @Alias(value = "ckey")
    @Command(desc = "creates a KeyBook or invalidates previous KeyBooks")
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void key(CommandContext context, @Flag(longName = "invalidate", name = "i") boolean invalidate, @Flag(longName = "persist", name = "p") boolean persist)
    {
        if (!this.module.getConfig().allowKeyBooks)
        {
            context.sendTranslated(NEGATIVE, "KeyBooks are deactivated!");
            return;
        }
        if (persist)
        {
            this.persist(context);
        }
        if (invalidate)
        {
            this.manager.commandListener.setCommandType(context.getSource(), INVALIDATE_KEYS, context.getString(0));
            context.sendTranslated(POSITIVE, "Right click a protection to invalidate old KeyBooks for it!");
        }
        else
        {
            this.manager.commandListener.setCommandType(context.getSource(), KEYS, context.getString(0), true);
            context.sendTranslated(POSITIVE, "Right click a protection to with a book to create a new KeyBook!");
        }
    }

    @Alias(value = "cflag")
    @Command(desc = "Sets or unsets flags")
    @Params(nonpositional = {@Param(names = "set", label = "flags...", completer = FlagCompleter.class),
                             @Param(names = "unset", label = "flags...", completer = FlagCompleter.class)})
    @Flags(@Flag(longName = "persist", name = "p"))
    @Restricted(value = User.class, msg = "This command can only be used in game")
    public void flag(CommandContext context) // TODO use parameterized cmd
    {
        if (context.hasNamed())
        {
            context.sendTranslated(NEUTRAL, "You need to define which flags to {text:set} or {text:unset}!");
            context.sendTranslated(NEUTRAL, "The following flags are available:");
            String format = "  " + GREY + "-" + GOLD;
            for (String flag : ProtectionFlag.getNames())
            {
                context.sendMessage(format + flag);
            }
            context.sendTranslated(NEUTRAL, "You can also unset {text:all}");
            return;
        }
        if (context.hasFlag("p"))
        {
            this.persist(context);
        }
        if (context.hasNamed("set") && context.hasNamed("unSet"))
        {
            context.sendTranslated(NEGATIVE, "You have cannot set and unset flags at the same time!");
            return;
        }
        if (context.hasNamed("set"))
        {
            this.manager.commandListener.setCommandType(context.getSource(), CommandType.FLAGS_SET, context.getString("set"));
        }
        else
        {
            this.manager.commandListener.setCommandType(context.getSource(), CommandType.FLAGS_UNSET, context.getString("unset"));
        }
        context.sendTranslated(POSITIVE, "Right click a protection to change its flags!");
    }

    public static class FlagCompleter implements Completer
    {
        @Override
        public List<String> getSuggestions(CommandInvocation invocation)
        {
            String subToken = invocation.currentToken();
            if (subToken.contains(","))
            {
                subToken = subToken.substring(subToken.lastIndexOf(",") + 1);
            }
            return ProtectionFlag.getTabCompleteList(invocation.currentToken(), subToken);
        }
    }
}
