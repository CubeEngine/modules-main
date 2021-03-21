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
package org.cubeengine.module.conomy.bank.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.cubeengine.module.conomy.command.UniqueAccountParser;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.service.permission.Subject;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Using({UniqueAccountParser.class, VirtualAccountParser.class})
@Singleton
@Command(name = "bank", desc = "Manages your money in banks.")
public class BankCommand extends DispatcherCommand
{
    private BankConomyService service;
    private I18n i18n;
    @Inject
    public BankCommand(BankManageCommand manage, BankEcoCommand eco, BankConomyService service, I18n i18n)
    {
        super(manage, eco);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias(value = "bbalance")
    @Command(desc = "Shows the balance of the specified bank")
    public void balance(CommandCause context, @Default BaseAccount.Virtual bank)
    {
        Map<Currency, BigDecimal> balances = bank.balances(context.activeContexts());

        if (balances.isEmpty())
        {
            i18n.send(context, NEGATIVE, "No Balance for bank {account} found!", bank);
            return;
        }
        i18n.send(context, POSITIVE, "Bank {account} Balance:", bank);
        for (Map.Entry<Currency, BigDecimal> entry : balances.entrySet())
        {
            context.sendMessage(Identity.nil(), Component.text(" - ").append(entry.getKey().format(entry.getValue()).color(NamedTextColor.GOLD)));
        }
    }

    @Command(desc = "Deposits given amount of money into the bank")
    @Restricted(msg =  "You cannot deposit into a bank as console!")
    public void deposit(ServerPlayer context, BaseAccount.Virtual bank, Double amount)
    {
        Optional<UniqueAccount> account = service.orCreateAccount(context.uniqueId());
        if (!account.isPresent())
        {
            i18n.send(context, NEGATIVE, "You do not have an account!");
            return;
        }

        if (!service.hasAccess(bank, AccessLevel.DEPOSIT, context))
        {
            i18n.send(context, NEGATIVE, "You are not allowed to deposit money into that bank!");
            return;
        }

        TransferResult result = account.get().transfer(bank, service.defaultCurrency(), new BigDecimal(amount));
        switch (result.result())
        {
            case SUCCESS:
                Currency cur = result.currency();
                i18n.send(context, POSITIVE, "Deposited {txt#amount} into {account}! New Balance: {txt#balance}",
                        cur.format(result.amount()), bank, cur.format(bank.balance(cur)));
                break;
            case ACCOUNT_NO_FUNDS:
                i18n.send(context, NEGATIVE, "You cannot afford to spend that much!");
                break;
            default:
                i18n.send(context, NEGATIVE, "Transaction failed!");
                break;
        }
    }

    @Command(desc = "Withdraws given amount of money from the bank")
    @Restricted(msg = "You cannot withdraw from a bank as console!")
    public void withdraw(ServerPlayer context, BaseAccount.Virtual bank, Double amount) //takes money from the bank
    {
        Optional<UniqueAccount> account = service.orCreateAccount(context.uniqueId());
        if (!account.isPresent())
        {
            i18n.send(context, NEGATIVE, "You do not have an account!");
            return;
        }

        if (!service.hasAccess(bank, AccessLevel.WITHDRAW, context))
        {
            i18n.send(context, NEGATIVE, "You are not allowed to withdraw money from that bank!");
        }

        TransferResult result = account.get().transfer(bank, service.defaultCurrency(), new BigDecimal(amount));
        switch (result.result())
        {
            case SUCCESS:
                Currency cur = result.currency();
                i18n.send(context, POSITIVE, "Withdrawn {txt#amount} from {account}! New Balance: {txt#balance}",
                        cur.format(result.amount()), bank, cur.format(bank.balance(cur)));
                break;
            case ACCOUNT_NO_FUNDS:
                i18n.send(context, NEGATIVE, "The bank does not hold enough money to spend that much!");
                break;
            default:
                i18n.send(context, NEGATIVE, "Transaction failed!");
                break;
        }

    }

    @Command(desc = "Pays given amount of money as bank to another bank")
    public void pay(CommandCause context, BaseAccount.Virtual bank, BaseAccount.Virtual otherBank, Double amount)
    {
        if (amount < 0)
        {
            i18n.send(context, NEGATIVE, "Sorry but robbing a bank is not allowed!");
            return;
        }

        if (!service.hasAccess(bank, AccessLevel.WITHDRAW, context))
        {
            i18n.send(context, NEGATIVE, "You are not allowed to make transaction from that bank!");
            return;
        }

        TransferResult result = bank.transfer(otherBank, service.defaultCurrency(), new BigDecimal(amount));
        switch (result.result())
        {
            case SUCCESS:
                Currency cur = result.currency();
                i18n.send(context, POSITIVE, "Transferred {txt#amount} from {account} to {account} New Balance: {txt#balance}",
                        cur.format(result.amount()), bank, otherBank, cur.format(bank.balance(cur)));
                break;
            case ACCOUNT_NO_FUNDS:
                i18n.send(context, NEGATIVE, "The bank does not hold enough money to spend that much!");
                break;
            default:
                i18n.send(context, NEGATIVE, "Transaction failed!");
                break;
        }
    }

    @Command(desc = "Pays given amount of money as bank to a user account")
    public void payUser(CommandCause context, BaseAccount.Virtual bank, BaseAccount.Unique user, Double amount)
    {
        if (!service.hasAccess(bank, AccessLevel.WITHDRAW, context))
        {
            i18n.send(context, NEGATIVE, "You are not allowed to make transaction from that bank!");
            return;
        }

        TransferResult result = bank.transfer(user, service.defaultCurrency(), new BigDecimal(amount));
        switch (result.result())
        {
            case SUCCESS:
                Currency cur = result.currency();
                i18n.send(context, POSITIVE, "Transferred {txt#amount} from {account} to {account#user}! New Balance: {txt#balance}",
                        cur.format(result.amount()), bank, user, cur.format(bank.balance(cur)));
                break;
            case ACCOUNT_NO_FUNDS:
                i18n.send(context, NEGATIVE, "The bank does not hold enough money to spend that much!");
                break;
            default:
                i18n.send(context, NEGATIVE, "Transaction failed!");
                break;
        }
    }

    // TODO pay members command?
    // TODO dirigent formatter for BaseAccount.Virtual

    @Command(desc = "Lists all banks")
    public void list(CommandCause context, @Default User owner) //Lists all banks [of given player]
    {
        if (owner != null)
        {
            List<BaseAccount.Virtual> namedAccounts = service.getBanks(owner, AccessLevel.MANAGE);
            if (namedAccounts.isEmpty())
            {
                i18n.send(context, POSITIVE, "{user} is not owner of any bank!", owner);
                return;
            }
            i18n.send(context, POSITIVE, "{user} is the owner of the following banks:", owner);

            for (BaseAccount.Virtual bank : namedAccounts)
            {
                context.sendMessage(Identity.nil(), Component.text(" - ").append(bank.displayName().color(NamedTextColor.YELLOW)));
            }
            return;
        }

        List<BaseAccount.Virtual> namedAccounts = service.getBanks(context, AccessLevel.SEE);
        if (namedAccounts.isEmpty())
        {
            i18n.send(context, NEUTRAL, "There are no banks currently!");
            return;
        }
        i18n.send(context, POSITIVE, "The following banks are available:");
        for (BaseAccount.Virtual bank : namedAccounts)
        {
            context.sendMessage(Identity.nil(), Component.text(" - ").append(bank.displayName().color(NamedTextColor.YELLOW)));
        }
    }

    @Command(desc = "Shows bank information")
    public void info(CommandCause context, BaseAccount.Virtual bank)
    {
        i18n.send(context, POSITIVE, "Bank Information for {account}:", bank);
        i18n.send(context, POSITIVE, "Current Balance: {txt}", service.defaultCurrency().format(bank.balance(service.defaultCurrency())));

        this.listaccess(context, bank);

        if (bank.isHidden())
        {
            i18n.send(context, POSITIVE, "This bank is hidden for other players!");
        }
    }

    @Command(desc = "Lists the access levels for a bank")
    public void listaccess(CommandCause context, @Default BaseAccount.Virtual bank)
    {
        i18n.send(context, POSITIVE, "Access Levels for {account}:", bank);

        // TODO global access levels
        // Everyone can SEE(hidden) DEPOSIT(needInvite)
        // Noone can ...

        for (Subject subject : Sponge.server().serviceProvider().permissionService().userSubjects().loadedSubjects())
        {
            Optional<String> option = subject.option(bank.activeContexts(), "conomy.bank.access-level." + bank.identifier());
            if (option.isPresent())
            {
                // TODO list players highlight granted access
                // <player> SEE DEPOSIT WITHDRAW MANAGE
            }
        }
    }
}
