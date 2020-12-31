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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.service.economy.transaction.TransactionResult;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Command(name = "eco", desc = "Administrative commands for Conomy Banks.")
public class BankEcoCommand extends DispatcherCommand
{
    private BankConomyService service;
    private I18n i18n;

    @Inject
    public BankEcoCommand(BankConomyService service, I18n i18n)
    {
        this.service = service;
        this.i18n = i18n;
    }

    @Command(alias = "grant", desc = "Gives money to a bank or all banks")
    public void give(CommandCause context, BaseAccount.Virtual bank, Double amount)
    {
        TransactionResult result = bank.deposit(service.getDefaultCurrency(), new BigDecimal(amount));
        switch (result.getResult())
        {
            case SUCCESS:
                Component formatAmount = result.getCurrency().format(result.getAmount());
                i18n.send(context, POSITIVE, "You gave {txt#amount} to the bank {account}!",
                        formatAmount, bank);
                Sponge.getServer().getOnlinePlayers().stream()
                        .filter(onlineUser -> service.hasAccess(bank, AccessLevel.WITHDRAW, onlineUser))
                        .forEach(onlineUser -> i18n.send(onlineUser, POSITIVE, "{user} granted {input#amount} to your bank {account}!",
                                onlineUser, formatAmount, bank));
                break;
            default:
                i18n.send(context, NEGATIVE, "Transaction failed!");
                break;
        }
    }

    @Command(alias = "remove", desc = "Takes money from given bank or all banks")
    public void take(CommandCause context, BaseAccount.Virtual bank, Double amount)
    {
        TransactionResult result = bank.withdraw(service.getDefaultCurrency(), new BigDecimal(amount));
        switch (result.getResult())
        {
            case SUCCESS:
                Component formatAmount = result.getCurrency().format(result.getAmount());
                i18n.send(context, POSITIVE, "You took {input#amount} from the bank {account}!",
                        formatAmount, bank);
                Sponge.getServer().getOnlinePlayers().stream()
                        .filter(onlineUser -> service.hasAccess(bank, AccessLevel.WITHDRAW, onlineUser))
                        .forEach(onlineUser -> i18n.send(onlineUser, POSITIVE, "{user} charged your bank {account} for {input#amount}!",
                                onlineUser, bank, formatAmount));
                break;
            default:
                i18n.send(context, NEGATIVE, "Transaction failed!");
                break;
        }

    }

    @Command(desc = "Reset the money from given banks")
    public void reset(CommandCause context, BaseAccount.Virtual bank)
    {
        TransactionResult result = bank.resetBalance(service.getDefaultCurrency());
        switch (result.getResult())
        {
            case SUCCESS:
                Component formatAmount = result.getCurrency().format(result.getAmount());
                i18n.send(context, POSITIVE, "The account of the bank {account} got reset to {txt#balance}!",
                        bank, formatAmount);
                Sponge.getServer().getOnlinePlayers().stream()
                        .filter(onlineUser -> service.hasAccess(bank, AccessLevel.WITHDRAW, onlineUser))
                        .forEach(onlineUser -> i18n.send(onlineUser, POSITIVE, "{user} reset the money of your bank {account} to {txt#balance}!",
                                onlineUser, bank, formatAmount));
                break;
            default:
                i18n.send(context, NEGATIVE, "Transaction failed!");
                break;
        }

    }

    @Command(desc = "Sets the money from given banks")
    public void set(CommandCause context, BaseAccount.Virtual bank, Double amount)
    {
        TransactionResult result = bank.setBalance(service.getDefaultCurrency(), new BigDecimal(amount));
        switch (result.getResult())
        {
            case SUCCESS:
                Component formatAmount = result.getCurrency().format(result.getAmount());
                i18n.send(context, POSITIVE, "The money of bank account {account} got set to {txt#balance}!",
                        bank, formatAmount);
                Sponge.getServer().getOnlinePlayers().stream()
                        .filter(onlineUser -> service.hasAccess(bank, AccessLevel.WITHDRAW, onlineUser))
                        .forEach(onlineUser -> i18n.send(onlineUser, POSITIVE, "{user} set the money of your bank {account} to {txt#balance}!",
                                onlineUser, bank, formatAmount));
                break;
            default:
                i18n.send(context, NEGATIVE, "Transaction failed!");
                break;
        }
    }

    @Command(desc = "Hides the account of given bank")
    public void hide(CommandCause context, BaseAccount.Virtual bank)
    {
        if (bank.isHidden())
        {
            i18n.send(context, POSITIVE, "The bank {account} is already hidden!", bank);
            return;
        }
        bank.setHidden(true);
        i18n.send(context, POSITIVE, "The bank {account} is now hidden!", bank);
    }

    @Command(desc = "Unhides the account of given banks")
    public void unhide(CommandCause context, BaseAccount.Virtual bank)
    {
        if (!bank.isHidden())
        {
            i18n.send(context, POSITIVE, "The bank {account} was not hidden!", bank);
            return;
        }
        bank.setHidden(false);
        i18n.send(context, POSITIVE, "The bank {account} is no longer hidden!", bank);
    }
}
