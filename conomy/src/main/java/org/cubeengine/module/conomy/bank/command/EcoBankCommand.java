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
package org.cubeengine.module.conomy.bank.command;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "bank", desc = "Administrative commands for Conomy Banks.")
public class EcoBankCommand extends ContainerCommand
{
    private final Conomy module;
    private BankConomyService service;
    private I18n i18n;

    public EcoBankCommand(Conomy module, BankConomyService service, I18n i18n)
    {
        super(module);
        this.module = module;
        this.service = service;
        this.i18n = i18n;
    }

    @Command(alias = "grant", desc = "Gives money to a bank or all banks")
    public void give(CommandSource context, BaseAccount.Virtual bank, Double amount)
    {
        TransactionResult result = bank.deposit(service.getDefaultCurrency(), new BigDecimal(amount), causeOf(context));
        switch (result.getResult())
        {
            case SUCCESS:
                Text formatAmount = result.getCurrency().format(result.getAmount());
                i18n.sendTranslated(context, POSITIVE, "You gave {txt#amount} to the bank {txt#bank}!",
                        formatAmount, bank.getDisplayName());
                Sponge.getServer().getOnlinePlayers().stream()
                        .filter(onlineUser -> service.hasAccess(bank, AccessLevel.WITHDRAW, onlineUser))
                        .forEach(onlineUser -> i18n.sendTranslated(onlineUser, POSITIVE, "{user} granted {input#amount} to your bank {input#bank}!",
                                onlineUser, bank.getDisplayName(), formatAmount));
                break;
            default:
                i18n.sendTranslated(context, NEGATIVE, "Transaction failed!");
                break;
        }
    }

    @Command(alias = "remove", desc = "Takes money from given bank or all banks")
    public void take(CommandSource context, BaseAccount.Virtual bank, Double amount)
    {
        TransactionResult result = bank.withdraw(service.getDefaultCurrency(), new BigDecimal(amount), causeOf(context));
        switch (result.getResult())
        {
            case SUCCESS:
                Text formatAmount = result.getCurrency().format(result.getAmount());
                i18n.sendTranslated(context, POSITIVE, "You took {input#amount} from the bank {input#bank}!",
                        formatAmount, bank.getDisplayName());
                Sponge.getServer().getOnlinePlayers().stream()
                        .filter(onlineUser -> service.hasAccess(bank, AccessLevel.WITHDRAW, onlineUser))
                        .forEach(onlineUser -> i18n.sendTranslated(onlineUser, POSITIVE, "{user} charged your bank {input#bank} for {input#amount}!",
                                onlineUser, bank.getDisplayName(), formatAmount));
                break;
            default:
                i18n.sendTranslated(context, NEGATIVE, "Transaction failed!");
                break;
        }

    }

    @Command(desc = "Reset the money from given banks")
    public void reset(CommandSource context, BaseAccount.Virtual bank)
    {
        TransactionResult result = bank.resetBalance(service.getDefaultCurrency(), causeOf(context));
        switch (result.getResult())
        {
            case SUCCESS:
                Text formatAmount = result.getCurrency().format(result.getAmount());
                i18n.sendTranslated(context, POSITIVE, "The account of the bank {txt#bank} got reset to {txt#balance}!",
                        bank.getDisplayName(), formatAmount);
                Sponge.getServer().getOnlinePlayers().stream()
                        .filter(onlineUser -> service.hasAccess(bank, AccessLevel.WITHDRAW, onlineUser))
                        .forEach(onlineUser -> i18n.sendTranslated(onlineUser, POSITIVE, "{user} reset the money of your bank {txt#bank} to {txt#balance}!",
                                onlineUser, bank.getDisplayName(), formatAmount));
                break;
            default:
                i18n.sendTranslated(context, NEGATIVE, "Transaction failed!");
                break;
        }

    }

    @Command(desc = "Sets the money from given banks")
    public void set(CommandSource context, BaseAccount.Virtual bank, Double amount)
    {
        TransactionResult result = bank.setBalance(service.getDefaultCurrency(), new BigDecimal(amount), causeOf(context));
        switch (result.getResult())
        {
            case SUCCESS:
                Text formatAmount = result.getCurrency().format(result.getAmount());
                i18n.sendTranslated(context, POSITIVE, "The money of bank account {txt#bank} got set to {txt#balance}!",
                        bank.getDisplayName(), formatAmount);
                Sponge.getServer().getOnlinePlayers().stream()
                        .filter(onlineUser -> service.hasAccess(bank, AccessLevel.WITHDRAW, onlineUser))
                        .forEach(onlineUser -> i18n.sendTranslated(onlineUser, POSITIVE, "{user} set the money of your bank {txt#bank} to {txt#balance}!",
                                onlineUser, bank.getDisplayName(), formatAmount));
                break;
            default:
                i18n.sendTranslated(context, NEGATIVE, "Transaction failed!");
                break;
        }
    }

    @Command(desc = "Hides the account of given bank")
    public void hide(CommandSource context, BaseAccount.Virtual bank)
    {
        if (bank.isHidden())
        {
            i18n.sendTranslated(context, POSITIVE, "The bank {txt#bank} is already hidden!", bank.getDisplayName());
            return;
        }
        bank.setHidden(true);
        i18n.sendTranslated(context, POSITIVE, "The bank {txt#bank} is now hidden!", bank.getDisplayName());
    }

    @Command(desc = "Unhides the account of given banks")
    public void unhide(CommandSource context, BaseAccount.Virtual bank)
    {
        if (!bank.isHidden())
        {
            i18n.sendTranslated(context, POSITIVE, "The bank {txt#bank} was not hidden!", bank.getDisplayName());
            return;
        }
        bank.setHidden(false);
        i18n.sendTranslated(context, POSITIVE, "The bank {txt#bank} is no longer hidden!", bank.getDisplayName());
    }

    private Cause causeOf(CommandSource context)
    {
        return Cause.of(NamedCause.source(context));
    }
}
