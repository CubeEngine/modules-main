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
import java.util.List;
import java.util.Map;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.ConfigCurrency;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.module.conomy.storage.BalanceModel;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.command.parser.PlayerList;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.*;

@Command(name = "money", desc = "Manage your money")
public class MoneyCommand extends ContainerCommand
{
    // TODO add in context to commands
    private final Conomy module;
    private ConomyService service;
    private I18n i18n;

    public MoneyCommand(CommandManager base, Conomy module, ConomyService conomy, I18n i18n)
    {
        super(base, Conomy.class);
        this.module = module;
        this.service = conomy;
        this.i18n = i18n;
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        return this.getCommand("balance").execute(invocation);
    }

    private BaseAccount.Unique getUserAccount(User user)
    {
        return service.getOrCreateAccount(user.getUniqueId())
                .filter(a -> a instanceof BaseAccount.Unique)
                .map(BaseAccount.Unique.class::cast).orElse(null);
    }

    @Alias(value = {"balance", "moneybalance", "pmoney"})
    @Command(desc = "Shows your balance")
    public void balance(CommandSource context, @Default BaseAccount.Unique account)
    {
        Map<Currency, BigDecimal> balances = account.getBalances();
        if (balances.isEmpty())
        {
            i18n.send(context, NEGATIVE, "No Balance for {account} found!", account);
            return;
        }
        i18n.send(context, POSITIVE, "{account}'s Balance:", account);
        for (Map.Entry<Currency, BigDecimal> entry : balances.entrySet())
        {
            context.sendMessage(Text.of(GOLD, entry.getKey().format(entry.getValue())));
        }
    }

    @Alias(value = {"toplist", "balancetop", "topmoney"})
    @Command(desc = "Shows the players with the highest balance.")
    public void top(CommandSource context, @Optional @Label("[fromRank-]toRank") String range)
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
        Collection<BalanceModel> models = this.service.getTopBalance(true, false, fromRank, toRank,
                context.hasPermission(module.perms().ACCESS_SEE.getId()));
        int i = fromRank;


        PaginationList.Builder pagination = Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder();
        pagination.padding(Text.of("-"));
        if (fromRank == 1)
        {
            pagination.title(i18n.translate(context, POSITIVE, "Top Balance ({amount})", models.size()));
        }
        else
        {
            pagination.title(i18n.translate(context, POSITIVE, "Top Balance from {integer} to {integer}",
                    fromRank, fromRank + models.size() - 1));
        }
        List<Text> texts = new ArrayList<>();
        for (BalanceModel balance : models)
        {
            Account account = service.getOrCreateAccount(balance.getAccountID()).get();
            ConfigCurrency currency = service.getCurrency(balance.getCurrency());
            if (currency == null)
            {
                texts.add(Text.of(CRITICAL, "?", balance.getCurrency(), "? : ", balance.getBalance()));
            }
            else
            {
                texts.add(Text.of(i++, WHITE, " - ",
                        DARK_GREEN, account.getDisplayName(), WHITE, ": ",
                        GOLD, currency.format(currency.fromLong(balance.getBalance()))));
            }
        }
        pagination.contents(texts);
        pagination.sendTo(context);
    }

    @Alias(value = "pay")
    @Command(alias = "give", desc = "Transfer the given amount to another account.")
    public void pay(CommandSource context, @Label("*|<players>") PlayerList users, Double amount, @Default @Named("as") BaseAccount.Unique source)
    {
        if (amount < 0)
        {
            i18n.send(context, NEGATIVE, "What are you trying to do?");
            return;
        }

        boolean asSomeOneElse = false;
        if (!source.getIdentifier().equals(context.getIdentifier()))
        {
            if (!context.hasPermission(module.perms().ACCESS_WITHDRAW.getId()))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to pay money as someone else!");
                return;
            }
            asSomeOneElse = true;
        }

        for (User user : users.list())
        {
            if (user.equals(source))
            {
                i18n.send(context, NEGATIVE, "Source and target are the same!", user);
                continue;
            }
            Account target = getUserAccount(user);
            if (target == null)
            {
                i18n.send(context, NEGATIVE, "{user} does not have an account!", user);
                continue;
            }
            Currency cur = service.getDefaultCurrency();
            TransferResult result = source.transfer(target, cur, new BigDecimal(amount), causeOf(context));
            Text formatAmount = cur.format(result.getAmount());
            switch (result.getResult())
            {
                case SUCCESS:
                    if (asSomeOneElse)
                    {
                        i18n.send(context, POSITIVE, "{txt#amount} transferred from {account}'s to {user}'s account!", formatAmount, source, user);
                    }
                    else
                    {
                        i18n.send(context, POSITIVE, "{txt#amount} transferred to {user}'s account!", formatAmount, user);
                    }
                    if (user.isOnline())
                    {
                        i18n.send(user.getPlayer().get(), POSITIVE, "{account} just paid you {txt#amount}!", source, formatAmount);
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

    private static Cause causeOf(CommandSource context)
    {
        return Cause.of(EventContext.empty(), context);
    }
}
