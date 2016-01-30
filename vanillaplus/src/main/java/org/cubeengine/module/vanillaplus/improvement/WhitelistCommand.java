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
package org.cubeengine.module.vanillaplus.improvement;

import de.cubeisland.engine.butler.CommandInvocation;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.command.sender.ConsoleCommandSender;
import org.cubeengine.service.user.User;
import org.spongepowered.api.Game;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

/**
 * All Whitelist related commands:
 *
 * {@link #add}
 * {@link #remove}
 * {@link #list}
 * {@link #on}
 * {@link #off}
 * {@link #wipe}
 */
@Command(name = "whitelist", desc = "Allows you to manage your whitelist")
public class WhitelistCommand extends ContainerCommand
{
    private final VanillaPlus module;
    private Game game;

    public WhitelistCommand(VanillaPlus module, Game game)
    {
        super(module);
        this.module = module;
        this.game = game;
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        if (invocation.isConsumed())
        {
            return this.getCommand("list").execute(invocation);
        }
        else if (invocation.tokens().size() - invocation.consumed() == 1)
        {
            return this.getCommand("add").execute(invocation);
        }
        return super.execute(invocation);
    }

    @Command(desc = "Adds a player to the whitelist.")
    public void add(CommandSender context, User player)
    {
        if (player.getData(WhitelistData.class).isPresent())
        {
            i18n.sendTranslated(context, NEUTRAL, "{user} is already whitelisted.", player);
            return;
        }
        player.offer(core.getGame().getRegistry().getBuilderOf(WhitelistData.class).get());
        i18n.sendTranslated(context, POSITIVE, "{user} is now whitelisted.", player);
    }

    @Command(alias = "rm", desc = "Removes a player from the whitelist.")
    public void remove(CommandSender context, User player)
    {
        if (!player.getData(WhitelistData.class).isPresent())
        {
            i18n.sendTranslated(context, NEUTRAL, "{user} is not whitelisted.", player);
            return;
        }
        player.getOfflinePlayer().remove(WhitelistData.class);
        i18n.sendTranslated(context, POSITIVE, "{user} is not whitelisted anymore.", player.getName());
    }

    @Command(desc = "Lists all the whitelisted players")
    public void list(CommandSender context)
    {
            /* TODO
        Set<org.spongepowered.api.entity.player.User> whitelist = this.core.getGame().getServer().getWhitelistedPlayers();
        if (!this.core.getGame().getServer().hasWhitelist())
        {
            i18n.sendTranslated(context, NEUTRAL, "The whitelist is currently disabled.");
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "The whitelist is enabled!.");
        }
        context.sendMessage(" ");
        if (whitelist.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "There are currently no whitelisted players!");
        }
        else
        {
            i18n.sendTranslated(context, NEUTRAL, "The following players are whitelisted:");
            for (org.spongepowered.api.entity.player.User player : whitelist)
            {
                context.sendMessage(" - " + player.getName());
            }
        }
        Set<org.spongepowered.api.entity.player.User> operators = this.core.getGame().getServer().getOperators();
        if (!operators.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "The following players are OP and can bypass the whitelist");
            for (org.spongepowered.api.entity.player.User operator : operators)
            {
                context.sendMessage(" - " + operator.getName());
            }
        }
        */
    }

    @Command(desc = "Enables the whitelisting")
    public void on(CommandSender context)
    {
        if (this.core.getGame().getServer().hasWhitelist())
        {
            i18n.sendTranslated(context, NEGATIVE, "The whitelist is already enabled!");
            return;
        }
        this.core.getGame().getServer().setHasWhitelist(true);
        i18n.sendTranslated(context, POSITIVE, "The whitelist is now enabled.");
    }

    @Command(desc = "Disables the whitelisting")
    public void off(CommandSender context)
    {
        if (!this.core.getGame().getServer().hasWhitelist())
        {
            i18n.sendTranslated(context, NEGATIVE, "The whitelist is already disabled!");
            return;
        }
        this.core.getGame().getServer().setHasWhitelist(false);
        i18n.sendTranslated(context, POSITIVE, "The whitelist is now disabled.");
    }

    @Command(desc = "Wipes the whitelist completely")
    @Restricted(value = ConsoleCommandSender.class, msg = "This command is too dangerous for users!")
    public void wipe(CommandSender context)
    {
        // TODO wipe whitelist
        i18n.sendTranslated(context, POSITIVE, "The whitelist was successfully wiped!");
    }
}
