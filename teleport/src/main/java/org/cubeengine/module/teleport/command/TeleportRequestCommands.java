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
package org.cubeengine.module.teleport.command;

import java.util.Optional;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.teleport.Teleport;
import org.cubeengine.module.teleport.TeleportListener;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType.ACTION_BAR;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

/**
 * Contains Teleport-Request commands.
 * /tpa
 * /tpahere
 * /tpaccept
 * /tpdeny
 */
@Singleton
public class TeleportRequestCommands
{
    private final Teleport module;
    private TaskManager taskManager;
    private TeleportListener tl;
    private I18n i18n;

    @Inject
    public TeleportRequestCommands(Teleport module, TaskManager taskManager, TeleportListener tl, I18n i18n)
    {
        this.module = module;
        this.taskManager = taskManager;
        this.tl = tl;
        this.i18n = i18n;
    }

    @Command(desc = "Requests to teleport to a player.")
    @Restricted(msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tpa(ServerPlayer context, ServerPlayer player)
    {
        if (player.getUniqueId().equals(context.getUniqueId()))
        {
            i18n.send(ACTION_BAR, context, NEUTRAL, "Teleporting you to yourself? Done.");
            return;
        }
        tl.removeRequestTask(context);
        i18n.send(player, POSITIVE, "{sender} wants to teleport to you!", context);
        i18n.send(player, NEUTRAL, "Use {text:/tpaccept} to accept or {text:/tpdeny} to deny the request!");
        tl.setToRequest(player, context);
        tl.removeFromRequest(player);
        i18n.send(context, POSITIVE, "Teleport request sent to {user}!", player);
        int waitTime = this.module.getConfig().teleportRequestWait * 20;
        if (waitTime > 0)
        {
            final Player sendingUser = context;
            final UUID taskID = taskManager.runTaskDelayed(Teleport.class, () -> {

                tl.removeRequestTask(player);
                tl.removeToRequest(player);
                i18n.send(sendingUser, NEGATIVE, "{user} did not accept your teleport request.", player);
                i18n.send(player, NEGATIVE, "Teleport request of {sender} timed out.", sendingUser);
            }, waitTime); // wait x - seconds
            UUID oldtaskID = tl.getRequestTask(player);
            if (oldtaskID != null)
            {
                taskManager.cancelTask(Teleport.class, oldtaskID);
            }
            tl.setRequestTask(player    , taskID);
        }
    }

    @Command(desc = "Requests to teleport a player to you.")
    @Restricted(msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tpahere(ServerPlayer context, ServerPlayer player)
    {
        if (player.getUniqueId().equals(context.getUniqueId()))
        {
            i18n.send(ACTION_BAR, context, NEUTRAL, "Teleporting yourself to you? Done.");
            return;
        }
        tl.removeRequestTask(player);
        i18n.send(player, POSITIVE, "{sender} wants to teleport you to them!", context);
        i18n.send(player, NEUTRAL, "Use {text:/tpaccept} to accept or {text:/tpdeny} to deny the request!");
        tl.setFromRequest(player, context);
        tl.removeToRequest(player);
        i18n.send(context, POSITIVE, "Teleport request send to {user}!", player);
        int waitTime = this.module.getConfig().teleportRequestWait * 20;
        if (waitTime > 0)
        {
            final Player sendingUser = context;
            final UUID taskID = taskManager.runTaskDelayed(Teleport.class, () -> {
                tl.removeRequestTask(player);
                tl.removeFromRequest(player);
                i18n.send(sendingUser, NEGATIVE, "{user} did not accept your teleport request.", player);
                i18n.send(player, NEGATIVE, "Teleport request of {sender} timed out.", sendingUser);
            }, waitTime); // wait x - seconds
            UUID oldtaskID = tl.getRequestTask(player);
            if (oldtaskID != null)
            {
                taskManager.cancelTask(Teleport.class, oldtaskID);
            }
            tl.setRequestTask(player, taskID);
        }
    }

    @Command(alias = "tpac", desc = "Accepts any pending teleport request.")
    @Restricted(msg = "No one wants to teleport to you!")
    public void tpaccept(ServerPlayer context)
    {

        UUID uuid = tl.getToRequest(context);
        if (uuid == null)
        {
            uuid = tl.getFromRequest(context);
            if (uuid == null)
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "You don't have any pending requests!");
                return;
            }
            tl.removeFromRequest(context);
            Optional<ServerPlayer> player = Sponge.getServer().getPlayer(uuid);
            if (!player.isPresent())
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "That player seems to have disappeared.");
                return;
            }
            context.setLocation(player.get().getServerLocation());
            i18n.send(player.get(), POSITIVE, "{user} accepted your teleport request!", context);
            i18n.send(context, POSITIVE, "You accepted a teleport to {user}!", player.get());
        }
        else
        {
            tl.removeToRequest(context);
            Optional<ServerPlayer> player = Sponge.getServer().getPlayer(uuid);
            if (!player.isPresent())
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "That player seems to have disappeared.");
                return;
            }
            player.get().setLocation(context.getServerLocation());
            i18n.send(player.get(), POSITIVE, "{user} accepted your teleport request!", context);
            i18n.send(context, POSITIVE, "You accepted a teleport to {user}!", player.get());
        }
        UUID taskID = tl.getRequestTask(context);
        if (taskID != null)
        {
            tl.getRequestTask(context);
            taskManager.cancelTask(Teleport.class, taskID);
        }
    }

    @Command(desc = "Denies any pending teleport request.")
    @Restricted(msg = "No one wants to teleport to you!")
    public void tpdeny(ServerPlayer sender)
    {

        UUID tpa = tl.getToRequest(sender);
        UUID tpahere = tl.getFromRequest(sender);
        if (tpa != null)
        {
            tl.removeToRequest(sender);

            Optional<ServerPlayer> player = Sponge.getServer().getPlayer(tpa);
            if (!player.isPresent())
            {
                throw new IllegalStateException("Player saved in \"pendingTpToRequest\" was not found!");
            }
            i18n.send(player.get(), NEGATIVE, "{user} denied your teleport request!", sender);
            i18n.send(sender, NEGATIVE, "You denied {user}'s teleport request!", player.get());
        }
        else if (tpahere != null)
        {
            tl.removeFromRequest(sender);
            Optional<ServerPlayer> player = Sponge.getServer().getPlayer(tpahere);
            if (!player.isPresent())
            {
                throw new IllegalStateException("User saved in \"pendingTpFromRequest\" was not found!");
            }
            i18n.send(player.get(), NEGATIVE, "{user} denied your request!", sender);
            i18n.send(sender, NEGATIVE, "You denied {user}'s teleport request", player.get());
        }
        else
        {
            i18n.send(ACTION_BAR, sender, NEGATIVE, "You don't have any pending requests!");
            return;
        }
        UUID taskID = tl.getRequestTask(sender);
        if (taskID != null)
        {
            tl.removeRequestTask(sender);
            taskManager.cancelTask(Teleport.class, taskID);
        }
    }
}
