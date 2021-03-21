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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
@Command(name = "manage", desc = "Management commands for Conomy Banks.")
public class BankManageCommand extends DispatcherCommand
{
    private BankConomyService service;
    private I18n i18n;
    @Inject
    public BankManageCommand(BankConomyService service, I18n i18n)
    {
        this.service = service;
        this.i18n = i18n;
    }

    @Command(desc = "Sets the access level for a player in a bank")
    public void access(CommandCause context, User player, AccessLevel level, @Default BaseAccount.Virtual bank)
    {
        player.subjectData().setOption(bank.activeContexts(),
                                          "conomy.bank.access-level." + bank.identifier(), String.valueOf(level.value));
        switch (level)
        {
            case NONE:
                if (context.equals(player))
                {
                    i18n.send(context, NEUTRAL, "Revoked all access to {account}", bank);
                }
                else
                {
                    i18n.send(context, NEUTRAL, "Revoked all access from {user} to {account}", player, bank);
                }
                break;
            case SEE:
                if (context.equals(player))
                {
                    i18n.send(context, NEUTRAL, "Granted seeing {account}", bank);
                }
                else
                {
                    i18n.send(context, NEUTRAL, "Granted seeing {account} to {user}", bank, player);
                }
                break;
            case DEPOSIT:
                if (context.equals(player))
                {
                    i18n.send(context, NEUTRAL, "Granted deposit access to {account}", bank);
                }
                else
                {
                    i18n.send(context, NEUTRAL, "Granted deposit access to {user} to {account}", player, bank);
                }
                break;
            case WITHDRAW:
                if (context.equals(player))
                {
                    i18n.send(context, NEUTRAL, "Granted withdraw access to {account}", bank);
                }
                else
                {
                    i18n.send(context, NEUTRAL, "Granted withdraw access to {user} to {account}", player, bank);
                }
                break;
            case MANAGE:
                if (context.equals(player))
                {
                    i18n.send(context, NEUTRAL, "Granted full access to {account}", bank);
                }
                else
                {
                    i18n.send(context, NEUTRAL, "Granted full  access from {user} to {account}", player, bank);
                }
                break;
        }
    }

    @Command(desc = "Creates a new bank")
    public void create(CommandCause context, String name,
                       @Flag(longName = "hidden", value = "h") boolean hidden,
                       @Flag(longName = "invite", value = "i") boolean invite)
    {
        if (service.hasAccount(name))
        {
            i18n.send(context, NEGATIVE, "There is already a bank names {input#bank}!", name);
            return;
        }
        BaseAccount.Virtual bank = service.orCreateAccount(name).map(BaseAccount.Virtual.class::cast).get();
        bank.setHidden(hidden);
        bank.setInvite(invite);
        i18n.send(context, POSITIVE, "Created new Bank {account}!", bank);
        if (context instanceof User)
        {
            access(context, ((User) context), AccessLevel.MANAGE, bank);

        }
    }

    @Command(desc = "Deletes a bank")
    public void delete(CommandCause context, BaseAccount.Virtual bank)
    {
        if (true)
        {
            // TODO
            context.sendMessage(Identity.nil(), Component.text("NOT IMPLEMENTED YET"));
            return;
        }

        if (!service.hasAccess(bank, AccessLevel.MANAGE, context))
        {
            // TODO msg not allowed
            return;
        }
        //bank.delete(); // TODO withdraw all money?
        i18n.send(context, POSITIVE, "You deleted the bank {account}!", bank);
    }

    @Command(desc = "Renames a bank")
    public void rename(CommandCause context, BaseAccount.Virtual bank, String newName)
    {
        if (!service.hasAccess(bank, AccessLevel.MANAGE, context))
        {
            // TODO check rename other perm
        }

        if (bank.rename(newName))
        {
            i18n.send(context, POSITIVE, "Bank renamed!");
            return;
        }
        i18n.send(context, NEGATIVE, "Bank name {input#bank} has already been taken!", newName);
    }

}
