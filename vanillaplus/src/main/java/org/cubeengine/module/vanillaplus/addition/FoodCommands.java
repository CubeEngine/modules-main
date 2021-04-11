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
package org.cubeengine.module.vanillaplus.addition;

import java.util.Collection;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
public class FoodCommands extends PermissionContainer
{
    private I18n i18n;
    private Broadcaster bc;

    public final Permission COMMAND_FEED_OTHER = register("command.feed.other", "Allows feeding other players");
    public final Permission COMMAND_STARVE_OTHER = register("command.starve.other", "Allows starving other players");

    @Inject
    public FoodCommands(PermissionManager pm, I18n i18n, Broadcaster bc)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
        this.bc = bc;
    }

    @Command(desc = "Refills your hunger bar")
    public void feed(CommandCause context, @Option Collection<ServerPlayer> players)
    {
        if (players == null)
        {
            if (!(context.subject() instanceof ServerPlayer))
            {
                i18n.send(context, NEGATIVE, "Don't feed the troll!");
                return;
            }
            final ServerPlayer sender = (ServerPlayer)context.subject();
            sender.offer(Keys.FOOD_LEVEL, 20);
            sender.offer(Keys.SATURATION, 20.0);
            sender.offer(Keys.EXHAUSTION, 0.0);
            i18n.send(context, POSITIVE, "You are now fed!");
            return;
        }
        if (!context.hasPermission(COMMAND_FEED_OTHER.getId()))
        {
            i18n.send(context, NEGATIVE, "You are not allowed to feed other players!");
            return;
        }

        boolean all = players.containsAll(Sponge.server().onlinePlayers());
        if (all)
        {
            if (players.isEmpty())
            {
                i18n.send(context, NEGATIVE, "There are no players online at the moment!");
                return;
            }
            i18n.send(context, POSITIVE, "You made everyone fat!");
            bc.broadcastStatus(Style.style(NamedTextColor.GREEN), "shared food with everyone.", context);
        }
        else
        {
            i18n.send(context, POSITIVE, "Fed {amount} players!", players.size());
        }

        for (ServerPlayer player : players)
        {
            if (!all)
            {
                i18n.send(player, POSITIVE, "You got fed by {user}!", context);
            }
            player.offer(Keys.FOOD_LEVEL, 20);
            player.offer(Keys.SATURATION, 20.0);
            player.offer(Keys.EXHAUSTION, 0.0);
        }
    }

    @Command(desc = "Empties the hunger bar")
    public void starve(CommandCause context, @Option Collection<ServerPlayer> players)
    {
        if (players == null)
        {
            if (!(context.subject() instanceof ServerPlayer))
            {
                context.sendMessage(Identity.nil(), Component.text("\n\n\n\n\n\n\n\n\n\n\n\n\n"));
                i18n.send(context, NEGATIVE, "I'll give you only one line to eat!");
                return;
            }
            final ServerPlayer sender = (ServerPlayer)context.subject();
            sender.offer(Keys.FOOD_LEVEL, 0);
            sender.offer(Keys.SATURATION, 0.0);
            sender.offer(Keys.EXHAUSTION, 4.0);
            i18n.send(context, NEGATIVE, "You are now starving!");
            return;
        }
        if (!context.hasPermission(COMMAND_STARVE_OTHER.getId()))
        {
            i18n.send(context, NEGATIVE, "You are not allowed to let other players starve!");
            return;
        }

        boolean all = players.containsAll(Sponge.server().onlinePlayers());
        if (all)
        {
            if (players.isEmpty())
            {
                i18n.send(context, NEGATIVE, "There are no players online at the moment!");
                return;
            }
            i18n.send(context, NEUTRAL, "You let everyone starve to death!");
            bc.broadcastStatus(i18n.translate(context, "took away all food.").color(NamedTextColor.YELLOW), context);
        }
        else
        {
            i18n.send(context, POSITIVE, "Starved {amount} players!", players.size());
        }
        for (ServerPlayer player : players)
        {
            if (!all)
            {
                i18n.send(player, NEUTRAL, "You are suddenly starving!");
            }
            player.offer(Keys.FOOD_LEVEL, 0);
            player.offer(Keys.SATURATION, 0.0);
            player.offer(Keys.EXHAUSTION, 4.0);
        }
    }
}
