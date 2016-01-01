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
import org.cubeengine.module.conomy.BankAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "bank", desc = "Administrative commands for Conomy Banks.")
public class EcoBankCommand extends ContainerCommand
{
    private final Conomy module;
    private ConomyService service;
    private I18n i18n;

    public EcoBankCommand(Conomy module, ConomyService service, I18n i18n)
    {
        super(module);
        this.module = module;
        this.service = service;
        this.i18n = i18n;
    }

    @Command(alias = "grant", desc = "Gives money to a bank or all banks")
    public void give(CommandSource context, BankAccount bank, Double amount)
    {
        String format = manager.format(amount);
        this.manager.transaction(null, bank, amount, true);
        i18n.sendTranslated(context, POSITIVE, "You gave {input#amount} to the bank {input#bank}!", format, bank.getName());
        for (User user : um.getOnlineUsers())
        {
            if (bank.isOwner(user))
            {
                user.sendTranslated(POSITIVE, "{user} granted {input#amount} to your bank {input#bank}!", context, format, bank.getName());
            }
        }
    }

    @Command(alias = "remove", desc = "Takes money from given bank or all banks")
    public void take(CommandSource context, BankAccount bank, Double amount)
    {
        String format = manager.format(amount);
        this.manager.transaction(bank, null, amount, true);
        i18n.sendTranslated(context, POSITIVE, "You took {input#amount} from the bank {input#bank}!", format, bank.getName());
        for (User onlineUser : um.getOnlineUsers())
        {
            if (bank.isOwner(onlineUser))
            {
                onlineUser.sendTranslated(POSITIVE, "{user} charged your bank {input#bank} for {input#amount}!", context.getSource(), bank.getName(), format);
            }
        }
    }

    @Command(desc = "Reset the money from given banks")
    public void reset(CommandSource context, BankAccount bank)
    {
        bank.reset();
        String format = this.manager.format(this.manager.getDefaultBalance());
        i18n.sendTranslated(context, POSITIVE, "The account of the bank {input#bank} got reset to {input#balance}!", bank.getName(), format);
        for (User onlineUser : um.getOnlineUsers())
        {
            if (bank.isOwner(onlineUser))
            {
                onlineUser.sendTranslated(POSITIVE, "{user} reset the money of your bank {input#bank} to {input#balance}!", context.getSource(), bank.getName(), format);
            }
        }
    }

    @Command(desc = "Sets the money from given banks")
    public void set(CommandSource context, BankAccount bank, Double amount)
    {
        String format = this.manager.format(amount);
        bank.set(amount);
        i18n.sendTranslated(context, POSITIVE, "The money of bank account {input#bank} got set to {input#balance}!", bank.getName(), format);
        for (User onlineUser : um.getOnlineUsers())
        {
            if (bank.isOwner(onlineUser))
            {
                onlineUser.sendTranslated(POSITIVE, "{user} set the money of your bank {input#bank} to {input#balance}!", context.getSource(), bank.getName(), format);
            }
        }
    }

    @Command(desc = "Hides the account of given bank")
    public void hide(CommandSource context, BankAccount bank)
    {
        if (bank.isHidden())
        {
            i18n.sendTranslated(context, POSITIVE, "The bank {input#bank} is already hidden!", bank.getName());
            return;
        }
        bank.setHidden(true);
        i18n.sendTranslated(context, POSITIVE, "The bank {input#bank} is now hidden!", bank.getName());
    }

    @Command(desc = "Unhides the account of given banks")
    public void unhide(CommandSource context, BankAccount bank)
    {
        if (!bank.isHidden())
        {
            i18n.sendTranslated(context, POSITIVE, "The bank {input#bank} was not hidden!", bank.getName());
            return;
        }
        bank.setHidden(false);
        i18n.sendTranslated(context, POSITIVE, "The bank {input#bank} is no longer hidden!", bank.getName());
    }
}
