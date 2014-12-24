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
package de.cubeisland.engine.module.conomy.commands;

import java.util.List;

import org.bukkit.OfflinePlayer;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.conomy.Conomy;
import de.cubeisland.engine.module.conomy.account.Account;
import de.cubeisland.engine.module.conomy.account.ConomyManager;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Command(name = "eco", desc = "Administrative commands for Conomy")
public class EcoCommands extends CommandContainer
{
    private final Conomy module;
    private final ConomyManager manager;

    public EcoCommands(Conomy module)
    {
        super(module);
        this.module = module;
        this.manager = module.getManager();
    }

    @Command(alias = "grant", desc = "Gives money to one or all players.")
    @Params(positional = {@Param(label = "players", type = User.class, reader = List.class), // TODO static values "*"
                          @Param(label = "amount", type = Double.class)})
    @Flags(@Flag(longName = "online", name = "o"))
    public void give(CommandContext context)
    {
        Double amount = context.get(1);
        String format = manager.format(amount);
        if ("*".equals(context.getString(0)))
        {
            if (context.hasFlag("o"))
            {
                this.manager.transactionAllOnline(amount);
                context.sendTranslated(POSITIVE, "You gave {input#amount} to every online player!", format);
                return;
            }
            this.manager.transactionAll(true, false, amount);
            context.sendTranslated(POSITIVE, "You gave {input#amount} to every player!", format);
            return;
        }
        for (User user : context.<List<User>>get(0))
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
                continue;
            }
            if (this.manager.transaction(null, target, amount, true))
            {
                context.sendTranslated(POSITIVE, "You gave {input#amount} to {user}!", format, user.getName());
                if (!context.getSource().equals(user) && user.isOnline())
                {
                    user.sendTranslated(POSITIVE, "You were granted {input#amount}.", format);
                }
            }
            else
            {
                context.sendTranslated(NEGATIVE, "Could not give the money to {user}!", user);
            }
        }
    }

    @Command(alias = "remove", desc = "Takes money from given user")
    @Params(positional = {@Param(label= "players", type = User.class, reader = List.class), // TODO static values "*"
                          @Param(label = "amount", type = Double.class)})
    @Flags(@Flag(longName = "online", name = "o"))
    public void take(CommandContext context)
    {
        Double amount = context.get(1);
        String format = manager.format(amount);
        if ("*".equals(context.getString(0)))
        {
            if (context.hasFlag("o"))
            {
                this.manager.transactionAllOnline(-amount);
                context.sendTranslated(POSITIVE, "You took {input#amount} from every online player!", format);
                return;
            }
            this.manager.transactionAll(true, false, -amount);
            context.sendTranslated(POSITIVE, "You took {input#amount} from every player!", format);
            return;
        }
        for (User user : context.<List<User>>get(0))
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
                return;
            }
            this.manager.transaction(target, null, amount, true);
            context.sendTranslated(POSITIVE, "You took {input#amount} from {user}!", format, user);
            if (!context.getSource().equals(user) && user.isOnline())
            {
                user.sendTranslated(NEUTRAL, "Withdrew {input#amount} from your account.", format);
            }
        }
    }

    @Command(desc = "Reset the money from given user")
    @Params(positional = @Param(label = "players", type = User.class, reader = List.class)) // TODO static values "*"
    @Flags(@Flag(longName = "online", name = "o"))
    public void reset(CommandContext context)
    {
        if ("*".equals(context.getString(0)))
        {
            if (context.hasFlag("o"))
            {
                this.manager.setAllOnline(this.manager.getDefaultBalance());
                context.sendTranslated(POSITIVE, "You reset every online players' account!");
                return;
            }
            this.manager.setAll(true, false, this.manager.getDefaultBalance());
            context.sendTranslated(POSITIVE, "You reset every players' account!");
            return;
        }
        for (User user : context.<List<User>>get(0))
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
                return;
            }
            target.reset();
            String format = this.manager.format(this.manager.getDefaultBalance());
            context.sendTranslated(POSITIVE, "{user} account reset to {input#balance}!", user, format);
            if (!context.getSource().equals(user) && user.isOnline())
            {
                user.sendTranslated(NEUTRAL, "Your balance got reset to {input#balance}.", format);
            }
        }
    }

    @Command(desc = "Sets the money of a given player")
    @Params(positional = {@Param(label = "players", type = User.class, reader = List.class), // TODO static values "*"
                          @Param(label = "amount", type = Double.class)})
    @Flags(@Flag(longName = "online", name = "o"))
    public void set(CommandContext context)
    {
        Double amount = context.get(1);
        String format = this.manager.format(amount);
        if ("*".equals(context.getString(0)))
        {
            if (context.hasFlag("o"))
            {
                this.manager.setAllOnline(amount);
                context.sendTranslated(POSITIVE, "You have set every online player account to {input#balance}!", format);
                return;
            }
            this.manager.setAll(true, false, amount);
            context.sendTranslated(POSITIVE, "You have set every player account to {input#balance}!", format);
            return;
        }
        for (User user : context.<List<User>>get(0))
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
                return;
            }
            target.set(amount);
            context.sendTranslated(POSITIVE, "{user} account set to {input#balance}!", user, format);
            if (!context.getSource().equals(user) && user.isOnline())
            {
                user.sendTranslated(NEUTRAL, "Your balance has been set to {input#balance}.", format);
            }
        }
    }

    @Command(desc = "Scales the money of a given player")
    @Params(positional = {@Param(label = "players", type = User.class, reader = List.class), // TODO static values "*"
                          @Param(label = "factor", type = Float.class)})
    @Flags(@Flag(longName = "online", name = "o"))
    public void scale(CommandContext context)
    {
        Float factor = context.get(1);
        if ("*".equals(context.getString(0)))
        {
            if (context.hasFlag("o"))
            {
                this.manager.scaleAllOnline(factor);
                context.sendTranslated(POSITIVE, "Scaled the balance of every online player by {decimal#factor}!", factor);
                return;
            }
            this.manager.scaleAll(true, false, factor);
            context.sendTranslated(POSITIVE, "Scaled the balance of every player by {decimal#factor}!", factor);
            return;
        }
        for (User user : context.<List<User>>get(0))
        {
            Account account = this.manager.getUserAccount(user, false);
            if (account == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
                return;
            }
            account.scale(factor);
            context.sendTranslated(POSITIVE, "Scaled the balance of {user} by {decimal#factor}!", user, factor);
        }
    }

    @Command(desc = "Hides the account of a given player")
    @Params(positional = @Param(label = "players", type = User.class, reader = List.class)) // TODO static values "*"
    public void hide(CommandContext context)
    {
        if ("*".equals(context.getString(0)))
        {
            this.manager.hideAll(true, false);
            return;
        }
        for (User user : context.<List<User>>get(0))
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
                return;
            }
            if (target.isHidden())
            {
                context.sendTranslated(NEUTRAL, "{user}'s account is already hidden!", user);
                return;
            }
            target.setHidden(true);
            context.sendTranslated(POSITIVE, "{user}'s account is now hidden!", user);
        }
    }

    @Command(desc = "Unhides the account of a given player")
    @Params(positional = @Param(label = "players", type = User.class, reader = List.class)) // TODO static values "*"
    public void unhide(CommandContext context)
    {
        if ("*".equals(context.getString(0)))
        {
            this.manager.unhideAll(true, false);
            return;
        }
        for (User user : context.<List<User>>get(0))
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
                return;
            }
            if (!target.isHidden())
            {
                context.sendTranslated(NEUTRAL, "{user}'s account was not hidden!", user);
                return;
            }
            target.setHidden(false);
            context.sendTranslated(POSITIVE, "{user}'s account is no longer hidden!", user);

        }
    }

    @Command(desc = "Deletes a users account.")
    @Params(positional = @Param(label = "player", type = User.class))
    public void delete(CommandContext context)
    {
        User user = context.get(0);
        if (this.manager.deleteUserAccount(user))
        {
            context.sendTranslated(POSITIVE, "Deleted the account of {user}", user);
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} did not have an account to delete!", user);
    }

    @Command(desc = "Creates a new account")
    @Params(positional = @Param(req = false, label = "player", type = OfflinePlayer.class))
    @Flags(@Flag(longName = "force", name = "f"))
    public void create(CommandContext context)
    {
        if (context.hasPositional(0))
        {
            if (!module.perms().ECO_CREATE_OTHER.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to create account for other users!");
                return;
            }
            OfflinePlayer oPlayer = context.get(0);
            if (!oPlayer.hasPlayedBefore() && !oPlayer.isOnline())
            {
                context.sendTranslated(NEUTRAL, "{user} has never played on this server!", context.get(0));
                if (!context.hasFlag("f"))
                {
                    return;
                }
                if (!module.perms().ECO_CREATE_FORCE.isAuthorized(context.getSource()))
                {
                    context.sendTranslated(POSITIVE, "Use the -force flag to create the account anyway.");
                    return;
                }
            }
            User user = this.module.getCore().getUserManager().getExactUser(oPlayer.getName());
            if (this.manager.getUserAccount(user, false) != null)
            {
                context.sendTranslated(POSITIVE, "{user} already has an account!", oPlayer);
                return;
            }
            this.manager.getUserAccount(user, true);
            context.sendTranslated(POSITIVE, "Created account for {user}!", oPlayer);
        }
        else if (context.getSource() instanceof User)
        {
            User sender = (User)context.getSource();
            if (this.manager.getUserAccount(sender, false) != null)
            {
                context.sendTranslated(NEGATIVE, "You already have an account!");
                return;
            }
            this.manager.getUserAccount(sender, true);
            context.sendTranslated(POSITIVE, "Your account has now been created!");
        }
    }
}
