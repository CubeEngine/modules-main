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

import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BankAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.command.annotation.ParameterPermission;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.List;
import java.util.Set;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "manage", desc = "Management commands for Conomy Banks.")
public class BankManageCommand extends ContainerCommand
{
    private final Conomy module;
    private ConomyService service;
    private I18n i18n;

    public BankManageCommand(Conomy module, ConomyService service, I18n i18n)
    {
        super(module);
        this.module = module;
        this.service = service;
        this.i18n = i18n;
    }

    @Command(desc = "Lists all banks")
    public void list(CommandSource context, @Optional User owner) //Lists all banks [of given player]
    {
        if (owner != null)
        {
            List<BankAccount> bankAccounts = service.getBanks(owner, AccessLevel.MANAGE);
            if (bankAccounts.isEmpty())
            {
                i18n.sendTranslated(context, POSITIVE, "{user} is not owner of any bank!", owner);
                return;
            }
            i18n.sendTranslated(context, POSITIVE, "{user} is the owner of the following banks:", owner);

            for (BankAccount bank : bankAccounts)
            {
                context.sendMessage(Text.of(" - ", TextColors.YELLOW, bank.getDisplayName()));
            }
            return;
        }

        List<BankAccount> bankAccounts = service.getBanks(context, AccessLevel.SEE);
        if (bankAccounts.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "There are no banks currently!");
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The following banks are available:");
        for (BankAccount bank : bankAccounts)
        {
            context.sendMessage(Text.of(" - ", TextColors.YELLOW, bank.getDisplayName()));
        }
    }


    @Command(desc = "Invites a user to a bank")
    public void invite(CommandSource context, User player, @Optional BankAccount bank, @Flag boolean force)
    {
        if (bank != null)
        {
            if (!bank.needsInvite())
            {
                i18n.sendTranslated(context, NEUTRAL, "This bank does not need an invite to be able to join!");
                return;
            }
            if (force || !(context instanceof User) || bank.hasAccess((User)context))
            {
                bank.invite(player);
                i18n.sendTranslated(context, POSITIVE, "You invited {user} to the bank {name#bank}!", player, bank.getName());
                return;
            }
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to invite a player to this bank.");
            return;
        }
        if (context instanceof User)
        {
            Set<BankAccount> bankAccounts = this.manager.getBankAccounts((User)context);
            if (bankAccounts.size() == 1)
            {
                BankAccount account = bankAccounts.iterator().next();
                if (!account.needsInvite())
                {
                    i18n.sendTranslated(context, NEUTRAL, "This bank does not need an invite to be able to join!");
                    return;
                }
                if (force || account.hasAccess((User)context))
                {
                    account.invite(player);
                    i18n.sendTranslated(context, POSITIVE, "You invited {user} to the bank {name#bank}!", player, account.getName());
                    return;
                }
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to invite a player to this bank.");
                return;
            }
        }
        i18n.sendTranslated(context, NEGATIVE, "Please do specify the bank you want to invite to!");
    }

    @Command(desc = "Joins a bank")
    public void join(CommandSource context, BankAccount bank, @Default User player, @Flag boolean force)
    {
        if (!context.equals(player) && !module.perms().COMMAND_BANK_JOIN_OTHER.isAuthorized(context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to let someone else join a bank!");
            return;
        }
        if (bank.isOwner(player))
        {
            if (context.equals(player))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are already owner of this bank!");
                return;
            }
            i18n.sendTranslated(context, NEGATIVE, "{user} is already owner of this bank!", player);
            return;
        }
        if (bank.isMember(player))
        {
            if (context.equals(player))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are already member of this bank!");
                return;
            }
            i18n.sendTranslated(context, NEGATIVE, "{user} is already member of this bank!", player);
            return;
        }
        force = force && module.perms().COMMAND_BANK_JOIN_FORCE.isAuthorized(context);
        if (!force || (bank.needsInvite() && !bank.isInvited(player)))
        {
            if (context.equals(player))
            {
                i18n.sendTranslated(context, NEGATIVE, "You need to be invited to join this bank!");
                return;
            }
            i18n.sendTranslated(context, NEGATIVE, "{user} needs to be invited to join this bank!", player);
            return;
        }
        bank.promoteToMember(player);
        i18n.sendTranslated(context, POSITIVE, "{user} is now a member of the {name#bank} bank!", player, bank.getName());
    }

    @Command(desc = "Leaves a bank")
    public void leave(CommandSource context, @Optional BankAccount bank, @Default User player)
    {
        if (bank == null)
        {
            Set<BankAccount> bankAccounts = this.manager.getBankAccounts(player);
            if (bankAccounts.size() != 1)
            {
                i18n.sendTranslated(context, NEGATIVE, "Please do specify a bank account to leave");
                return;
            }
            bank = bankAccounts.iterator().next();
        }
        if (!context.equals(player) && !module.perms().COMMAND_BANK_LEAVE_OTHER.isAuthorized(context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to let someone else leave a bank!");
            return;
        }

        if (bank.hasAccess(player))
        {
            bank.kickUser(player);
            if (context.equals(player))
            {
                i18n.sendTranslated(context, POSITIVE, "You are no longer a member of the bank {name#bank}!", bank.getName());
                return;
            }
            i18n.sendTranslated(context, POSITIVE, "{user} is no longer a member of the bank {name#bank}!", player, bank.getName());
            return;
        }
        if (context.equals(player))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not a member if that bank!");
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "{user} is not a member of that bank!", player);
    }

    @Command(desc = "Removes a player from the invite-list")
    public void uninvite(CommandSource context, User player, BankAccount bank, boolean force)
    {
        if (bank.isOwner(player) || module.perms().COMMAND_BANK_UNINVITE_FORCE.isAuthorized(context))
        {
            if (!bank.isInvited(player))
            {
                i18n.sendTranslated(context, NEGATIVE, "{user} is not invited to the bank {name#bank}!", player, bank.getName());
                return;
            }
            bank.uninvite(player);
            i18n.sendTranslated(context, NEGATIVE, "{user} is no longer invited to the bank {name#bank}!", player, bank.getName());
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "You are not allowed to uninvite someone from this bank!");
    }

    @Command(desc = "Rejects an invite from a bank")
    @Restricted(value = User.class, msg = "How did you manage to get invited in the first place?")
    public void rejectinvite(User context, BankAccount bank)
    {
        if (bank.isInvited(context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not invited to the bank {name#bank}!", bank.getName());
            return;
        }
        bank.uninvite(context);
    }

    @Command(desc = "Creates a new bank")
    public void create(CommandSource context, String name, @Flag(longName = "nojoin", name = "nj") boolean noJoin)
    {
        if (this.manager.bankAccountExists(name))
        {
            i18n.sendTranslated(context, NEGATIVE, "There is already a bank names {input#bank}!", name);
            return;
        }
        BankAccount bankAccount = this.manager.getBankAccount(name, true);
        if (context instanceof User && !noJoin)
        {
            bankAccount.promoteToOwner((User)context);
        }
        i18n.sendTranslated(context, POSITIVE, "Created new Bank {name#bank}!", bankAccount.getName());
    }

    @Command(desc = "Deletes a bank")
    public void delete(CommandSource context, BankAccount bank)
    {
        if (context instanceof User)
        {
            if (bank.isOwner((User)context))
            {
                if (!module.perms().COMMAND_BANK_DELETE_OWN.isAuthorized(context))
                {
                    i18n.sendTranslated(context, NEGATIVE, "You are not allowed to delete your bank!");
                    return;
                }
            }
            else
            {
                if (!module.perms().COMMAND_BANK_DELETE_OTHER.isAuthorized(context))
                {
                    i18n.sendTranslated(context, NEGATIVE, "You are not owner of this bank!");
                    return;
                }
            }
        } // else ignore perms
        bank.delete();
        i18n.sendTranslated(context, POSITIVE, "You deleted the bank {name#bank}!", bank.getName());
    }

    @Command(desc = "Renames a bank")
    public void rename(CommandSource context, BankAccount bank, String newName,
                       @ParameterPermission @Flag boolean other)
    {
        if (!force && context instanceof User && !bank.isOwner((User)context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You need to be owner of a bank to rename it!");
            return;
        }
        if (bank.rename(newName))
        {
            i18n.sendTranslated(context, POSITIVE, "Bank renamed!");
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "Bank name {input#bank} has already been taken!", newName);
    }

    @Command(desc = "Sets given user as owner for a bank")
    public void setOwner(CommandSource context, BankAccount bank, User player,
                         @ParameterPermission @Flag boolean other)
    {
        if ((other || context instanceof User) && !bank.isOwner((User)context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to set an owner for this bank!");
            return;
        }
        bank.promoteToOwner(player);
        i18n.sendTranslated(context, POSITIVE, "{user} is now owner of the bank {name#bank}!", player, bank.getName());
    }

    @Command(desc = "Lists the current invites of a bank")
    public void listinvites(CommandSource context, BankAccount bank)
    {
        ensureIsVisible(context, bank);
        if (bank.needsInvite())
        {
            if (context instanceof User && !bank.hasAccess((User)context))
            {
                if (!module.perms().COMMAND_BANK_LISTINVITES_OTHER.isAuthorized(context))
                {
                    i18n.sendTranslated(context, NEGATIVE, "You are not allowed to see the invites of this bank!");
                    return;
                }
            }
            Set<String> invites = bank.getInvites();
            if (invites.isEmpty())
            {
                String format = " - " + ChatFormat.DARK_GREEN;
                i18n.sendTranslated(context, POSITIVE, "The following players are invited:");
                for (String invite : invites)
                {
                    context.sendMessage(format + invite);
                }
                return;
            }
            i18n.sendTranslated(context, NEUTRAL, "There are currently no invites for this bank");
            return;
        }
        i18n.sendTranslated(context, NEUTRAL, "This bank does not require invites");
    }

    @Command(desc = "Lists the members of a bank")
    public void listmembers(CommandSource context, BankAccount bank)
    {
        ensureIsVisible(context, bank);
        Set<String> owners = bank.getOwners();
        Set<String> members = bank.getMembers();
        String format = " - " + ChatFormat.DARK_GREEN;
        if (owners.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "This bank has no owners!");
        }
        else
        {
            i18n.sendTranslated(context, NEUTRAL, "Owners:");
            for (String owner : owners)
            {
                context.sendMessage(format + owner);
            }
        }
        if (members.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "This bank has no members!");
        }
        else
        {
            i18n.sendTranslated(context, NEUTRAL, "Members:");
            for (String member : members)
            {
                context.sendMessage(format + member);
            }
        }
    }

    // Owners Members Invites Balance Hidden
    @Command(desc = "Shows bank information")
    public void info(CommandSource context, BankAccount bank) //list all members with their rank
    {
        ensureIsVisible(context, bank);
        i18n.sendTranslated(context, POSITIVE, "Bank Information for {name}:", bank.getName());
        i18n.sendTranslated(context, POSITIVE, "Owner:");
        for (String owner : bank.getOwners())
        {
            context.sendMessage(" - " + owner);
        }
        i18n.sendTranslated(context, POSITIVE, "Member:");
        for (String member : bank.getMembers())
        {
            context.sendMessage(" - " + member);
        }
        if (!(context instanceof User) || bank.isMember((User)context))
        {
            i18n.sendTranslated(context, POSITIVE, "Invited:");
            for (String invite : bank.getInvites())
            {
                context.sendMessage(" - " + invite);
            }
            i18n.sendTranslated(context, POSITIVE, "Current Balance: {input}", manager.format(context.getLocale(),
                    bank.balance()));
        }
        if (bank.isHidden())
        {
            i18n.sendTranslated(context, POSITIVE, "This bank is hidden for other players!");
        }
    }
}
