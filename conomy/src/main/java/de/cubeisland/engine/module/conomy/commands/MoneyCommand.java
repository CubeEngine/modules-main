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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import de.cubeisland.engine.command.CommandInvocation;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.command.methodic.parametric.Named;
import de.cubeisland.engine.command.methodic.parametric.Optional;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.user.UserList;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.module.conomy.Conomy;
import de.cubeisland.engine.module.conomy.account.Account;
import de.cubeisland.engine.module.conomy.account.ConomyManager;
import de.cubeisland.engine.module.conomy.account.UserAccount;
import de.cubeisland.engine.module.conomy.account.storage.AccountModel;

import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.conomy.account.storage.TableAccount.TABLE_ACCOUNT;

@Command(name = "money", desc = "Manage your money")
public class MoneyCommand extends CommandContainer
{
    private final Conomy module;
    private final ConomyManager manager;

    public MoneyCommand(Conomy module)
    {
        super(module);
        this.module = module;
        this.manager = module.getManager();
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        return this.getCommand("balance").execute(invocation);
    }

    private UserAccount getUserAccount(User user)
    {
        return this.manager.getUserAccount(user, this.module.getConfig().autocreateUserAcc);
    }

    @Alias(value = {"balance", "moneybalance", "pmoney"})
    @Command(desc = "Shows your balance")
    public void balance(CommandContext context, @Optional User player, @Flag boolean force)
    {
        if (player == null)
        {
            if (!(context.getSource() instanceof User))
            {
                context.sendTranslated(NEGATIVE,
                                       "If you are out of money, better go work than typing silly commands in the console.");
                return;
            }
            player = (User)context.getSource();
        }
        boolean showHidden = force && module.perms().USER_SHOWHIDDEN.isAuthorized(context.getSource());

        UserAccount account = this.getUserAccount(player);
        if (account != null)
        {
            if (!account.isHidden() || showHidden || account.getName().equalsIgnoreCase(player.getName()))
            {
                context.sendTranslated(POSITIVE, "{user}'s Balance: {currency}", player, account.balance());
                return;
            }
        }
        context.sendTranslated(NEGATIVE, "No account found for {user}!", player);
    }

    @Alias(value = {"toplist", "balancetop", "topmoney"})
    @Command(desc = "Shows the players with the highest balance.")
    public void top(CommandContext context, @Optional @Label("[fromRank-]toRank") String range,
                    @Flag(longName = "showhidden", name = "f") boolean force)
    {
        boolean showHidden = force && module.perms().USER_SHOWHIDDEN.isAuthorized(context.getSource());
        int fromRank = 1;
        int toRank = 10;
        if (context.hasPositional(0))
        {
            try
            {
                if (range.contains("-"))
                {
                    fromRank = Integer.parseInt(range.substring(0, range.indexOf("-")));
                    range = range.substring(range.indexOf("-") + 1);
                }
                toRank = Integer.parseInt(range);
            }
            catch (NumberFormatException e)
            {
                context.sendTranslated(NEGATIVE, "Invalid rank!");
                return;
            }
        }
        Collection<AccountModel> models = this.manager.getTopAccounts(true, false, fromRank, toRank, showHidden);
        int i = fromRank;
        if (fromRank == 1)
        {
            context.sendTranslated(POSITIVE, "Top Balance ({amount})", models.size());
        }
        else
        {
            context.sendTranslated(POSITIVE, "Top Balance from {integer} to {integer}", fromRank,
                                   fromRank + models.size() - 1);
        }
        for (AccountModel account : models)
        {
            context.sendMessage("" + i++ + ChatFormat.WHITE + "- " + ChatFormat.DARK_GREEN +
                                    this.module.getCore().getUserManager().getUser(account.getValue(
                                        TABLE_ACCOUNT.USER_ID)).getName() +
                                    ChatFormat.WHITE + ": " + ChatFormat.GOLD + (manager.format(
                (double)account.getValue(TABLE_ACCOUNT.VALUE) / manager.fractionalDigitsFactor())));
        }
    }

    @Alias(value = "pay")
    @Command(alias = "give", desc = "Transfer the given amount to another account.")
    public void pay(CommandContext context, @Label("*|<players>") UserList users, Double amount,
                    @Named("as") User player, @Flag boolean force)
    {
        if (amount < 0)
        {
            context.sendTranslated(NEGATIVE, "What are you trying to do?");
            return;
        }
        boolean asSomeOneElse = false;
        if (player != null)
        {
            if (!module.perms().COMMAND_PAY_ASOTHER.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to pay money as someone else!");
                return;
            }
            asSomeOneElse = true;
        }
        else
        {
            if (!(context.getSource() instanceof User))
            {
                context.sendTranslated(NEGATIVE, "Please specify a player to use their account.");
                return;
            }
            player = (User)context.getSource();
        }
        Account source = this.manager.getUserAccount(player, false);
        if (source == null)
        {
            if (asSomeOneElse)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", player);
            }
            else
            {
                context.sendTranslated(NEGATIVE, "You do not have an account!");
            }
            return;
        }
        List<User> list = new ArrayList<>();
        if (users.isAll())
        {
            list.addAll(module.getCore().getUserManager().getOnlineUsers());
        }
        else
        {
            list.addAll(users.list());
        }
        for (User user : list)
        {
            Account target = this.manager.getUserAccount(user, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "{user} does not have an account!", user);
                continue;
            }
            if (!source.has(amount) && !force && module.perms().COMMAND_MONEY_PAY_FORCE.isAuthorized(context.getSource()))
            {
                if (asSomeOneElse)
                {
                    context.sendTranslated(NEGATIVE, "{user} cannot afford {currency}!", player.getName(), amount);
                }
                else
                {
                    context.sendTranslated(NEGATIVE, "You cannot afford {currency}!", amount);
                }
                return;
            }

            if (this.manager.transaction(source, target, amount, false))
            {
                if (asSomeOneElse)
                {
                    context.sendTranslated(POSITIVE, "{currency} transferred from {user}'s to {user}'s account!", amount, player, user);
                }
                else
                {
                    context.sendTranslated(POSITIVE, "{currency} transferred to {user}'s account!", amount, user);
                }
                user.sendTranslated(POSITIVE, "{user} just paid you {currency}!", player, amount);
            }
            else
            {
                context.sendTranslated(NEGATIVE, "The Transaction was not successful!");
            }
        }
    }
}
