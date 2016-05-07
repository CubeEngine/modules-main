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

import java.util.Collection;
import java.util.stream.Collectors;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.whitelist.WhitelistService;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

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
    private I18n i18n;

    public WhitelistCommand(CommandManager base, I18n i18n)
    {
        super(base, VanillaPlus.class);
        this.i18n = i18n;
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


    private WhitelistService getWhitelistService()
    {
        return Sponge.getServiceManager().provideUnchecked(WhitelistService.class);
    }

    @Command(desc = "Adds a player to the whitelist.")
    public void add(CommandSource context, Player player)
    {
        WhitelistService service = getWhitelistService();
        if (service.addProfile(player.getProfile()))
        {
            i18n.sendTranslated(context, NEUTRAL, "{user} is already whitelisted.", player);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "{user} is now whitelisted.", player);
    }

    @Command(alias = "rm", desc = "Removes a player from the whitelist.")
    public void remove(CommandSource context, Player player)
    {
        WhitelistService service = getWhitelistService();
        if (service.removeProfile(player.getProfile()))
        {
            i18n.sendTranslated(context, NEUTRAL, "{user} is not whitelisted.", player);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "{user} is not whitelisted anymore.", player.getName());
    }

    @Command(desc = "Lists all the whitelisted players")
    public void list(CommandSource context)
    {
        WhitelistService service = getWhitelistService();
        if (!Sponge.getServer().hasWhitelist())
        {
            i18n.sendTranslated(context, NEUTRAL, "The whitelist is currently disabled.");
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "The whitelist is enabled!.");
        }
        context.sendMessage(Text.EMPTY);
        Collection<GameProfile> list = service.getWhitelistedProfiles();
        if (list.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "There are currently no whitelisted players!");
        }
        else
        {

            Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder()
                .title(i18n.getTranslation(context, NEUTRAL, "The following players are whitelisted"))
                .contents(list.stream().map(p -> Text.of(" - ", p.getName())).collect(Collectors.toList()))
                .sendTo(context);
        }

        /* TODO list ops too
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
    public void on(CommandSource context)
    {
        if (Sponge.getServer().hasWhitelist())
        {
            i18n.sendTranslated(context, NEGATIVE, "The whitelist is already enabled!");
            return;
        }
        Sponge.getServer().setHasWhitelist(true);
        i18n.sendTranslated(context, POSITIVE, "The whitelist is now enabled.");
    }

    @Command(desc = "Disables the whitelisting")
    public void off(CommandSource context)
    {
        if (!Sponge.getServer().hasWhitelist())
        {
            i18n.sendTranslated(context, NEGATIVE, "The whitelist is already disabled!");
            return;
        }
        Sponge.getServer().setHasWhitelist(false);
        i18n.sendTranslated(context, POSITIVE, "The whitelist is now disabled.");
    }

    @Command(desc = "Wipes the whitelist completely")
    @Restricted(value = ConsoleSource.class, msg = "This command is too dangerous for users!")
    public void wipe(CommandSource context)
    {
        WhitelistService service = getWhitelistService();
        service.getWhitelistedProfiles().forEach(service::removeProfile);
        i18n.sendTranslated(context, POSITIVE, "The whitelist was successfully wiped!");
    }
}
