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
package org.cubeengine.module.conomy.command;

import java.math.BigDecimal;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.ConomyService;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.TransactionResult;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Using(UniqueAccountParser.class)
@Command(name = "eco", desc = "Administrative commands for Conomy")
public class EcoCommand extends DispatcherCommand
{
    private final ConomyService service;
    private I18n i18n;

    public EcoCommand(ConomyService service, I18n i18n)
    {
        this.service = service;
        this.i18n = i18n;
    }

    @Command(alias = "grant", desc = "Gives money to one or all players.")
    public void give(CommandCause context, Collection<User> users, Double amount)
    {
        for (User user : users)
        {
            UniqueAccount target = getAccount(context, user);
            if (target != null)
            {
                Currency cur = service.defaultCurrency();
                TransactionResult result = target.deposit(cur, new BigDecimal(amount));
                Component formatAmount = cur.format(result.amount());
                switch (result.result())
                {
                    case SUCCESS:

                        i18n.send(context, POSITIVE, "You gave {txt} to {user}!", formatAmount, user.name());
                        if (!user.isOnline() && context.subject().equals(user.player().get()))
                        {
                            i18n.send(user.player().get(), POSITIVE, "You were granted {txt}.", formatAmount);
                        }
                        break;
                    default:
                        i18n.send(context, NEGATIVE, "Could not give the money to {user}!", user);
                        break;
                }
            }
        }
    }

    private BaseAccount.Unique getAccount(CommandCause context, User user)
    {
        BaseAccount.Unique target = service.orCreateAccount(user.uniqueId())
                .filter(a -> a instanceof BaseAccount.Unique)
                .map(BaseAccount.Unique.class::cast).orElse(null);
        if (target == null)
        {
            i18n.send(context, NEGATIVE, "{user} does not have an account!", user);
        }
        return target;
    }

    @Command(alias = "remove", desc = "Takes money from given user")
    public void take(CommandCause context, Collection<User> users, Double amount)
    {
        for (User user : users)
        {
            UniqueAccount target = getAccount(context, user);
            if (target != null)
            {
                Currency cur = service.defaultCurrency();
                TransactionResult result = target.withdraw(cur, new BigDecimal(amount));
                Component formatAmount = cur.format(result.amount());
                switch (result.result())
                {
                    case SUCCESS:
                        i18n.send(context, POSITIVE, "You took {txt} from {user}!", formatAmount, user);
                        if (!user.isOnline() && context.subject().equals(user.player().get()))
                        {
                            i18n.send(user.player().get(), NEUTRAL, "Withdrew {txt} from your account.", formatAmount);
                        }
                        break;
                    default:
                        i18n.send(context, NEGATIVE, "Could not take the money from {user}!", user);
                        break;
                }
            }
        }
    }

    @Command(desc = "Reset the money from given user")
    public void reset(CommandCause context, Collection<User> users)
    {
        for (User user : users)
        {
            UniqueAccount target = getAccount(context, user);
            if (target != null)
            {
                Currency cur = service.defaultCurrency();
                TransactionResult result = target.resetBalance(cur);
                Component formatAmount = cur.format(result.amount());
                switch (result.result())
                {
                    case SUCCESS:
                        i18n.send(context, POSITIVE, "{user} account reset to {txt}!", user, formatAmount);
                        if (!user.isOnline() && context.subject().equals(user.player().get()))
                        {
                            i18n.send(user.player().get(), NEUTRAL, "Your balance got reset to {txt}.", formatAmount);
                        }
                        break;
                    default:
                        i18n.send(context, NEGATIVE, "Could not reset the money from {user}!", user);
                        break;
                }
            }
        }
    }

    @Command(desc = "Sets the money of a given player")
    public void set(CommandCause context, Collection<User> users, Double amount)
    {
        for (User user : users)
        {
            UniqueAccount target = getAccount(context, user);
            if (target != null)
            {
                Currency cur = service.defaultCurrency();
                TransactionResult result = target.setBalance(cur, new BigDecimal(amount));
                Component formatAmount = cur.format(result.amount());
                switch (result.result())
                {
                    case SUCCESS:
                        i18n.send(context, POSITIVE, "{user} account set to {txt}!", user, formatAmount);
                        if (!user.isOnline() && context.subject().equals(user.player().get()))
                        {
                            i18n.send(user.player().get(), NEUTRAL, "Your balance got set to {txt}.", formatAmount);
                        }
                        break;
                    default:
                        i18n.send(context, NEGATIVE, "Could not reset the money from {user}!", user);
                        break;
                }
            }
        }
    }

    @Command(desc = "Hides the account of a given player")
    public void hide(CommandCause context, Collection<User> users)
    {
        for (User user : users)
        {
            BaseAccount.Unique target = getAccount(context, user);
            if (target != null)
            {
                if (target.isHidden())
                {
                    i18n.send(context, NEUTRAL, "{user}'s account is already hidden!", user);
                }
                else
                {
                    target.setHidden(true);
                    i18n.send(context, POSITIVE, "{user}'s account is now hidden!", user);
                }
            }
        }
    }

    @Command(desc = "Unhides the account of a given player")
    public void unhide(CommandCause context, Collection<User> users)
    {
        for (User user : users)
        {
            BaseAccount.Unique target = getAccount(context, user);
            if (target != null)
            {
                if (target.isHidden())
                {
                    target.setHidden(false);
                    i18n.send(context, POSITIVE, "{user}'s account is no longer hidden!", user);
                }
                else
                {
                    i18n.send(context, NEUTRAL, "{user}'s account was not hidden!", user);
                }
            }
        }
    }

    //@Command(desc = "Deletes a users account.")
    // TODO public void delete(CommandSource context, User player)

    //TODO list currencies
}
