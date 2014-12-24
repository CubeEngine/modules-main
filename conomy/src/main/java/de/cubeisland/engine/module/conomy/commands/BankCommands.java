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
package de.cubeisland.engine.module.conomy.commands;

import java.util.Set;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.command.parameter.reader.ReaderException;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.module.conomy.Conomy;
import de.cubeisland.engine.module.conomy.account.Account;
import de.cubeisland.engine.module.conomy.account.BankAccount;
import de.cubeisland.engine.module.conomy.account.ConomyManager;
import de.cubeisland.engine.module.conomy.account.UserAccount;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Command(name = "bank", desc = "Manages your money in banks.")
public class BankCommands extends CommandContainer
{
    private final ConomyManager manager;
    private final Conomy module;

    public BankCommands(Conomy module)
    {
        super(module);
        this.module = module;
        this.manager = module.getManager();
    }

    @Alias(value = "bbalance")
    @Command(desc = "Shows the balance of the specified bank")
    @Params(positional = @Param(req = false, label = "bank", type = BankAccount.class))
    public void balance(CommandContext context)
    {
        if (context.hasPositional(0))
        {
            BankAccount bankAccount = context.get(0);
            ensureIsVisible(context, bankAccount);
            context.sendTranslated(POSITIVE, "Bank {name#bank} Balance: {input#balance}", bankAccount.getName(), this.manager.format(bankAccount.balance()));
            return;
        }
        if (context.getSource() instanceof User)
        {
            Set<BankAccount> bankAccounts = this.manager.getBankAccounts((User)context.getSource());
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
    @Params(positional = @Param(req = false, label = "owner", type = User.class))
    public void list(CommandContext context) //Lists all banks [of given player]
    {
        String format = " - " + ChatFormat.YELLOW;
        if (context.hasPositional(0))
        {
            User user = context.get(1);
            Set<BankAccount> bankAccounts = this.manager.getBankAccounts(user);
            if (bankAccounts.isEmpty())
            {
                context.sendTranslated(POSITIVE, "{user} is not owner of any bank!", user);
                return;
            }
            context.sendTranslated(POSITIVE, "{user} is the owner of the following banks:", user);

            for (BankAccount bankAccount : bankAccounts)
            {
                context.sendMessage(format + bankAccount.getName());
            }
            return;
        }
        Set<String> allBanks = this.manager.getBankNames(module.perms().BANK_SHOWHIDDEN.isAuthorized(context.getSource()));
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
    @Params(positional = {@Param(label = "player", type = User.class),
                          @Param(req = false, label = "bank", type = BankAccount.class)})
    @Flags(@Flag(longName = "force", name = "f"))
    public void invite(CommandContext context)
    {
        User user = context.get(0);
        boolean force = context.hasFlag("f") && module.perms().COMMAND_BANK_INVITE_FORCE.isAuthorized(context.getSource());
        if (context.hasPositional(1))
        {
            BankAccount account = context.get(1);
            if (!account.needsInvite())
            {
                context.sendTranslated(NEUTRAL, "This bank does not need an invite to be able to join!");
                return;
            }
            if (force || !(context.getSource() instanceof User) || account.hasAccess((User)context.getSource()))
            {
                account.invite(user);
                context.sendTranslated(POSITIVE, "You invited {user} to the bank {name#bank}!", user, account.getName());
                return;
            }
            context.sendTranslated(NEGATIVE, "You are not allowed to invite a player to this bank.");
            return;
        }
        if (context.getSource() instanceof User)
        {
            Set<BankAccount> bankAccounts = this.manager.getBankAccounts((User)context.getSource());
            if (bankAccounts.size() == 1)
            {
                BankAccount account = bankAccounts.iterator().next();
                if (!account.needsInvite())
                {
                    context.sendTranslated(NEUTRAL, "This bank does not need an invite to be able to join!");
                    return;
                }
                if (force || account.hasAccess((User)context.getSource()))
                {
                    account.invite(user);
                    context.sendTranslated(POSITIVE, "You invited {user} to the bank {name#bank}!", user, account.getName());
                    return;
                }
                context.sendTranslated(NEGATIVE, "You are not allowed to invite a player to this bank.");
                return;
            }
        }
        context.sendTranslated(NEGATIVE, "Please do specify the bank you want to invite to!");
    }

    @Command(desc = "Joins a bank")
    @Params(positional = {@Param(label = "bank", type = BankAccount.class),
                         @Param(req = false, label = "player", type = User.class)})
    @Flags(@Flag(longName = "force", name = "f"))
    public void join(CommandContext context)
    {
        User user;
        boolean other = false;
        if (context.hasPositional(1))
        {
            if (!module.perms().COMMAND_BANK_JOIN_OTHER.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to let someone else join a bank!");
                return;
            }
            user = context.get(1);
            other = true;
        }
        else if (context.getSource() instanceof User)
        {
            user = (User)context.getSource();
        }
        else
        {
            context.sendTranslated(NEGATIVE, "Please specify a player to join!");
            return;
        }
        BankAccount account = context.get(0);
        if (account.isOwner(user))
        {
            if (other)
            {
                context.sendTranslated(NEGATIVE, "{user} is already owner of this bank!", user);
                return;
            }
            context.sendTranslated(NEGATIVE, "You are already owner of this bank!");
            return;
        }
        if (account.isMember(user))
        {
            if (other)
            {
                context.sendTranslated(NEGATIVE, "{user} is already member of this bank!", user);
                return;
            }
            context.sendTranslated(NEGATIVE, "You are already member of this bank!");
            return;
        }
        boolean force = context.hasFlag("f") && module.perms().COMMAND_BANK_JOIN_FORCE.isAuthorized(context.getSource());
        if (!force || (account.needsInvite() && !account.isInvited(user)))
        {
            if (other)
            {
                context.sendTranslated(NEGATIVE, "{user} needs to be invited to join this bank!", user);
                return;
            }
            context.sendTranslated(NEGATIVE, "You need to be invited to join this bank!");
            return;
        }
        account.promoteToMember(user);
        context.sendTranslated(POSITIVE, "{user} is now a member of the {name#bank} bank!", user, account.getName());
    }

    @Command(desc = "Leaves a bank")
    @Params(positional = {@Param(req = false, label = "bank", type = BankAccount.class),
                          @Param(req = false, label = "player", type = User.class)})
    public void leave(CommandContext context)
    {
        if (context.hasPositional(0))
        {
            User user;
            boolean other = false;
            if (context.hasPositional(1))
            {
                if (!module.perms().COMMAND_BANK_LEAVE_OTHER.isAuthorized(context.getSource()))
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to let someone else leave a bank!");
                    return;
                }
                user = context.get(1);
                other = true;
            }
            else if (context.getSource() instanceof User)
            {
                user = (User)context.getSource();
            }
            else
            {
                context.sendTranslated(NEGATIVE, "Please specify a player to leave!");
                return;
            }
            BankAccount account;
            if (context.hasPositional(0))
            {
                account = context.get(0);
            }
            else
            {
                Set<BankAccount> bankAccounts = this.manager.getBankAccounts(user);
                if (bankAccounts.size() == 1)
                {
                    account = bankAccounts.iterator().next();
                }
                else
                {
                    context.sendTranslated(NEGATIVE, "Please do specify a bank account to leave");
                    return;
                }
            }
            if (account.hasAccess(user))
            {
                account.kickUser(user);
                if (other)
                {
                    context.sendTranslated(POSITIVE, "{user} is no longer a member of the bank {name#bank}!", user, account.getName());
                    return;
                }
                context.sendTranslated(POSITIVE, "You are no longer a member of the bank {name#bank}!", account.getName());
                return;
            }
            if (other)
            {
                context.sendTranslated(NEGATIVE, "{user} is not a member of that bank!", user);
                return;
            }
            context.sendTranslated(NEGATIVE, "You are not a member if that bank!");
        }
        context.sendTranslated(NEUTRAL, "You have to specify a bank to leave!");
    }

    @Command(desc = "Removes a player from the invite-list")
    @Params(positional = {@Param(label = "player", type = User.class),
                          @Param(label = "bank", type = BankAccount.class)})
    public void uninvite(CommandContext context)
    {
        User user = context.get(0);
        BankAccount bankAccount = context.get(1);
        if (bankAccount.isOwner(user) || module.perms().COMMAND_BANK_UNINVITE_FORCE.isAuthorized(context.getSource()))
        {
            if (!bankAccount.isInvited(user))
            {
                context.sendTranslated(NEGATIVE, "{user} is not invited to the bank {name#bank}!", user, bankAccount.getName());
                return;
            }
            bankAccount.uninvite(user);
            context.sendTranslated(NEGATIVE, "{user} is no longer invited to the bank {name#bank}!", user, bankAccount.getName());
            return;
        }
        context.sendTranslated(NEGATIVE, "You are not allowed to uninvite someone from this bank!");
    }

    @Command(desc = "Rejects an invite from a bank")
    @Params(positional = @Param(label = "bank", type = BankAccount.class))
    public void rejectinvite(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            User user = (User)context.getSource();
            BankAccount bankAccount = context.get(0);
            if (bankAccount.isInvited(user))
            {
                context.sendTranslated(NEGATIVE, "You are not invited to the bank {name#bank}!", bankAccount.getName());
                return;
            }
            bankAccount.uninvite(user);
            return;
        }
        context.sendTranslated(NEGATIVE, "How did you manage to get invited in the first place?");
    }

    @Command(desc = "Creates a new bank")
    @Params(positional = @Param(label = "name"))
    @Flags(@Flag(longName = "nojoin", name = "nj"))
    public void create(CommandContext context)
    {
        if (this.manager.bankAccountExists(context.getString(0)))
        {
            context.sendTranslated(NEGATIVE, "There is already a bank names {input#bank}!", context.get(0));
        }
        else
        {
            BankAccount bankAccount = this.manager.getBankAccount(context.getString(0), true);
            if (context.getSource() instanceof User && !context.hasFlag("nj"))
            {
                bankAccount.promoteToOwner((User)context.getSource());
            }
            context.sendTranslated(POSITIVE, "Created new Bank {name#bank}!", bankAccount.getName());
        }
    }

    @Command(desc = "Deletes a bank")
    @Params(positional = @Param(label = "bank", type = BankAccount.class))
    public void delete(CommandContext context)
    {
        BankAccount account = context.get(0);
        if (context.getSource() instanceof User)
        {
            if (account.isOwner((User)context.getSource()))
            {
                if (!module.perms().COMMAND_BANK_DELETE_OWN.isAuthorized(context.getSource()))
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to delete your bank!");
                    return;
                }
            }
            else
            {
                if (!module.perms().COMMAND_BANK_DELETE_OTHER.isAuthorized(context.getSource()))
                {
                    context.sendTranslated(NEGATIVE, "You are not owner of this bank!");
                    return;
                }
            }
        } // else ignore perms
        account.delete();
        context.sendTranslated(POSITIVE, "You deleted the bank {name#bank}!", account.getName());
    }

    @Command(desc = "Renames a bank")
    @Params(positional = {@Param(label = "name",type = BankAccount.class),
                          @Param(label = "new name")})
    @Flags(@Flag(longName = "force", name = "f"))
    public void rename(CommandContext context)
    {
        BankAccount account = context.get(0);
        boolean force = context.hasFlag("f") && module.perms().COMMAND_BANK_RENAME_FORCE.isAuthorized(context.getSource());
        if (!force && context.getSource() instanceof User)
        {
            if (!account.isOwner((User)context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You need to be owner of a bank to rename it!");
                return;
            }
        }
        if (account.rename(context.getString(1)))
        {
            context.sendTranslated(POSITIVE, "Bank renamed!");
            return;
        }
        context.sendTranslated(NEGATIVE, "Bank name {input#bank} has already been taken!", context.get(1));
    }

    @Command(desc = "Sets given user as owner for a bank")
    @Params(positional = {@Param(label = "bank", type = BankAccount.class),
                          @Param(label = "player", type = User.class)})
    @Flags(@Flag(longName = "force", name = "f"))
    public void setOwner(CommandContext context)
    {
        User user = context.get(1);
        BankAccount account = context.get(0);
        boolean force = context.hasFlag("f") && module.perms().COMMAND_BANK_SETOWNER_FORCE.isAuthorized(context.getSource());
        if (force || context.getSource() instanceof User)
        {
            if (!account.isOwner((User)context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to set an owner for this bank!");
                return;
            }
        }
        account.promoteToOwner(user);
        context.sendTranslated(POSITIVE, "{user} is now owner of the bank {name#bank}!", user, account.getName());
    }

    @Command(desc = "Lists the current invites of a bank")
    @Params(positional = @Param(label = "bank", type = BankAccount.class))
    public void listinvites(CommandContext context)
    {
        BankAccount account = context.get(0);
        ensureIsVisible(context, account);
        if (account.needsInvite())
        {
            if (context.getSource() instanceof User && !account.hasAccess((User)context.getSource()))
            {
                if (!module.perms().COMMAND_BANK_LISTINVITES_OTHER.isAuthorized(context.getSource()))
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to see the invites of this bank!");
                    return;
                }
            }
            Set<String> invites = account.getInvites();
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
    @Params(positional = @Param(label = "bank", type = BankAccount.class))
    public void listmembers(CommandContext context)
    {
        BankAccount account = context.get(0);
        ensureIsVisible(context, account);
        Set<String> owners = account.getOwners();
        Set<String> members = account.getMembers();
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
    @Params(positional = @Param(label = "bank", type = BankAccount.class))
    public void info(CommandContext context) //list all members with their rank
    {
        BankAccount account = context.get(0);
        ensureIsVisible(context, account);
        context.sendTranslated(POSITIVE, "Bank Information for {name}:", context.getString(0));
        context.sendTranslated(POSITIVE, "Owner:");
        for (String owner : account.getOwners())
        {
            context.sendMessage(" - " + owner);
        }
        context.sendTranslated(POSITIVE, "Member:");
        for (String member : account.getMembers())
        {
            context.sendMessage(" - " + member);
        }
        if (!context.isSource(User.class) || account.isMember((User)context.getSource()))
        {
            context.sendTranslated(POSITIVE, "Invited:");
            for (String invite : account.getInvites())
            {
                context.sendMessage(" - " + invite);
            }
            context.sendTranslated(POSITIVE, "Current Balance: {input}", manager.format(context.getSource().getLocale(), account.balance()));
        }
        if (account.isHidden())
        {
            context.sendTranslated(POSITIVE, "This bank is hidden for other players!");
        }
    }

    private void ensureIsVisible(CommandContext context, BankAccount account)
    {
        if (!account.isHidden() || module.perms().BANK_SHOWHIDDEN.isAuthorized(context.getSource())
            || !context.isSource(User.class))
        {
            return;
        }
        if (account.hasAccess((User)context.getSource()))
        {
            throw new ReaderException(context.getSource().getTranslation(NEGATIVE, "There is no bank account named {input#name}!", account.getName()));
        }
    }

    @Command(desc = "Deposits given amount of money into the bank")
    @Params(positional = {@Param(label = "bank", type = BankAccount.class),
                          @Param(label = "amount", type = Double.class)})
    @Flags(@Flag(longName = "force", name = "f"))
    public void deposit(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            BankAccount account = context.get(0);
            Double amount = context.get(1);
            UserAccount userAccount = this.manager.getUserAccount((User)context.getSource(), this.manager.getAutoCreateUserAccount());
            if (userAccount == null)
            {
                context.sendTranslated(NEGATIVE, "You do not have an account!");
                return;
            }
            boolean force = context.hasFlag("f") && module.perms().COMMAND_BANK_DEPOSIT_FORCE.isAuthorized(context.getSource());
            if (userAccount.transactionTo(account, amount, force))
            {
                context.sendTranslated(POSITIVE, "Deposited {input#amount} into {name#bank}! New Balance: {input#balance}", this.manager.format(amount), account.getName(), this.manager.format(account.balance()));
                return;
            }
            context.sendTranslated(NEGATIVE, "You cannot afford to spend that much!");
            return;
        }
        context.sendTranslated(NEGATIVE, "You cannot deposit into a bank as console!");
    }

    @Command(desc = "Withdraws given amount of money from the bank")
    @Params(positional = {@Param(label = "bank", type = BankAccount.class),
                          @Param(label = "amount", type = Double.class)})
    @Flags(@Flag(longName = "force", name = "f"))
    public void withdraw(CommandContext context)//takes money from the bank
    {
        if (context.getSource() instanceof User)
        {
            BankAccount account = context.get(0);
            if (!account.isOwner((User)context.getSource()) && !module.perms().COMMAND_BANK_WITHDRAW_OTHER.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "Only owners of the bank are allowed to withdraw from it!");
                return;
            }
            Double amount = context.get(1);
            UserAccount userAccount = this.manager.getUserAccount((User)context.getSource(), this.manager.getAutoCreateUserAccount());
            if (userAccount == null)
            {
                context.sendTranslated(NEGATIVE, "You do not have an account!");
                return;
            }
            boolean force = context.hasFlag("f") && module.perms().COMMAND_BANK_WITHDRAW_FORCE.isAuthorized(context.getSource());
            if (account.transactionTo(userAccount, amount, force))
            {
                context.sendTranslated(POSITIVE, "Withdrawn {input#amount} from {name#bank}! New Balance: {input#balance}", this.manager.format(amount), account.getName(), this.manager.format(account.balance()));
                return;
            }
            context.sendTranslated(NEGATIVE, "The bank does not hold enough money to spend that much!");
            return;
        }
        context.sendTranslated(NEGATIVE, "You cannot withdraw from a bank as console!");
    }

    @Command(desc = "Pays given amount of money as bank to another account")
    @Params(positional = {@Param(label = "bank", type = BankAccount.class),
                          @Param(label = "target-account"),
                          @Param(label = "amount", type = Double.class)})
    @Flags({@Flag(longName = "force", name = "f"),
            @Flag(longName = "bank", name = "b")})
    public void pay(CommandContext context)//pay AS bank to a player or other bank <name> [-bank]
    {
        BankAccount account = context.get(0);
        if (!account.isOwner((User)context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "Only owners of the bank are allowed to spend the money from it!");
            return;
        }
        Account target;
        if (context.hasFlag("b"))
        {
            User user = this.module.getCore().getUserManager().findUser(context.getString(1));
            target = this.manager.getUserAccount(user, this.manager.getAutoCreateUserAccount());
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} has no account!", user);
                return;
            }
        }
        else
        {
            target = this.manager.getBankAccount(context.getString(1), false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "There is no bank account named {input#bank}!", context.get(1));
                return;
            }
        }
        Double amount = context.get(2);
        if (amount < 0)
        {
            context.sendTranslated(NEGATIVE, "Sorry but robbing a bank is not allowed!");
            return;
        }
        boolean force = context.hasFlag("f") && module.perms().COMMAND_BANK_PAY_FORCE.isAuthorized(context.getSource());
        if (account.transactionTo(target, amount, force))
        {
            if (context.hasFlag("b"))
            {
                context.sendTranslated(POSITIVE, "Transferred {input#amount} from {name#bank} to {user}! New Balance: {input#balance}", this.manager.format(amount), account.getName(), target.getName(), this.manager.format(account.balance()));
            }
            else
            {
                context.sendTranslated(POSITIVE, "Transferred {input#amount} from {name#bank} to {name#bank} New Balance: {input#balance}", this.manager.format(amount), account.getName(), target.getName(), this.manager.format(account.balance()));
            }
            return;
        }
        context.sendTranslated(NEGATIVE, "The bank does not hold enough money to spend that much!");
    }
}
