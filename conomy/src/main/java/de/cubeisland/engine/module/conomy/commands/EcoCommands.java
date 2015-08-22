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

import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.conomy.Conomy;
import de.cubeisland.engine.module.conomy.account.Account;
import de.cubeisland.engine.module.conomy.account.ConomyManager;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserList;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.data.manipulator.entity.JoinData;

@Command(name = "eco", desc = "Administrative commands for Conomy")
public class EcoCommands extends ContainerCommand
{
    private final Conomy module;
    private UserManager um;
    private final ConomyManager manager;

    public EcoCommands(Conomy module, UserManager um)
    {
        super(module);
        this.module = module;
        this.um = um;
        this.manager = module.getManager();
    }

    @Command(alias = "grant", desc = "Gives money to one or all players.")
    public void give(CommandContext context, @Label("*|<players>") UserList users, Double amount, @Flag boolean online)
    {
        if (users.isAll())
        {
            if (online)
            {
                this.manager.transactionAllOnline(amount);
                context.sendTranslated(POSITIVE, "You gave {currency} to every online player!", amount);
                return;
            }
            this.manager.transactionAll(true, false, amount);
            context.sendTranslated(POSITIVE, "You gave {currency} to every player!", amount);
            return;
        }

        for (User user : users.list())
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
            }
            else
            {
                if (!this.manager.transaction(null, target, amount, true))
                {
                    context.sendTranslated(NEGATIVE, "Could not give the money to {user}!", user);
                }
                else
                {
                    context.sendTranslated(POSITIVE, "You gave {currency} to {user}!", amount, user.getName());
                    if (!context.getSource().equals(user) && user.isOnline())
                    {
                        user.sendTranslated(POSITIVE, "You were granted {currency}.", amount);
                    }
                }
            }
        }
    }

    @Command(alias = "remove", desc = "Takes money from given user")
    public void take(CommandContext context, @Label("*|<players>") UserList users, Double amount, @Flag boolean online)
    {
        if (users.isAll())
        {
            if (online)
            {
                this.manager.transactionAllOnline(-amount);
                context.sendTranslated(POSITIVE, "You took {currency} from every online player!", amount);
                return;
            }
            this.manager.transactionAll(true, false, -amount);
            context.sendTranslated(POSITIVE, "You took {currency} from every player!", amount);
            return;
        }

        for (User user : users.list())
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
            }
            else
            {
                this.manager.transaction(target, null, amount, true);
                context.sendTranslated(POSITIVE, "You took {currency} from {user}!", amount, user);
                if (!context.getSource().equals(user) && user.isOnline())
                {
                    user.sendTranslated(NEUTRAL, "Withdrew {currency} from your account.", amount);
                }
            }
        }
    }

    @Command(desc = "Reset the money from given user")
    public void reset(CommandContext context, @Label("*|<players>") UserList users, @Flag boolean online)
    {
        if (users.isAll())
        {
            if (online)
            {
                this.manager.setAllOnline(this.manager.getDefaultBalance());
                context.sendTranslated(POSITIVE, "You reset every online players' account!");
                return;
            }
            this.manager.setAll(true, false, this.manager.getDefaultBalance());
            context.sendTranslated(POSITIVE, "You reset every players' account!");
            return;
        }

        for (User user : users.list())
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
            }
            else
            {
                target.reset();
                String format = this.manager.format(this.manager.getDefaultBalance());
                context.sendTranslated(POSITIVE, "{user} account reset to {input#balance}!", user, format);
                if (!context.getSource().equals(user) && user.isOnline())
                {
                    user.sendTranslated(NEUTRAL, "Your balance got reset to {input#balance}.", format);
                }
            }
        }
    }

    @Command(desc = "Sets the money of a given player")
    public void set(CommandContext context, @Label("*|<players>") UserList users, Double amount, @Flag boolean online)
    {
        if (users.isAll())
        {
            if (online)
            {
                this.manager.setAllOnline(amount);
                context.sendTranslated(POSITIVE, "You have set every online player account to {currency}!", amount);
                return;
            }
            this.manager.setAll(true, false, amount);
            context.sendTranslated(POSITIVE, "You have set every player account to {currency}!", amount);
            return;
        }

        for (User user : users.list())
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
            }
            else
            {
                target.set(amount);
                context.sendTranslated(POSITIVE, "{user} account set to {currency}!", user, amount);
                if (!context.getSource().equals(user) && user.isOnline())
                {
                    user.sendTranslated(NEUTRAL, "Your balance has been set to {currency}.", amount);
                }
            }
        }
    }

    @Command(desc = "Scales the money of a given player")
    public void scale(CommandContext context, @Label("*|<players>") UserList users, Float factor, @Flag boolean online)
    {
        if (users.isAll())
        {
            if (online)
            {
                this.manager.scaleAllOnline(factor);
                context.sendTranslated(POSITIVE, "Scaled the balance of every online player by {decimal#factor}!", factor);
                return;
            }
            this.manager.scaleAll(true, false, factor);
            context.sendTranslated(POSITIVE, "Scaled the balance of every player by {decimal#factor}!", factor);
            return;
        }

        for (User user : users.list())
        {
            Account account = this.manager.getUserAccount(user, false);
            if (account == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
            }
            else
            {
                account.scale(factor);
                context.sendTranslated(POSITIVE, "Scaled the balance of {user} by {decimal#factor}!", user, factor);
            }
        }
    }

    @Command(desc = "Hides the account of a given player")
    public void hide(CommandContext context, @Label("*|<players>") UserList users)
    {
        if (users.isAll())
        {
            this.manager.hideAll(true, false);
            return;
        }

        for (User user : users.list())
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
            }
            else if (target.isHidden())
            {
                context.sendTranslated(NEUTRAL, "{user}'s account is already hidden!", user);
            }
            else
            {
                target.setHidden(true);
                context.sendTranslated(POSITIVE, "{user}'s account is now hidden!", user);
            }
        }
    }

    @Command(desc = "Unhides the account of a given player")
    public void unhide(CommandContext context, @Label("*|<players>") UserList users)
    {
        if (users.isAll())
        {
            this.manager.unhideAll(true, false);
            return;
        }

        for (User user : users.list())
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
            }
            else if (target.isHidden())
            {
                target.setHidden(false);
                context.sendTranslated(POSITIVE, "{user}'s account is no longer hidden!", user);
            }
            else
            {
                context.sendTranslated(NEUTRAL, "{user}'s account was not hidden!", user);
            }
        }
    }

    @Command(desc = "Deletes a users account.")
    public void delete(CommandContext context, User player)
    {
        if (this.manager.deleteUserAccount(player))
        {
            context.sendTranslated(POSITIVE, "Deleted the account of {user}", player);
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} did not have an account to delete!", player);
    }

    @Command(desc = "Creates a new account")
    public void create(CommandContext context, @Optional org.spongepowered.api.entity.player.User player, @Flag boolean force)
    {
        if (player == null)
        {
            if (context.getSource() instanceof User)
            {
                User sender = (User)context.getSource();
                if (this.manager.getUserAccount(sender, false) != null)
                {
                    context.sendTranslated(NEGATIVE, "You already have an account!");
                    return;
                }
                this.manager.getUserAccount(sender, true);
                context.sendTranslated(POSITIVE, "Your account has now been created!");
                return;
            }
            context.sendTranslated(POSITIVE, "You have to provide a player!");
            return;
        }
        if (!module.perms().ECO_CREATE_OTHER.isAuthorized(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to create account for other users!");
            return;
        }
        if (!player.getData(JoinData.class).isPresent() && !player.isOnline())
        {
            context.sendTranslated(NEUTRAL, "{user} has never played on this server!", player);
            if (!force)
            {
                return;
            }
            if (!module.perms().ECO_CREATE_FORCE.isAuthorized(context.getSource()))
            {
                context.sendTranslated(POSITIVE, "Use the -force flag to create the account anyway.");
                return;
            }
        }
        User user = um.getExactUser(player.getName());
        if (this.manager.getUserAccount(user, false) != null)
        {
            context.sendTranslated(POSITIVE, "{user} already has an account!", player);
            return;
        }
        this.manager.getUserAccount(user, true);
        context.sendTranslated(POSITIVE, "Created account for {user}!", player);
    }
}
