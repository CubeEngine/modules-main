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
package org.cubeengine.module.conomy.command;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.UserList;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;

import static org.cubeengine.service.i18n.formatter.MessageType.*;

@Command(name = "eco", desc = "Administrative commands for Conomy")
public class EcoCommand extends ContainerCommand
{
    private final Conomy module;
    private final ConomyService service;
    private I18n i18n;

    public EcoCommand(Conomy module, ConomyService service, I18n i18n)
    {
        super(module);
        this.module = module;
        this.service = service;
        this.i18n = i18n;
    }

    @Command(alias = "grant", desc = "Gives money to one or all players.")
    public void give(CommandSource context,
                     @Label("*|<players>") UserList users,
                     Double amount)
    {
        for (User user : users.list())
        {
            UniqueAccount target = getAccount(context, user);
            if (target != null)
            {
                Currency cur = service.getDefaultCurrency();
                TransactionResult result = target.deposit(cur, new BigDecimal(amount), causeOf(context));
                Text formatAmount = cur.format(result.getAmount());
                switch (result.getResult())
                {
                    case SUCCESS:

                        i18n.sendTranslated(context, POSITIVE, "You gave {txt} to {user}!", formatAmount, user.getName());
                        if (!context.equals(user) && user.isOnline())
                        {
                            i18n.sendTranslated(user.getPlayer().get(), POSITIVE, "You were granted {txt}.", formatAmount);
                        }
                        break;
                    default:
                        i18n.sendTranslated(context, NEGATIVE, "Could not give the money to {user}!", user);
                        break;
                }
            }
        }
    }

    private BaseAccount.Unique getAccount(CommandSource context, User user)
    {
        BaseAccount.Unique target = service.createAccount(user.getUniqueId())
                .filter(a -> a instanceof BaseAccount.Unique)
                .map(BaseAccount.Unique.class::cast).orElse(null);
        if (target == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "{user} does not have an account!", user);
        }
        return target;
    }

    @Command(alias = "remove", desc = "Takes money from given user")
    public void take(CommandSource context, @Label("*|<players>") UserList users, Double amount)
    {
        for (User user : users.list())
        {
            UniqueAccount target = getAccount(context, user);
            if (target != null)
            {
                Currency cur = service.getDefaultCurrency();
                TransactionResult result = target.withdraw(cur, new BigDecimal(amount), causeOf(context));
                Text formatAmount = cur.format(result.getAmount());
                switch (result.getResult())
                {
                    case SUCCESS:
                        i18n.sendTranslated(context, POSITIVE, "You took {txt} from {user}!", formatAmount, user);
                        if (!context.equals(user) && user.isOnline())
                        {
                            i18n.sendTranslated(user.getPlayer().get(), NEUTRAL, "Withdrew {txt} from your account.", formatAmount);
                        }
                        break;
                    default:
                        i18n.sendTranslated(context, NEGATIVE, "Could not take the money from {user}!", user);
                        break;
                }
            }
        }
    }

    private Cause causeOf(CommandSource context)
    {
        return Cause.of(NamedCause.source(context));
    }

    @Command(desc = "Reset the money from given user")
    public void reset(CommandSource context, @Label("*|<players>") UserList users)
    {
        for (User user : users.list())
        {
            UniqueAccount target = getAccount(context, user);
            if (target != null)
            {
                Currency cur = service.getDefaultCurrency();
                TransactionResult result = target.resetBalance(cur, causeOf(context));
                Text formatAmount = cur.format(result.getAmount());
                switch (result.getResult())
                {
                    case SUCCESS:
                        i18n.sendTranslated(context, POSITIVE, "{user} account reset to {txt}!", user, formatAmount);
                        if (!context.equals(user) && user.isOnline())
                        {
                            i18n.sendTranslated(user.getPlayer().get(), NEUTRAL, "Your balance got reset to {txt}.", formatAmount);
                        }
                        break;
                    default:
                        i18n.sendTranslated(context, NEGATIVE, "Could not reset the money from {user}!", user);
                        break;
                }
            }
        }
    }

    @Command(desc = "Sets the money of a given player")
    public void set(CommandSource context, @Label("*|<players>") UserList users, Double amount)
    {
        for (User user : users.list())
        {
            UniqueAccount target = getAccount(context, user);
            if (target != null)
            {
                Currency cur = service.getDefaultCurrency();
                TransactionResult result = target.setBalance(cur, new BigDecimal(amount), causeOf(context));
                Text formatAmount = cur.format(result.getAmount());
                switch (result.getResult())
                {
                    case SUCCESS:
                        i18n.sendTranslated(context, POSITIVE, "{user} account set to {txt}!", user, formatAmount);
                        if (!context.equals(user) && user.isOnline())
                        {
                            i18n.sendTranslated(user.getPlayer().get(), NEUTRAL, "Your balance got set to {txt}.", formatAmount);
                        }
                        break;
                    default:
                        i18n.sendTranslated(context, NEGATIVE, "Could not reset the money from {user}!", user);
                        break;
                }
            }
        }
    }

    @Command(desc = "Hides the account of a given player")
    public void hide(CommandSource context, @Label("*|<players>") UserList users)
    {
        for (User user : users.list())
        {
            BaseAccount.Unique target = getAccount(context, user);
            if (target != null)
            {
                if (target.isHidden())
                {
                    i18n.sendTranslated(context, NEUTRAL, "{user}'s account is already hidden!", user);
                }
                else
                {
                    target.setHidden(true);
                    i18n.sendTranslated(context, POSITIVE, "{user}'s account is now hidden!", user);
                }
            }
        }
    }

    @Command(desc = "Unhides the account of a given player")
    public void unhide(CommandSource context, @Label("*|<players>") UserList users)
    {
        for (User user : users.list())
        {
            BaseAccount.Unique target = getAccount(context, user);
            if (target != null)
            {
                if (target.isHidden())
                {
                    target.setHidden(false);
                    i18n.sendTranslated(context, POSITIVE, "{user}'s account is no longer hidden!", user);
                }
                else
                {
                    i18n.sendTranslated(context, NEUTRAL, "{user}'s account was not hidden!", user);
                }
            }
        }
    }

    //@Command(desc = "Deletes a users account.")
    // TODO public void delete(CommandSource context, User player)
}
