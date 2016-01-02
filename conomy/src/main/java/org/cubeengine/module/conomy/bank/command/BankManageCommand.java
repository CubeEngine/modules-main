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
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.option.OptionSubjectData;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "manage", desc = "Management commands for Conomy Banks.")
public class BankManageCommand extends ContainerCommand
{
    private final Conomy module;
    private BankConomyService service;
    private I18n i18n;

    public BankManageCommand(Conomy module, BankConomyService service, I18n i18n)
    {
        super(module);
        this.module = module;
        this.service = service;
        this.i18n = i18n;
    }

    @Command(desc = "Sets the access level for a player in a bank")
    public void access(CommandSource context, User player, AccessLevel level, @Default BaseAccount.Virtual bank)
    {
        ((OptionSubjectData) player.getSubjectData()).setOption(bank.getActiveContexts(),
                "conomy.bank.access-level." + bank.getIdentifier(), String.valueOf(level.value));
        switch (level)
        {
            case NONE:
                if (context.equals(player))
                {
                    i18n.sendTranslated(context, NEUTRAL, "Revoked all access to {txt}", bank.getDisplayName());
                }
                else
                {
                    i18n.sendTranslated(context, NEUTRAL, "Revoked all access from {user} to {txt}", player, bank.getDisplayName());
                }
                break;
            case SEE:
                if (context.equals(player))
                {
                    i18n.sendTranslated(context, NEUTRAL, "Granted seeing {txt}", bank.getDisplayName());
                }
                else
                {
                    i18n.sendTranslated(context, NEUTRAL, "Granted seeing {txt} to {user}", bank.getDisplayName(), player);
                }
                break;
            case DEPOSIT:
                if (context.equals(player))
                {
                    i18n.sendTranslated(context, NEUTRAL, "Granted deposit access to {txt}", bank.getDisplayName());
                }
                else
                {
                    i18n.sendTranslated(context, NEUTRAL, "Granted deposit access to {user} to {txt}", player, bank.getDisplayName());
                }
                break;
            case WITHDRAW:
                if (context.equals(player))
                {
                    i18n.sendTranslated(context, NEUTRAL, "Granted withdraw access to {txt}", bank.getDisplayName());
                }
                else
                {
                    i18n.sendTranslated(context, NEUTRAL, "Granted withdraw access to {user} to {txt}", player, bank.getDisplayName());
                }
                break;
            case MANAGE:
                if (context.equals(player))
                {
                    i18n.sendTranslated(context, NEUTRAL, "Granted full access to {txt}", bank.getDisplayName());
                }
                else
                {
                    i18n.sendTranslated(context, NEUTRAL, "Granted full  access from {user} to {txt}", player, bank.getDisplayName());
                }
                break;
        }
    }

    @Command(desc = "Creates a new bank")
    public void create(CommandSource context, String name,
                       @Flag(longName = "hidden", name = "h") boolean hidden,
                       @Flag(longName = "invite", name = "i") boolean invite)
    {
        if (service.getAccount(name).isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "There is already a bank names {input#bank}!", name);
            return;
        }
        BaseAccount.Virtual bank = service.createVirtualAccount(name).map(BaseAccount.Virtual.class::cast).get();
        bank.setHidden(hidden);
        bank.setInvite(invite);
        i18n.sendTranslated(context, POSITIVE, "Created new Bank {txt#bank}!", bank.getDisplayName());
        if (context instanceof User)
        {
            access(context, ((User) context), AccessLevel.MANAGE, bank);

        }
    }

    @Command(desc = "Deletes a bank")
    public void delete(CommandSource context, BaseAccount.Virtual bank)
    {
        if (!service.hasAccess(bank, AccessLevel.MANAGE, context))
        {
            // TODO msg not allowed
            return;
        }
        bank.delete(); // TODO withdraw all money?
        i18n.sendTranslated(context, POSITIVE, "You deleted the bank {txt#bank}!", bank.getDisplayName());
    }

    @Command(desc = "Renames a bank")
    public void rename(CommandSource context, BaseAccount.Virtual bank, String newName)
    {
        if (!service.hasAccess(bank, AccessLevel.MANAGE, context))
        {
            // TODO check rename other perm
        }

        if (bank.rename(newName))
        {
            i18n.sendTranslated(context, POSITIVE, "Bank renamed!");
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "Bank name {input#bank} has already been taken!", newName);
    }

}
