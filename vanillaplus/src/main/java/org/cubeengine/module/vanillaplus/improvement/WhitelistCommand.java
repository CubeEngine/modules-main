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
package org.cubeengine.module.vanillaplus.improvement;

import java.util.Collection;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Delegate;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.whitelist.WhitelistService;

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
@Delegate("list")
public class WhitelistCommand extends DispatcherCommand
{
    private I18n i18n;

    @Inject
    public WhitelistCommand(I18n i18n)
    {
        this.i18n = i18n;
    }

    private WhitelistService getWhitelistService()
    {
        return Sponge.server().serviceProvider().whitelistService();
    }

    @Command(desc = "Adds a player to the whitelist.")
    public void add(CommandCause context, User player) // TODO allow players that never played on the server
    {
        WhitelistService service = getWhitelistService();
        if (service.addProfile(player.profile()).join())
        {
            i18n.send(context, NEUTRAL, "{user} is already whitelisted.", player);
            return;
        }
        i18n.send(context, POSITIVE, "{user} is now whitelisted.", player);
    }

    @Command(alias = "rm", desc = "Removes a player from the whitelist.")
    public void remove(CommandCause context, User player)
    {
        WhitelistService service = getWhitelistService();
        if (service.removeProfile(player.profile()).join())
        {
            i18n.send(context, POSITIVE, "{user} is not whitelisted anymore.", player.name());
            return;
        }
        i18n.send(context, NEUTRAL, "{user} is not whitelisted.", player);
    }

    @Command(desc = "Lists all the whitelisted players")
    public void list(CommandCause context)
    {
        WhitelistService service = getWhitelistService();
        if (!Sponge.server().isWhitelistEnabled())
        {
            i18n.send(context, NEUTRAL, "The whitelist is currently disabled.");
        }
        else
        {
            i18n.send(context, POSITIVE, "The whitelist is enabled!.");
        }
        context.sendMessage(Identity.nil(), Component.empty());
        Collection<GameProfile> list = service.whitelistedProfiles().join();
        if (list.isEmpty())
        {
            i18n.send(context, NEUTRAL, "There are currently no whitelisted players!");
        }
        else
        {
            Sponge.serviceProvider().paginationService().builder()
                  .title(i18n.translate(context, NEUTRAL, "The following players are whitelisted"))
                  .contents(list.stream().map(p -> Component.text(" - ").append(Component.text(p.name().orElse("??"), NamedTextColor.DARK_GREEN)))
                                .collect(Collectors.toList()))
                .sendTo(context.audience());
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
    public void on(CommandCause context)
    {
        if (Sponge.server().isWhitelistEnabled())
        {
            i18n.send(context, NEGATIVE, "The whitelist is already enabled!");
            return;
        }
        Sponge.server().setHasWhitelist(true);
        i18n.send(context, POSITIVE, "The whitelist is now enabled.");
    }

    @Command(desc = "Disables the whitelisting")
    public void off(CommandCause context)
    {
        if (!Sponge.server().isWhitelistEnabled())
        {
            i18n.send(context, NEGATIVE, "The whitelist is already disabled!");
            return;
        }
        Sponge.server().setHasWhitelist(false);
        i18n.send(context, POSITIVE, "The whitelist is now disabled.");
    }


    @Command(desc = "Wipes the whitelist completely")
    @Restricted(value = SystemSubject.class) // TODO , msg = "This command is too dangerous for users!"
    public void wipe(CommandCause context)
    {
        WhitelistService service = getWhitelistService();
        service.whitelistedProfiles().join().forEach(service::removeProfile);
        i18n.send(context, POSITIVE, "The whitelist was successfully wiped!");
    }
}
