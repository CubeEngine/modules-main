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
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.text.format.TextColors.GOLD;

@Command(name = "bank", desc = "Manages your money in banks.")
public class BankCommand extends ContainerCommand
{
    private BankConomyService service;
    private I18n i18n;

    public BankCommand(CommandManager base, BankConomyService service, I18n i18n)
    {
        super(base, Conomy.class);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias(value = "bbalance")
    @Command(desc = "Shows the balance of the specified bank")
    public void balance(CommandSource context, @Default BaseAccount.Virtual bank)
    {
        Map<Currency, BigDecimal> balances = bank.getBalances(context.getActiveContexts());

        if (balances.isEmpty())
        {
            i18n.sendTranslated(context, NEGATIVE, "No Balance for bank {account} found!", bank);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "Bank {account} Balance:", bank);
        for (Map.Entry<Currency, BigDecimal> entry : balances.entrySet())
        {
            context.sendMessage(Text.of(" - ", GOLD, entry.getKey().format(entry.getValue())));
        }
    }

    private Cause causeOf(CommandSource context)
    {
        return Cause.of(NamedCause.source(context));
    }

    @Command(desc = "Deposits given amount of money into the bank")
    @Restricted(value = Player.class, msg =  "You cannot deposit into a bank as console!")
    public void deposit(Player context, BaseAccount.Virtual bank, Double amount)
    {
        Optional<UniqueAccount> account = service.getOrCreateAccount(context.getUniqueId());
        if (!account.isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "You do not have an account!");
            return;
        }

        if (!service.hasAccess(bank, AccessLevel.DEPOSIT, context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to deposit money into that bank!");
            return;
        }

        TransferResult result = account.get().transfer(bank, service.getDefaultCurrency(), new BigDecimal(amount), causeOf(context));
        switch (result.getResult())
        {
            case SUCCESS:
                Currency cur = result.getCurrency();
                i18n.sendTranslated(context, POSITIVE, "Deposited {txt#amount} into {account}! New Balance: {txt#balance}",
                        cur.format(result.getAmount()), bank, cur.format(bank.getBalance(cur)));
                break;
            case ACCOUNT_NO_FUNDS:
                i18n.sendTranslated(context, NEGATIVE, "You cannot afford to spend that much!");
                break;
            default:
                i18n.sendTranslated(context, NEGATIVE, "Transaction failed!");
                break;
        }
    }

    @Command(desc = "Withdraws given amount of money from the bank")
    @Restricted(value = Player.class, msg = "You cannot withdraw from a bank as console!")
    public void withdraw(Player context, BaseAccount.Virtual bank, Double amount) //takes money from the bank
    {
        Optional<UniqueAccount> account = service.getOrCreateAccount(context.getUniqueId());
        if (!account.isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "You do not have an account!");
            return;
        }

        if (!service.hasAccess(bank, AccessLevel.WITHDRAW, context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to withdraw money from that bank!");
        }

        TransferResult result = account.get().transfer(bank, service.getDefaultCurrency(), new BigDecimal(amount), causeOf(context));
        switch (result.getResult())
        {
            case SUCCESS:
                Currency cur = result.getCurrency();
                i18n.sendTranslated(context, POSITIVE, "Withdrawn {txt#amount} from {account}! New Balance: {txt#balance}",
                        cur.format(result.getAmount()), bank, cur.format(bank.getBalance(cur)));
                break;
            case ACCOUNT_NO_FUNDS:
                i18n.sendTranslated(context, NEGATIVE, "The bank does not hold enough money to spend that much!");
                break;
            default:
                i18n.sendTranslated(context, NEGATIVE, "Transaction failed!");
                break;
        }

    }

    @Command(desc = "Pays given amount of money as bank to another bank")
    public void pay(CommandSource context, BaseAccount.Virtual bank, BaseAccount.Virtual otherBank, Double amount)
    {
        if (amount < 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "Sorry but robbing a bank is not allowed!");
            return;
        }

        if (!service.hasAccess(bank, AccessLevel.WITHDRAW, context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to make transaction from that bank!");
            return;
        }

        TransferResult result = bank.transfer(otherBank, service.getDefaultCurrency(), new BigDecimal(amount), causeOf(context));
        switch (result.getResult())
        {
            case SUCCESS:
                Currency cur = result.getCurrency();
                i18n.sendTranslated(context, POSITIVE, "Transferred {txt#amount} from {account} to {account} New Balance: {txt#balance}",
                        cur.format(result.getAmount()), bank, otherBank, cur.format(bank.getBalance(cur)));
                break;
            case ACCOUNT_NO_FUNDS:
                i18n.sendTranslated(context, NEGATIVE, "The bank does not hold enough money to spend that much!");
                break;
            default:
                i18n.sendTranslated(context, NEGATIVE, "Transaction failed!");
                break;
        }
    }

    @Command(desc = "Pays given amount of money as bank to a user account")
    public void payUser(CommandSource context, BaseAccount.Virtual bank, BaseAccount.Unique user, Double amount)
    {
        if (!service.hasAccess(bank, AccessLevel.WITHDRAW, context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to make transaction from that bank!");
            return;
        }

        TransferResult result = bank.transfer(user, service.getDefaultCurrency(), new BigDecimal(amount), causeOf(context));
        switch (result.getResult())
        {
            case SUCCESS:
                Currency cur = result.getCurrency();
                i18n.sendTranslated(context, POSITIVE, "Transferred {txt#amount} from {account} to {account#user}! New Balance: {txt#balance}",
                        cur.format(result.getAmount()), bank, user, cur.format(bank.getBalance(cur)));
                break;
            case ACCOUNT_NO_FUNDS:
                i18n.sendTranslated(context, NEGATIVE, "The bank does not hold enough money to spend that much!");
                break;
            default:
                i18n.sendTranslated(context, NEGATIVE, "Transaction failed!");
                break;
        }
    }

    // TODO pay members command?
    // TODO dirigent formatter for BaseAccount.Virtual

    @Command(desc = "Lists all banks")
    public void list(CommandSource context, @Default User owner) //Lists all banks [of given player]
    {
        if (owner != null)
        {
            List<BaseAccount.Virtual> namedAccounts = service.getBanks(owner, AccessLevel.MANAGE);
            if (namedAccounts.isEmpty())
            {
                i18n.sendTranslated(context, POSITIVE, "{user} is not owner of any bank!", owner);
                return;
            }
            i18n.sendTranslated(context, POSITIVE, "{user} is the owner of the following banks:", owner);

            for (BaseAccount.Virtual bank : namedAccounts)
            {
                context.sendMessage(Text.of(" - ", TextColors.YELLOW, bank.getDisplayName()));
            }
            return;
        }

        List<BaseAccount.Virtual> namedAccounts = service.getBanks(context, AccessLevel.SEE);
        if (namedAccounts.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "There are no banks currently!");
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The following banks are available:");
        for (BaseAccount.Virtual bank : namedAccounts)
        {
            context.sendMessage(Text.of(" - ", TextColors.YELLOW, bank.getDisplayName()));
        }
    }

    @Command(desc = "Shows bank information")
    public void info(CommandSource context, BaseAccount.Virtual bank)
    {
        i18n.sendTranslated(context, POSITIVE, "Bank Information for {account}:", bank);
        i18n.sendTranslated(context, POSITIVE, "Current Balance: {txt}", service.getDefaultCurrency().format(bank.getBalance(service.getDefaultCurrency())));

        this.listaccess(context, bank);

        if (bank.isHidden())
        {
            i18n.sendTranslated(context, POSITIVE, "This bank is hidden for other players!");
        }
    }

    @Command(desc = "Lists the access levels for a bank")
    public void listaccess(CommandSource context, @Default BaseAccount.Virtual bank)
    {
        i18n.sendTranslated(context, POSITIVE, "Access Levels for {account}:", bank);

        // TODO global access levels
        // Everyone can SEE(hidden) DEPOSIT(needInvite)
        // Noone can ...

        for (Subject subject : Sponge.getServiceManager().provideUnchecked(PermissionService.class).getUserSubjects().getAllSubjects())
        {
            Optional<String> option = subject.getOption(bank.getActiveContexts(), "conomy.bank.access-level." + bank.getIdentifier());
            if (option.isPresent())
            {
                // TODO list players highlight granted access
                // <player> SEE DEPOSIT WITHDRAW MANAGE
            }
        }
    }
}
