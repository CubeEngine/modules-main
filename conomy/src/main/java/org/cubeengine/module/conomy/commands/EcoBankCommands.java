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

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;
import org.cubeengine.module.conomy.Conomy;
import de.cubeisland.engine.module.conomy.account.BankAccount;
import de.cubeisland.engine.module.conomy.account.ConomyManager;
import org.cubeengine.service.user.UserManager;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "bank", desc = "Administrative commands for Conomy Banks.")
public class EcoBankCommands extends ContainerCommand
{
    private final Conomy module;
    private final UserManager um;
    private final ConomyManager manager;

    public EcoBankCommands(Conomy module, UserManager um)
    {
        super(module);
        this.module = module;
        this.um = um;
        this.manager = module.getManager();
    }

    @Command(alias = "grant", desc = "Gives money to a bank or all banks")
    public void give(CommandSender context, BankAccount bank, Double amount)
    {
        /* TODO give all
        if ("*".equals(context.get(0)))
        {
            this.manager.transactionAll(false, true, amount);
            context.sendTranslated(POSITIVE, "You gave {input#amount} to every bank!", format);
            return;
        } */
        String format = manager.format(amount);
        this.manager.transaction(null, bank, amount, true);
        context.sendTranslated(POSITIVE, "You gave {input#amount} to the bank {input#bank}!", format, bank.getName());
        for (User user : um.getOnlineUsers())
        {
            if (bank.isOwner(user))
            {
                user.sendTranslated(POSITIVE, "{user} granted {input#amount} to your bank {input#bank}!", context, format, bank.getName());
            }
        }
    }

    @Command(alias = "remove", desc = "Takes money from given bank or all banks")
    public void take(CommandContext context, BankAccount bank, Double amount)
    {
        /* TODO take all
        if ("*".equals(context.get(0)))
        {
            this.manager.transactionAll(false, true, -amount);
            context.sendTranslated(POSITIVE, "You took {input#amount} from every bank!", format);
            return;
        } */
        String format = manager.format(amount);
        this.manager.transaction(bank, null, amount, true);
        context.sendTranslated(POSITIVE, "You took {input#amount} from the bank {input#bank}!", format, bank.getName());
        for (User onlineUser : um.getOnlineUsers())
        {
            if (bank.isOwner(onlineUser))
            {
                onlineUser.sendTranslated(POSITIVE, "{user} charged your bank {input#bank} for {input#amount}!", context.getSource(), bank.getName(), format);
            }
        }
    }

    @Command(desc = "Reset the money from given banks")
    public void reset(CommandContext context, BankAccount bank)
    {
        /* TODO reset all
        if ("*".equals(context.get(0)))
        {
            this.manager.setAll(false, true, this.manager.getDefaultBankBalance());
            context.sendTranslated(POSITIVE, "You reset every bank account!");
            return;
        } */
        bank.reset();
        String format = this.manager.format(this.manager.getDefaultBalance());
        context.sendTranslated(POSITIVE, "The account of the bank {input#bank} got reset to {input#balance}!", bank.getName(), format);
        for (User onlineUser : um.getOnlineUsers())
        {
            if (bank.isOwner(onlineUser))
            {
                onlineUser.sendTranslated(POSITIVE, "{user} reset the money of your bank {input#bank} to {input#balance}!", context.getSource(), bank.getName(), format);
            }
        }
    }

    @Command(desc = "Sets the money from given banks")
    public void set(CommandContext context, BankAccount bank, Double amount)
    {
        /* TODO set all
        if ("*".equals(context.get(0)))
        {
            this.manager.setAll(false, true, amount);
            context.sendTranslated(POSITIVE, "You have set every bank account to {input#balance}!", format);
            return;
        }
        */
        String format = this.manager.format(amount);
        bank.set(amount);
        context.sendTranslated(POSITIVE, "The money of bank account {input#bank} got set to {input#balance}!", bank.getName(), format);
        for (User onlineUser : um.getOnlineUsers())
        {
            if (bank.isOwner(onlineUser))
            {
                onlineUser.sendTranslated(POSITIVE, "{user} set the money of your bank {input#bank} to {input#balance}!", context.getSource(), bank.getName(), format);
            }
        }
    }

    @Command(desc = "Scales the money from given banks")
    public void scale(CommandContext context, BankAccount bank, Float factor)
    {
        /* TODO scale all
        if ("*".equals(context.get(0)))
        {
            this.manager.scaleAll(false, true, factor);
            context.sendTranslated(POSITIVE, "Scaled the balance of every bank by {decimal#factor}!", factor);
            return;
        } */
        bank.scale(factor);
        context.sendTranslated(POSITIVE, "Scaled the balance of the bank {input#bank} by {decimal#factor}!", bank.getName(), factor);
        for (User onlineUser : um.getOnlineUsers())
        {
            if (bank.isOwner(onlineUser))
            {
                onlineUser.sendTranslated(POSITIVE, "{user} scaled the money of your bank {input#bank} by {decimal#factor}", context.getSource().getName(), bank.getName(), factor);
            }
        }
    }

    @Command(desc = "Hides the account of given bank")
    public void hide(CommandContext context, BankAccount bank)
    {
        /* TODO hide all
        if ("*".equals(context.get(0)))
        {
            this.manager.hideAll(false, true);
            return;
        } */
        if (bank.isHidden())
        {
            context.sendTranslated(POSITIVE, "The bank {input#bank} is already hidden!", bank.getName());
            return;
        }
        bank.setHidden(true);
        context.sendTranslated(POSITIVE, "The bank {input#bank} is now hidden!", bank.getName());
    }

    @Command(desc = "Unhides the account of given banks")
    public void unhide(CommandContext context, BankAccount bank)
    {
        /* TODO uhide all
        if ("*".equals(context.get(0)))
        {
            this.manager.unhideAll(false, true);
            return;
        } */
        if (!bank.isHidden())
        {
            context.sendTranslated(POSITIVE, "The bank {input#bank} was not hidden!", bank.getName());
            return;
        }
        bank.setHidden(false);
        context.sendTranslated(POSITIVE, "The bank {input#bank} is no longer hidden!", bank.getName());
    }
}
