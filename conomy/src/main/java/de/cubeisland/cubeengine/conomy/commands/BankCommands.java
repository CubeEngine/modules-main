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
package de.cubeisland.cubeengine.conomy.commands;

import java.util.Set;

import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.ContainerCommand;
import de.cubeisland.cubeengine.core.command.parameterized.Flag;
import de.cubeisland.cubeengine.core.command.parameterized.ParameterizedContext;
import de.cubeisland.cubeengine.core.command.reflected.Alias;
import de.cubeisland.cubeengine.core.command.reflected.Command;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.conomy.Conomy;
import de.cubeisland.cubeengine.conomy.ConomyPermissions;
import de.cubeisland.cubeengine.conomy.account.Account;
import de.cubeisland.cubeengine.conomy.account.BankAccount;
import de.cubeisland.cubeengine.conomy.account.ConomyManager;
import de.cubeisland.cubeengine.conomy.account.UserAccount;

public class BankCommands extends ContainerCommand
{
    private final ConomyManager manager;
    public BankCommands(Conomy module)
    {
        super(module, "bank", "Manages your money in banks.");
        this.manager = module.getManager();
    }

    @Alias(names = "bbalance")
    @Command(desc = "Shows the balance of the specified bank",
             usage = "[name]",
             flags = @Flag(longName = "showHidden", name = "f"),
             max = 1)
    public void balance(ParameterizedContext context)
    {
        if (context.hasArg(0))
        {
            BankAccount bankAccount = this.manager.getBankAccount(context.getString(0), false);
            if (bankAccount == null)
            {
                context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(0));
            }
            else
            {
                boolean showHidden = context.hasFlag("f") && ConomyPermissions.COMMAND_BANK_BALANCE_SHOWHIDDEN.isAuthorized(context.getSender());
                if (!showHidden && bankAccount.isHidden())
                {
                    if (context.getSender() instanceof User && !bankAccount.hasAccess((User)context.getSender()))
                    {
                        context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(0));
                        return;
                    }
                }
                context.sendTranslated("&aBank &6%s&a Balance: &6%s", bankAccount.getName(), this.manager.format(bankAccount.balance()));
            }
        }
        else if (context.getSender() instanceof User)
        {
            Set<BankAccount> bankAccounts = this.manager.getBankAccounts((User)context.getSender());
            if (bankAccounts.size() == 1)
            {
                BankAccount bankAccount = bankAccounts.iterator().next();
                context.sendTranslated("&aBank &6%s&a Balance: &6%s", bankAccount.getName(), this.manager.format(bankAccount.balance()));
                return;
            }
        }
        context.sendTranslated("&cPlease do specify the bank you want to show the balance of!");
    }

    public void list(CommandContext context) //Lists all banks [of given player]
    {
        // TODO
    }

    @Command(desc = "Invites a user to a bank",
             usage = "<user> [bank]",
             flags = @Flag(longName = "force", name = "f"),
             max = 2, min = 1)
    public void invite(ParameterizedContext context)
    {
        User user = context.getUser(0);
        if (user == null)
        {
            context.sendTranslated("&cUser &2%s&c not found!", context.getString(0));
            return;
        }
        boolean force = context.hasFlag("f")
            && ConomyPermissions.COMMAND_BANK_INVITE_FORCE.isAuthorized(context.getSender());
        if (context.hasArg(1))
        {
            BankAccount account = this.getBankAccount(context.getString(1));
            if (account == null)
            {
                context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(1));
                return;
            }
            if (!account.needsInvite())
            {
                context.sendTranslated("&eThis bank does not need an invite to be able to join!");
                return;
            }
            if (force || !(context.getSender() instanceof User) || account.hasAccess((User)context.getSender()))
            {
                account.invite(user);
                context.sendTranslated("&aYou invited &2%s&a to the bank &6%s&a!", user.getName(), account.getName());
                return;
            }
            context.sendTranslated("&cYou are not allowed to invite a player to this bank.");
            return;
        }
        if (context.getSender() instanceof User)
        {
            Set<BankAccount> bankAccounts = this.manager.getBankAccounts((User)context.getSender());
            if (bankAccounts.size() == 1)
            {
                BankAccount account = bankAccounts.iterator().next();
                if (!account.needsInvite())
                {
                    context.sendTranslated("&eThis bank does not need an invite to be able to join!");
                    return;
                }
                if (force || account.hasAccess((User)context.getSender()))
                {
                    account.invite(user);
                    context.sendTranslated("&aYou invited &2%s&a to the bank &6%s&a!", user.getName(), account.getName());
                    return;
                }
                context.sendTranslated("&cYou are not allowed to invite a player to this bank.");
                return;
            }
        }
        context.sendTranslated("&cPlease do specify the bank you want to invite to!");
    }

    @Command(desc = "Joins a bank",
             usage = "<bank> [user]",
             flags = @Flag(longName = "force", name = "f"),
             max = 2, min = 1)
    public void join(ParameterizedContext context)
    {
        User user;
        boolean other = false;
        if (context.hasArg(1))
        {
            if (!ConomyPermissions.COMMAND_BANK_JOIN_OTHER.isAuthorized(context.getSender()))
            {
                context.sendTranslated("&cYou are not allowed to let someone else join a bank!");
                return;
            }
            user = context.getUser(1);
            if (user == null)
            {
                context.sendTranslated("&cUser &2%s&c not found!", context.getString(1));
                return;
            }
            other = true;
        }
        else if (context.getSender() instanceof User)
        {
            user = (User)context.getSender();
        }
        else
        {
            context.sendTranslated("&cPlease specify a player to join!");
            return;
        }
        BankAccount account = this.getBankAccount(context.getString(0));
        if (account == null)
        {
            context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(0));
            return;
        }
        if (account.isOwner(user))
        {
            if (other)
            {
                context.sendTranslated("&2&s&c is already owner of this bank!", user.getName());
                return;
            }
            context.sendTranslated("&cYou are already owner of this bank!");
            return;
        }
        if (account.isMember(user))
        {
            if (other)
            {
                context.sendTranslated("&2&s&c is already member of this bank!", user.getName());
                return;
            }
            context.sendTranslated("&cYou are already member of this bank!");
            return;
        }
        boolean force = context.hasFlag("f") && ConomyPermissions.COMMAND_BANK_JOIN_FORCE.isAuthorized(context.getSender());
        if (!force || (account.needsInvite() && !account.isInvited(user)))
        {
            if (other)
            {
                context.sendTranslated("&2&s&c needs to be invited to join this bank!", user.getName());
                return;
            }
            context.sendTranslated("&cYou need to be invited to join this bank!");
            return;
        }
        account.promoteToMember(user);
        context.sendTranslated("&2%s&a is now a member of the &6%s&a bank!", user.getName(), account.getName());
    }

    @Command(desc = "Leaves a bank",
             usage = "[bank] [user]",
             max = 2, min = 0)
    public void leave(CommandContext context)
    {
        if (context.hasArg(0))
        {
            User user;
            boolean other = false;
            if (context.hasArg(1))
            {
                if (!ConomyPermissions.COMMAND_BANK_LEAVE_OTHER.isAuthorized(context.getSender()))
                {
                    context.sendTranslated("&cYou are not allowed to let someone else leave a bank!");
                    return;
                }
                user = context.getUser(1);
                if (user == null)
                {
                    context.sendTranslated("&cUser &2%s&c not found!", context.getString(1));
                    return;
                }
                other = true;
            }
            else if (context.getSender() instanceof User)
            {
                user = (User)context.getSender();
            }
            else
            {
                context.sendTranslated("&cPlease specify a player to leave!");
                return;
            }
            BankAccount account;
            if (context.hasArg(0))
            {
                account = this.getBankAccount(context.getString(0));
                if (account == null)
                {
                    context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(0));
                    return;
                }
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
                    context.sendTranslated("&cPlease do specify a bank-account to leave");
                    return;
                }
            }
            if (account.hasAccess(user))
            {
                account.kickUser(user);
                if (other)
                {
                    context.sendTranslated("&2%s&a is no longer a member of the bank &6%s&a!", user.getName(), account.getName());
                    return;
                }
                context.sendTranslated("&aYou are no longer a member of the bank &6%s&a!", account.getName());
                return;
            }
            if (other)
            {
                context.sendTranslated("&2&s&c is not a member of that bank!", user.getName());
                return;
            }
            context.sendTranslated("&cYou are not a member if that bank!");
        }
    }


    public void uninvite(CommandContext context)
    {
        // TODO reject invite / uninvite
    }

    @Command(desc = "Creates a new bank",
             usage = "<name> [-nojoin]",
             flags = @Flag(longName = "nojoin", name = "nj"),
             max = 1, min = 1)
    public void create(ParameterizedContext context)
    {
        if (this.manager.bankAccountExists(context.getString(0)))
        {
            context.sendTranslated("&cThere is already a bank names &6%2&c!", context.getString(0));
        }
        else
        {
            BankAccount bankAccount = this.manager.getBankAccount(context.getString(0), true);
            if (context.getSender() instanceof User && !context.hasFlag("nj"))
            {
                bankAccount.promoteToOwner((User)context.getSender());
            }
            context.sendTranslated("&aCreated new Bank &6%s&a!", bankAccount.getName());
        }
    }

    @Command(desc = "Deletes a bank",
             usage = "<name>",
             max = 1, min = 1)
    public void delete(CommandContext context)
    {
        BankAccount account = this.getBankAccount(context.getString(0));
        if (account == null)
        {
            context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(0));
            return;
        }
        account.delete();
        context.sendTranslated("&aYou deleted the bank &6%s&a!", account.getName());
    }

    @Command(desc = "Renames a bank",
             usage = "<name> <new name>",
             flags = @Flag(longName = "force", name = "f"),
             max = 2, min = 2)
    public void rename(ParameterizedContext context)
    {
        BankAccount account = this.getBankAccount(context.getString(0));
        if (account == null)
        {
            context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(0));
            return;
        }
        boolean force = context.hasFlag("f") && ConomyPermissions.COMMAND_BANK_RENAME_FORCE.isAuthorized(context.getSender());
        if (!force && context.getSender() instanceof User)
        {
            if (!account.isOwner((User)context.getSender()))
            {
                context.sendTranslated("&cYou need to be owner of a bank to rename it!");
                return;
            }
        }
        if (account.rename(context.getString(1)))
        {
            context.sendTranslated("&aBank renamed!");
            return;
        }
        context.sendTranslated("&cThere is already a bank names &6%s&c!", context.getString(1));
    }

    @Command(desc = "Sets given user as owner for a bank",
             usage = "<bank-name> <user>",
             flags = @Flag(longName = "force", name = "f"),
             max = 2, min = 2)
    public void setOwner(ParameterizedContext context)
    {
        User user = context.getUser(1);
        if (user == null)
        {
            context.sendTranslated("&cUser &2%s&c not found!", context.getString(1));
            return;
        }
        BankAccount account = this.getBankAccount(context.getString(0));
        if (account == null)
        {
            context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(0));
            return;
        }
        boolean force = context.hasFlag("f") && ConomyPermissions.COMMAND_BANK_SETOWNER_FORCE.isAuthorized(context.getSender());
        if (force || context.getSender() instanceof User)
        {
            if (!account.isOwner((User)context.getSender()))
            {
                context.sendTranslated("&cYou are not allowed to set an owner for this bank!");
                return;
            }
        }
        account.promoteToOwner(user);
        context.sendTranslated("&2%s&a is now owner of the bank &6%s&a!", user.getName(), account.getName());
    }

    // TODO listinvites

    public void listmembers(CommandContext context)//list all members with their rank
    {}// TODO

    // TODO bank Info cmd
    // Owners Members Invites Balance Hidden

    @Command(desc = "Deposits given amount of money into the bank",
             usage = "<bank-name> <amount>",
             flags = @Flag(longName = "force", name = "f"),
             max = 2, min = 2)
    public void deposit(ParameterizedContext context)
    {
        if (context.getSender() instanceof User)
        {
            BankAccount account = this.getBankAccount(context.getString(0));
            if (account == null)
            {
                context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(0));
                return;
            }
            Double amount = context.getArg(1, Double.class, null);
            if (amount == null)
            {
                context.sendTranslated("&6%s&c is not a valid amount!", context.getString(1));
                return;
            }
            UserAccount userAccount = this.manager.getUserAccount((User)context.getSender(), this.manager
                .getAutoCreateUserAccount());
            if (userAccount == null)
            {
                context.sendTranslated("&cYou do not have an account!");
                return;
            }
            boolean force = context.hasFlag("f") && ConomyPermissions.COMMAND_BANK_DEPOSIT_FORCE.isAuthorized(context.getSender());
            if (userAccount.transactionTo(account, amount, force))
            {
                context.sendTranslated("&aDeposited &6%s&a into &6%s&a! New Balance: &6%s",
                           this.manager.format(amount), account.getName(), this.manager.format(account.balance()));
                return;
            }
            context.sendTranslated("&cYou cannot afford to spend that much!");
            return;
        }
        context.sendTranslated("&cYou cannot deposit into a bank as console!");
    }

    @Command(desc = "Withdraws given amount of money from the bank",
             usage = "<bank-name> <amount>",
             flags = @Flag(longName = "force", name = "f"),
             max = 2, min = 2)
    public void withdraw(ParameterizedContext context)//takes money from the bank
    {
        if (context.getSender() instanceof User)
        {
            BankAccount account = this.getBankAccount(context.getString(0));
            if (account == null)
            {
                context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(0));
                return;
            }
            if (!account.isOwner((User)context.getSender()))
            {
                context.sendMessage("&cOnly owners of the bank are allowed to withdraw from it!");
                return;
            }
            Double amount = context.getArg(1, Double.class, null);
            if (amount == null)
            {
                context.sendTranslated("&6%s&c is not a valid amount!", context.getString(1));
                return;
            }
            UserAccount userAccount = this.manager.getUserAccount((User)context.getSender(), this.manager.getAutoCreateUserAccount());
            if (userAccount == null)
            {
                context.sendTranslated("&cYou do not have an account!");
                return;
            }
            boolean force = context.hasFlag("f") && ConomyPermissions.COMMAND_BANK_WITHDRAW_FORCE.isAuthorized(context.getSender());
            if (account.transactionTo(userAccount, amount, force))
            {
                context.sendTranslated("&aWithdrawn &6%s&a from &6%s&a! New Balance: &6%s",
                                       this.manager.format(amount), account.getName(), this.manager.format(account.balance()));
                return;
            }
            context.sendTranslated("&cThe bank does not hold enough money to spend that much!");
            return;
        }
        context.sendTranslated("&cYou cannot withdraw from a bank as console!");
    }

    @Command(desc = "Pays given amount of money as bank to another account",
             usage = "<bank-name> <target-account> <amount> [-bank]",
             flags = {@Flag(longName = "force", name = "f"),
                      @Flag(longName = "bank", name = "b")},
             max = 3, min = 2)
    public void pay(ParameterizedContext context)//pay AS bank to a player or other bank <name> [-bank]
    {
        BankAccount account = this.getBankAccount(context.getString(0));
        if (account == null)
        {
            context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(0));
            return;
        }
        if (!account.isOwner((User)context.getSender()))
        {
            context.sendMessage("&cOnly owners of the bank are allowed to spend the money from it!");
            return;
        }
        Account target;
        if (context.hasFlag("b"))
        {
            User user = context.getUser(1);
            if (user == null)
            {
                context.sendTranslated("&cUser &2%s&c not found!", context.getString(1));
                return;
            }
            target = this.manager.getUserAccount(user, this.manager.getAutoCreateUserAccount());
            if (target == null)
            {
                context.sendTranslated("&2%s&c has no account!", user.getName());
                return;
            }
        }
        else
        {
            target = this.manager.getBankAccount(context.getString(1), false);
            if (target == null)
            {
                context.sendTranslated("&cThere is no bank-account named &6%s&c!", context.getString(1));
                return;
            }
        }
        Double amount = context.getArg(2, Double.class, null);
        if (amount == null)
        {
            context.sendTranslated("&6%s&c is not a valid amount!", context.getString(1));
            return;
        }
        boolean force = context.hasFlag("f") && ConomyPermissions.COMMAND_BANK_PAY_FORCE.isAuthorized(context.getSender());
        if (account.transactionTo(target, amount, force))
        {
            if (context.hasFlag("b"))
            {
                context.sendTranslated("&aTranfered &6%s&a from &6%s&a to &2&s&a! New Balance: &6%s",
                                       this.manager.format(amount), account.getName(), target.getName(), this.manager.format(account.balance()));
            }
            else
            {
                context.sendTranslated("&aTranfered &6%s&a from &6%s&a to &6&s&a! New Balance: &6%s",
                                       this.manager.format(amount), account.getName(), target.getName(), this.manager.format(account.balance()));
            }
            return;
        }
        context.sendTranslated("&cThe bank does not hold enough money to spend that much!");
    }

    private BankAccount getBankAccount(String name)
    {
        return this.manager.getBankAccount(name, false);
    }
}
