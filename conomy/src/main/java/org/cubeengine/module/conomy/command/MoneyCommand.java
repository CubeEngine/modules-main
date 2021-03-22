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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Delegate;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.ConfigCurrency;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.module.conomy.storage.BalanceModel;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.service.pagination.PaginationList;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
@Delegate("balance")
@Command(name = "money", desc = "Manage your money")
public class MoneyCommand extends DispatcherCommand
{
    // TODO add in context to commands
    private final Conomy module;
    private ConomyService service;
    private I18n i18n;

    @Inject
    public MoneyCommand(Conomy module, ConomyService conomy, I18n i18n)
    {
        this.module = module;
        this.service = conomy;
        this.i18n = i18n;
    }

    private BaseAccount.Unique getUserAccount(UUID user)
    {
        return service.accountOrCreate(user)
                .filter(a -> a instanceof BaseAccount.Unique)
                .map(BaseAccount.Unique.class::cast).orElse(null);
    }

    @Alias(value = "balance", alias = {"moneybalance", "pmoney"})
    @Command(desc = "Shows your balance")
    public void balance(CommandCause context, @Default BaseAccount.Unique account)
    {
        Map<Currency, BigDecimal> balances = account.balances();
        if (balances.isEmpty())
        {
            i18n.send(context, NEGATIVE, "No Balance for {account} found!", account);
            return;
        }
        i18n.send(context, POSITIVE, "{account}'s Balance:", account);
        for (Map.Entry<Currency, BigDecimal> entry : balances.entrySet())
        {
            context.sendMessage(Identity.nil(), entry.getKey().format(entry.getValue()).color(NamedTextColor.GOLD));
        }
    }

    @Alias(value = "toplist", alias = {"balancetop", "topmoney"})
    @Command(desc = "Shows the players with the highest balance.")
    public void top(CommandCause context, @Option @Label("[fromRank-]toRank") String range)
    {
        int fromRank = 1;
        int toRank = 10;
        if (range != null)
        {
            try
            {
                if (range.contains("-"))
                {
                    fromRank = Integer.parseInt(range.substring(0, range.indexOf("-")));
                    range = range.substring(range.indexOf("-") + 1);
                }
                toRank = Integer.parseInt(range);
            }
            catch (NumberFormatException e)
            {
                i18n.send(context, NEGATIVE, "Invalid rank!");
                return;
            }
        }
        Collection<BalanceModel> models = this.service.getTopBalance(true, false, fromRank, toRank, service.getPerms().ACCESS_SEE.check(context));
        int i = fromRank;


        PaginationList.Builder pagination = Sponge.game().serviceProvider().paginationService().builder();
        pagination.padding(Component.text("-"));
        if (fromRank == 1)
        {
            pagination.title(i18n.translate(context, POSITIVE, "Top Balance ({amount})", models.size()));
        }
        else
        {
            pagination.title(i18n.translate(context, POSITIVE, "Top Balance from {integer} to {integer}", fromRank, fromRank + models.size() - 1));
        }
        List<Component> texts = new ArrayList<>();
        for (BalanceModel balance : models)
        {
            Account account = service.accountOrCreate(balance.getAccountID()).get();
            ConfigCurrency currency = service.getCurrency(balance.getCurrency());
            if (currency == null)
            {
                texts.add(Component.text( "?" + balance.getCurrency() + "? : " + balance.getBalance(), CRITICAL));
            }
            else
            {
                texts.add(Component.text(i++).append(Component.text(" - ", NamedTextColor.WHITE))
                         .append(account.displayName().color(NamedTextColor.DARK_GREEN))
                         .append(Component.text(":", NamedTextColor.WHITE))
                         .append(Component.text(currency.fromLong(balance.getBalance()).longValue(), NamedTextColor.GOLD)));
            }
        }
        pagination.contents(texts);
        pagination.sendTo(context.audience());
    }

    @Alias(value = "pay")
    @Command(alias = "give", desc = "Transfer the given amount to another account.")
    public void pay(CommandCause context, Collection<User> users, Double amount, @Default @Named("as") BaseAccount.Unique source)
    {
        if (amount < 0)
        {
            i18n.send(context, NEGATIVE, "What are you trying to do?");
            return;
        }

        boolean asSomeOneElse = false;
        if (!source.identifier().equals(context.identifier()))
        {
            if (!service.getPerms().ACCESS_WITHDRAW.check(context))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to pay money as someone else!");
                return;
            }
            asSomeOneElse = true;
        }

        for (User player : users)
        {
            if (player.uniqueId().equals(source.uniqueId()))
            {
                i18n.send(context, NEGATIVE, "Source and target are the same!", player);
                continue;
            }
            Account target = getUserAccount(player.uniqueId());
            if (target == null)
            {
                i18n.send(context, NEGATIVE, "{user} does not have an account!", player);
                continue;
            }
            Currency cur = service.defaultCurrency();
            TransferResult result = source.transfer(target, cur, new BigDecimal(amount), Collections.emptySet());
            Component formatAmount = cur.format(result.amount());
            switch (result.result())
            {
                case SUCCESS:
                    if (asSomeOneElse)
                    {
                        i18n.send(context, POSITIVE, "{txt#amount} transferred from {account}'s to {user}'s account!", formatAmount, source, player);
                    }
                    else
                    {
                        i18n.send(context, POSITIVE, "{txt#amount} transferred to {user}'s account!", formatAmount, player);
                    }
                    if (player.isOnline())
                    {
                        i18n.send(player.player().get(), POSITIVE, "{account} just paid you {txt#amount}!", source, formatAmount);
                    }
                    break;
                case ACCOUNT_NO_FUNDS:
                    if (asSomeOneElse)
                    {
                        i18n.send(context, NEGATIVE, "{account} cannot afford {txt#amount}!", source, formatAmount);
                    }
                    else
                    {
                        i18n.send(context, NEGATIVE, "You cannot afford {txt#amount}!", formatAmount);
                    }
                    break;
                default:
                    i18n.send(context, NEGATIVE, "The Transaction was not successful!");
                    break;
            }
        }
    }

}
