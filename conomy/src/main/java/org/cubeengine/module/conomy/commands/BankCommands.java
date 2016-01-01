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

import java.util.Set;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.conomy.Conomy;
import de.cubeisland.engine.module.conomy.account.Account;
import de.cubeisland.engine.module.conomy.account.BankAccount;
import de.cubeisland.engine.module.conomy.account.ConomyManager;
import de.cubeisland.engine.module.conomy.account.UserAccount;
import org.cubeengine.service.user.UserManager;

@Command(name = "bank", desc = "Manages your money in banks.")
public class BankCommands extends ContainerCommand
{
    private final ConomyManager manager;
    private final Conomy module;
    private UserManager um;

    public BankCommands(Conomy module, UserManager um)
    {
        super(module);
        this.module = module;
        this.um = um;
        this.manager = module.getManager();
    }

    @Alias(value = "bbalance")
    @Command(desc = "Shows the balance of the specified bank")
    public void balance(CommandSender context, @Optional BankAccount bank)
    {
        if (bank != null)
        {
            ensureIsVisible(context, bank);
            context.sendTranslated(POSITIVE, "Bank {name#bank} Balance: {input#balance}", bank.getName(), this.manager.format(bank.balance()));
            return;
        }
        if (context instanceof User)
        {
            Set<BankAccount> bankAccounts = this.manager.getBankAccounts((User)context);
            if (bankAccounts.size() == 1)
            {
                BankAccount bankAccount = bankAccounts.iterator().next();
                context.sendTranslated(POSITIVE, "Bank {name#bank} Balance: {input#balance}", bankAccount.getName(), this.manager.format(bankAccount.balance()));
                return;
            }
            // else more than 1 bank possible
        }
        context.sendTranslated(NEGATIVE, "Please do specify the bank you want to show the balance of!");
    }

    @Command(desc = "Lists all banks")
    public void list(CommandSender context, @Optional User owner) //Lists all banks [of given player]
    {
        String format = " - " + ChatFormat.YELLOW;
        if (owner != null)
        {
            Set<BankAccount> bankAccounts = this.manager.getBankAccounts(owner);
            if (bankAccounts.isEmpty())
            {
                context.sendTranslated(POSITIVE, "{user} is not owner of any bank!", owner);
                return;
            }
            context.sendTranslated(POSITIVE, "{user} is the owner of the following banks:", owner);

            for (BankAccount bankAccount : bankAccounts)
            {
                context.sendMessage(format + bankAccount.getName());
            }
            return;
        }
        Set<String> allBanks = this.manager.getBankNames(module.perms().BANK_SHOWHIDDEN.isAuthorized(context));
        if (allBanks.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "There are no banks currently!");
            return;
        }
        context.sendTranslated(POSITIVE, "The following banks are available:");
        for (String bank : allBanks)
        {
            context.sendMessage(format + bank);
        }
    }

    @Command(desc = "Invites a user to a bank")
    public void invite(CommandSender context, User player, @Optional BankAccount bank, @Flag boolean force)
    {
        force = force && module.perms().COMMAND_BANK_INVITE_FORCE.isAuthorized(context);
        if (bank != null)
        {
            if (!bank.needsInvite())
            {
                context.sendTranslated(NEUTRAL, "This bank does not need an invite to be able to join!");
                return;
            }
            if (force || !(context instanceof User) || bank.hasAccess((User)context))
            {
                bank.invite(player);
                context.sendTranslated(POSITIVE, "You invited {user} to the bank {name#bank}!", player, bank.getName());
                return;
            }
            context.sendTranslated(NEGATIVE, "You are not allowed to invite a player to this bank.");
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
                    context.sendTranslated(NEUTRAL, "This bank does not need an invite to be able to join!");
                    return;
                }
                if (force || account.hasAccess((User)context))
                {
                    account.invite(player);
                    context.sendTranslated(POSITIVE, "You invited {user} to the bank {name#bank}!", player, account.getName());
                    return;
                }
                context.sendTranslated(NEGATIVE, "You are not allowed to invite a player to this bank.");
                return;
            }
        }
        context.sendTranslated(NEGATIVE, "Please do specify the bank you want to invite to!");
    }

    @Command(desc = "Joins a bank")
    public void join(CommandSender context, BankAccount bank, @Default User player, @Flag boolean force)
    {
        if (!context.equals(player) && !module.perms().COMMAND_BANK_JOIN_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to let someone else join a bank!");
            return;
        }
        if (bank.isOwner(player))
        {
            if (context.equals(player))
            {
                context.sendTranslated(NEGATIVE, "You are already owner of this bank!");
                return;
            }
            context.sendTranslated(NEGATIVE, "{user} is already owner of this bank!", player);
            return;
        }
        if (bank.isMember(player))
        {
            if (context.equals(player))
            {
                context.sendTranslated(NEGATIVE, "You are already member of this bank!");
                return;
            }
            context.sendTranslated(NEGATIVE, "{user} is already member of this bank!", player);
            return;
        }
        force = force && module.perms().COMMAND_BANK_JOIN_FORCE.isAuthorized(context);
        if (!force || (bank.needsInvite() && !bank.isInvited(player)))
        {
            if (context.equals(player))
            {
                context.sendTranslated(NEGATIVE, "You need to be invited to join this bank!");
                return;
            }
            context.sendTranslated(NEGATIVE, "{user} needs to be invited to join this bank!", player);
            return;
        }
        bank.promoteToMember(player);
        context.sendTranslated(POSITIVE, "{user} is now a member of the {name#bank} bank!", player, bank.getName());
    }

    @Command(desc = "Leaves a bank")
    public void leave(CommandSender context, @Optional BankAccount bank, @Default User player)
    {
        if (bank == null)
        {
            Set<BankAccount> bankAccounts = this.manager.getBankAccounts(player);
            if (bankAccounts.size() != 1)
            {
                context.sendTranslated(NEGATIVE, "Please do specify a bank account to leave");
                return;
            }
            bank = bankAccounts.iterator().next();
        }
        if (!context.equals(player) && !module.perms().COMMAND_BANK_LEAVE_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to let someone else leave a bank!");
            return;
        }

        if (bank.hasAccess(player))
        {
            bank.kickUser(player);
            if (context.equals(player))
            {
                context.sendTranslated(POSITIVE, "You are no longer a member of the bank {name#bank}!", bank.getName());
                return;
            }
            context.sendTranslated(POSITIVE, "{user} is no longer a member of the bank {name#bank}!", player, bank.getName());
            return;
        }
        if (context.equals(player))
        {
            context.sendTranslated(NEGATIVE, "You are not a member if that bank!");
            return;
        }
        context.sendTranslated(NEGATIVE, "{user} is not a member of that bank!", player);
    }

    @Command(desc = "Removes a player from the invite-list")
    public void uninvite(CommandSender context, User player, BankAccount bank)
    {
        if (bank.isOwner(player) || module.perms().COMMAND_BANK_UNINVITE_FORCE.isAuthorized(context))
        {
            if (!bank.isInvited(player))
            {
                context.sendTranslated(NEGATIVE, "{user} is not invited to the bank {name#bank}!", player, bank.getName());
                return;
            }
            bank.uninvite(player);
            context.sendTranslated(NEGATIVE, "{user} is no longer invited to the bank {name#bank}!", player, bank.getName());
            return;
        }
        context.sendTranslated(NEGATIVE, "You are not allowed to uninvite someone from this bank!");
    }

    @Command(desc = "Rejects an invite from a bank")
    @Restricted(value = User.class, msg = "How did you manage to get invited in the first place?")
    public void rejectinvite(User context, BankAccount bank)
    {
        if (bank.isInvited(context))
        {
            context.sendTranslated(NEGATIVE, "You are not invited to the bank {name#bank}!", bank.getName());
            return;
        }
        bank.uninvite(context);
    }

    @Command(desc = "Creates a new bank")
    public void create(CommandSender context, String name, @Flag(longName = "nojoin", name = "nj") boolean noJoin)
    {
        if (this.manager.bankAccountExists(name))
        {
            context.sendTranslated(NEGATIVE, "There is already a bank names {input#bank}!", name);
            return;
        }
        BankAccount bankAccount = this.manager.getBankAccount(name, true);
        if (context instanceof User && !noJoin)
        {
            bankAccount.promoteToOwner((User)context);
        }
        context.sendTranslated(POSITIVE, "Created new Bank {name#bank}!", bankAccount.getName());
    }

    @Command(desc = "Deletes a bank")
    public void delete(CommandSender context, BankAccount bank)
    {
        if (context instanceof User)
        {
            if (bank.isOwner((User)context))
            {
                if (!module.perms().COMMAND_BANK_DELETE_OWN.isAuthorized(context))
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to delete your bank!");
                    return;
                }
            }
            else
            {
                if (!module.perms().COMMAND_BANK_DELETE_OTHER.isAuthorized(context))
                {
                    context.sendTranslated(NEGATIVE, "You are not owner of this bank!");
                    return;
                }
            }
        } // else ignore perms
        bank.delete();
        context.sendTranslated(POSITIVE, "You deleted the bank {name#bank}!", bank.getName());
    }

    @Command(desc = "Renames a bank")
    public void rename(CommandSender context, BankAccount bank, String newName, @Flag boolean force)
    {
        force = force && module.perms().COMMAND_BANK_RENAME_FORCE.isAuthorized(context);
        if (!force && context instanceof User && !bank.isOwner((User)context))
        {
            context.sendTranslated(NEGATIVE, "You need to be owner of a bank to rename it!");
            return;
        }
        if (bank.rename(newName))
        {
            context.sendTranslated(POSITIVE, "Bank renamed!");
            return;
        }
        context.sendTranslated(NEGATIVE, "Bank name {input#bank} has already been taken!", newName);
    }

    @Command(desc = "Sets given user as owner for a bank")
    public void setOwner(CommandSender context, BankAccount bank, User player, @Flag boolean force)
    {
        force = force && module.perms().COMMAND_BANK_SETOWNER_FORCE.isAuthorized(context);
        if ((force || context instanceof User) && !bank.isOwner((User)context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to set an owner for this bank!");
            return;
        }
        bank.promoteToOwner(player);
        context.sendTranslated(POSITIVE, "{user} is now owner of the bank {name#bank}!", player, bank.getName());
    }

    @Command(desc = "Lists the current invites of a bank")
    public void listinvites(CommandSender context, BankAccount bank)
    {
        ensureIsVisible(context, bank);
        if (bank.needsInvite())
        {
            if (context instanceof User && !bank.hasAccess((User)context))
            {
                if (!module.perms().COMMAND_BANK_LISTINVITES_OTHER.isAuthorized(context))
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to see the invites of this bank!");
                    return;
                }
            }
            Set<String> invites = bank.getInvites();
            if (invites.isEmpty())
            {
                String format = " - " + ChatFormat.DARK_GREEN;
                context.sendTranslated(POSITIVE, "The following players are invited:");
                for (String invite : invites)
                {
                    context.sendMessage(format + invite);
                }
                return;
            }
            context.sendTranslated(NEUTRAL, "There are currently no invites for this bank");
            return;
        }
        context.sendTranslated(NEUTRAL, "This bank does not require invites");
    }

    @Command(desc = "Lists the members of a bank")
    public void listmembers(CommandSender context, BankAccount bank)
    {
        ensureIsVisible(context, bank);
        Set<String> owners = bank.getOwners();
        Set<String> members = bank.getMembers();
        String format = " - " + ChatFormat.DARK_GREEN;
        if (owners.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "This bank has no owners!");
        }
        else
        {
            context.sendTranslated(NEUTRAL, "Owners:");
            for (String owner : owners)
            {
                context.sendMessage(format + owner);
            }
        }
        if (members.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "This bank has no members!");
        }
        else
        {
            context.sendTranslated(NEUTRAL, "Members:");
            for (String member : members)
            {
                context.sendMessage(format + member);
            }
        }
    }

    // Owners Members Invites Balance Hidden
    @Command(desc = "Shows bank information")
    public void info(CommandSender context, BankAccount bank) //list all members with their rank
    {
        ensureIsVisible(context, bank);
        context.sendTranslated(POSITIVE, "Bank Information for {name}:", bank.getName());
        context.sendTranslated(POSITIVE, "Owner:");
        for (String owner : bank.getOwners())
        {
            context.sendMessage(" - " + owner);
        }
        context.sendTranslated(POSITIVE, "Member:");
        for (String member : bank.getMembers())
        {
            context.sendMessage(" - " + member);
        }
        if (!(context instanceof User) || bank.isMember((User)context))
        {
            context.sendTranslated(POSITIVE, "Invited:");
            for (String invite : bank.getInvites())
            {
                context.sendMessage(" - " + invite);
            }
            context.sendTranslated(POSITIVE, "Current Balance: {input}", manager.format(context.getLocale(),
                                                                                        bank.balance()));
        }
        if (bank.isHidden())
        {
            context.sendTranslated(POSITIVE, "This bank is hidden for other players!");
        }
    }

    private void ensureIsVisible(CommandSender context, BankAccount account)
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
    public void deposit(User context, BankAccount bank, Double amount, @Flag boolean force)
    {
        UserAccount userAccount = this.manager.getUserAccount(context, this.manager.getAutoCreateUserAccount());
        if (userAccount == null)
        {
            context.sendTranslated(NEGATIVE, "You do not have an account!");
            return;
        }
        force = force && module.perms().COMMAND_BANK_DEPOSIT_FORCE.isAuthorized(context);
        if (userAccount.transactionTo(bank, amount, force))
        {
            context.sendTranslated(POSITIVE, "Deposited {input#amount} into {name#bank}! New Balance: {input#balance}", this.manager.format(amount), bank.getName(), this.manager.format( bank.balance()));
            return;
        }
        context.sendTranslated(NEGATIVE, "You cannot afford to spend that much!");
    }

    @Command(desc = "Withdraws given amount of money from the bank")
    @Restricted(value = User.class, msg = "You cannot withdraw from a bank as console!")
    public void withdraw(User context, BankAccount bank, Double amount, @Flag boolean force)//takes money from the bank
    {
        if (!bank.isOwner(context) && !module.perms().COMMAND_BANK_WITHDRAW_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "Only owners of the bank are allowed to withdraw from it!");
            return;
        }
        UserAccount userAccount = this.manager.getUserAccount(context, this.manager.getAutoCreateUserAccount());
        if (userAccount == null)
        {
            context.sendTranslated(NEGATIVE, "You do not have an account!");
            return;
        }
        force = force && module.perms().COMMAND_BANK_WITHDRAW_FORCE.isAuthorized(context);
        if (bank.transactionTo(userAccount, amount, force))
        {
            context.sendTranslated(POSITIVE, "Withdrawn {input#amount} from {name#bank}! New Balance: {input#balance}", this.manager.format(amount), bank.getName(), this.manager.format(bank.balance()));
            return;
        }
        context.sendTranslated(NEGATIVE, "The bank does not hold enough money to spend that much!");
    }

    @Command(desc = "Pays given amount of money as bank to another account")
    public void pay(CommandSender context, @Label("bank")BankAccount bankAccount, String targetAccount, Double amount,
                    @Flag boolean force, @Flag boolean bank)//pay AS bank to a player or other bank <name> [-bank]
    {
        if (!bankAccount.isOwner((User)context))
        {
            context.sendTranslated(NEGATIVE, "Only owners of the bank are allowed to spend the money from it!");
            return;
        }
        Account target;
        if (!bank)
        {
            User user = um.findUser(targetAccount);
            target = this.manager.getUserAccount(user, this.manager.getAutoCreateUserAccount());
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} has no account!", user);
                return;
            }
        }
        else
        {
            target = this.manager.getBankAccount(targetAccount, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "There is no bank account named {input#bank}!", targetAccount);
                return;
            }
        }
        if (amount < 0)
        {
            context.sendTranslated(NEGATIVE, "Sorry but robbing a bank is not allowed!");
            return;
        }
        force = force && module.perms().COMMAND_BANK_PAY_FORCE.isAuthorized(context);
        if (bankAccount.transactionTo(target, amount, force))
        {
            if (bank)
            {
                context.sendTranslated(POSITIVE, "Transferred {input#amount} from {name#bank} to {user}! New Balance: {input#balance}", this.manager.format(amount), bankAccount.getName(), target.getName(), this.manager.format(bankAccount.balance()));
            }
            else
            {
                context.sendTranslated(POSITIVE, "Transferred {input#amount} from {name#bank} to {name#bank} New Balance: {input#balance}", this.manager.format(amount), bankAccount.getName(), target.getName(), this.manager.format(bankAccount.balance()));
            }
            return;
        }
        context.sendTranslated(NEGATIVE, "The bank does not hold enough money to spend that much!");
    }
}
