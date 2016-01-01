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
package org.cubeengine.module.conomy.commands;

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.module.conomy.BankAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;
import java.util.Map;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.GOLD;

@Command(name = "bank", desc = "Manages your money in banks.")
public class BankCommand extends ContainerCommand
{
    private final Conomy module;
    private ConomyService service;
    private I18n i18n;

    public BankCommand(Conomy module, ConomyService service, I18n i18n)
    {
        super(module);
        this.module = module;
        this.service = service;
        this.i18n = i18n;
    }

    @Alias(value = "bbalance")
    @Command(desc = "Shows the balance of the specified bank")
    public void balance(CommandSource context, @Default BankAccount bank)
    {
        ensureIsVisible(context, bank);
        Map<Currency, BigDecimal> balances = bank.getBalances(context.getActiveContexts());

        if (balances.isEmpty())
        {
            i18n.sendTranslated(context, NEGATIVE, "No Balance for bank {txt} found!", bank.getDisplayName());
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "Bank {txt} Balance:", bank.getDisplayName());
        for (Map.Entry<Currency, BigDecimal> entry : balances.entrySet())
        {
            context.sendMessage(Text.of(" - ", GOLD, entry.getKey().format(entry.getValue())));
        }
    }

    private void ensureIsVisible(CommandSource context, BankAccount account)
    {
        if (!account.isHidden() || module.perms().BANK_SHOWHIDDEN.isAuthorized(context)
            || !(context instanceof User))
        {
            return;
        }
        if (account.hasAccess((User)context))
        {
            throw new ReaderException(context.getTranslation(NEGATIVE, "There is no bank account named {input#name}!", account.getName()).get(context.getLocale()));
        }
    }

    @Command(desc = "Deposits given amount of money into the bank")
    @Restricted(value = User.class, msg =  "You cannot deposit into a bank as console!")
    public void deposit(User context, BankAccount bank, Double amount)
    {
        UserAccount userAccount = this.manager.getUserAccount(context, this.manager.getAutoCreateUserAccount());
        if (userAccount == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "You do not have an account!");
            return;
        }
        if (userAccount.transactionTo(bank, amount, force))
        {
            i18n.sendTranslated(context, POSITIVE, "Deposited {input#amount} into {name#bank}! New Balance: {input#balance}", this.manager.format(amount), bank.getName(), this.manager.format( bank.balance()));
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "You cannot afford to spend that much!");
    }

    @Command(desc = "Withdraws given amount of money from the bank")
    @Restricted(value = User.class, msg = "You cannot withdraw from a bank as console!")
    public void withdraw(User context, BankAccount bank, Double amount)//takes money from the bank
    {
        if (!bank.isOwner(context) && !module.perms().COMMAND_BANK_WITHDRAW_OTHER.isAuthorized(context))
        {
            i18n.sendTranslated(context, NEGATIVE, "Only owners of the bank are allowed to withdraw from it!");
            return;
        }
        UserAccount userAccount = this.manager.getUserAccount(context, this.manager.getAutoCreateUserAccount());
        if (userAccount == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "You do not have an account!");
            return;
        }
        if (bank.transactionTo(userAccount, amount, force))
        {
            i18n.sendTranslated(context, POSITIVE, "Withdrawn {input#amount} from {name#bank}! New Balance: {input#balance}", this.manager.format(amount), bank.getName(), this.manager.format(bank.balance()));
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "The bank does not hold enough money to spend that much!");
    }

    @Command(desc = "Pays given amount of money as bank to another account")
    public void pay(CommandSender context, @Label("bank")BankAccount bankAccount, String targetAccount, Double amount,
                    @Flag boolean bank)//pay AS bank to a player or other bank <name> [-bank]
    {
        if (!bankAccount.isOwner((User)context))
        {
            i18n.sendTranslated(context, NEGATIVE, "Only owners of the bank are allowed to spend the money from it!");
            return;
        }
        Account target;
        if (!bank)
        {
            User user = um.findUser(targetAccount);
            target = this.manager.getUserAccount(user, this.manager.getAutoCreateUserAccount());
            if (target == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "{user} has no account!", user);
                return;
            }
        }
        else
        {
            target = this.manager.getBankAccount(targetAccount, false);
            if (target == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "There is no bank account named {input#bank}!", targetAccount);
                return;
            }
        }
        if (amount < 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "Sorry but robbing a bank is not allowed!");
            return;
        }
        force = force && module.perms().COMMAND_BANK_PAY_FORCE.isAuthorized(context);
        if (bankAccount.transactionTo(target, amount, force))
        {
            if (bank)
            {
                i18n.sendTranslated(context, POSITIVE, "Transferred {input#amount} from {name#bank} to {user}! New Balance: {input#balance}", this.manager.format(amount), bankAccount.getName(), target.getName(), this.manager.format(bankAccount.balance()));
            }
            else
            {
                i18n.sendTranslated(context, POSITIVE, "Transferred {input#amount} from {name#bank} to {name#bank} New Balance: {input#balance}", this.manager.format(amount), bankAccount.getName(), target.getName(), this.manager.format(bankAccount.balance()));
            }
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "The bank does not hold enough money to spend that much!");
    }
}
