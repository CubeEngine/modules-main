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
package org.cubeengine.module.teleport;

import java.util.UUID;
import com.google.common.base.Optional;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.MultilingualPlayer;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;

import static org.cubeengine.service.i18n.formatter.MessageType.*;

/**
 * Contains Teleport-Request commands.
 * /tpa
 * /tpahere
 * /tpaccept
 * /tpdeny
 */
public class TeleportRequestCommands
{
    private final Teleport module;
    private TaskManager taskManager;
    private UserManager um;
    private TeleportListener tl;
    private Game game;
    private I18n i18n;

    public TeleportRequestCommands(Teleport module, TaskManager taskManager, UserManager um, TeleportListener tl, Game game, I18n i18n)
    {
        this.module = module;
        this.taskManager = taskManager;
        this.um = um;
        this.tl = tl;
        this.game = game;
        this.i18n = i18n;
    }

    @Command(desc = "Requests to teleport to a player.")
    @Restricted(value = MultilingualPlayer.class, msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tpa(MultilingualPlayer context, MultilingualPlayer player)
    {
        if (player.getUniqueId().equals(context.getUniqueId()))
        {
            context.sendTranslated(NEUTRAL, "Teleporting you to yourself? Done.");
            return;
        }
        tl.removeRequestTask(context.original());
        player.sendTranslated(POSITIVE, "{sender} wants to teleport to you!", context);
        player.sendTranslated(NEUTRAL, "Use {text:/tpaccept} to accept or {text:/tpdeny} to deny the request!");
        tl.setToRequest(player.original(), context.original());
        tl.removeFromRequest(player.original());
        context.sendTranslated(POSITIVE, "Teleport request sent to {user}!", player);
        int waitTime = this.module.getConfig().teleportRequestWait * 20;
        if (waitTime > 0)
        {
            final MultilingualPlayer sendingUser = context;
            final UUID taskID = taskManager.runTaskDelayed(this.module, () -> {

                tl.removeRequestTask(player.original());
                tl.removeToRequest(player.original());
                sendingUser.sendTranslated(NEGATIVE, "{user} did not accept your teleport request.", player);
                player.sendTranslated(NEGATIVE, "Teleport request of {sender} timed out.", sendingUser);
            }, waitTime); // wait x - seconds
            UUID oldtaskID = tl.getRequestTask(player.original());
            if (oldtaskID != null)
            {
                taskManager.cancelTask(this.module, oldtaskID);
            }
            tl.setRequestTask(player.original(), taskID);
        }
    }

    @Command(desc = "Requests to teleport a player to you.")
    @Restricted(value = MultilingualPlayer.class, msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tpahere(MultilingualPlayer context, MultilingualPlayer player)
    {
        if (player.getUniqueId().equals(context.getUniqueId()))
        {
            context.sendTranslated(NEUTRAL, "Teleporting yourself to you? Done.");
            return;
        }
        tl.removeRequestTask(player.original());
        player.sendTranslated(POSITIVE, "{sender} wants to teleport you to them!", context);
        player.sendTranslated(NEUTRAL, "Use {text:/tpaccept} to accept or {text:/tpdeny} to deny the request!");
        tl.setFromRequest(player.original(), context.original());
        tl.removeToRequest(player.original());
        context.sendTranslated(POSITIVE, "Teleport request send to {user}!", player);
        int waitTime = this.module.getConfig().teleportRequestWait * 20;
        if (waitTime > 0)
        {
            final MultilingualPlayer sendingUser = context;
            final UUID taskID = taskManager.runTaskDelayed(this.module, () -> {
                tl.removeRequestTask(player.original());
                tl.removeFromRequest(player.original());
                sendingUser.sendTranslated(NEGATIVE, "{user} did not accept your teleport request.", player);
                player.sendTranslated(NEGATIVE, "Teleport request of {sender} timed out.", sendingUser);
            }, waitTime); // wait x - seconds
            UUID oldtaskID = tl.getRequestTask(player.original());
            if (oldtaskID != null)
            {
                taskManager.cancelTask(this.module, oldtaskID);
            }
            tl.setRequestTask(player.original(), taskID);
        }
    }

    @Command(alias = "tpac", desc = "Accepts any pending teleport request.")
    @Restricted(value = MultilingualPlayer.class, msg = "No one wants to teleport to you!")
    public void tpaccept(MultilingualPlayer context)
    {

        UUID uuid = tl.getToRequest(context.original());
        if (uuid == null)
        {
            uuid = tl.getFromRequest(context.original());
            if (uuid == null)
            {
                context.sendTranslated(NEGATIVE, "You don't have any pending requests!");
                return;
            }
            tl.removeFromRequest(context.original());
            Optional<Player> player = game.getServer().getPlayer(uuid);
            if (!player.isPresent())
            {
                context.sendTranslated(NEGATIVE, "That player seems to have disappeared.");
                return;
            }
            context.original().setLocation(player.get().getLocation());
            i18n.sendTranslated(player.get(), POSITIVE, "{user} accepted your teleport request!", context);
            context.sendTranslated(POSITIVE, "You accepted a teleport to {user}!", player.get());
        }
        else
        {
            tl.removeToRequest(context.original());
            Optional<Player> player = game.getServer().getPlayer(uuid);
            if (!player.isPresent())
            {
                context.sendTranslated(NEGATIVE, "That player seems to have disappeared.");
                return;
            }
            player.get().setLocation(context.original().getLocation());
            i18n.sendTranslated(player.get(), POSITIVE, "{user} accepted your teleport request!", context);
            context.sendTranslated(POSITIVE, "You accepted a teleport to {user}!", player.get());
        }
        UUID taskID = tl.getRequestTask(context.original());
        if (taskID != null)
        {
            tl.getRequestTask(context.original());
            taskManager.cancelTask(this.module, taskID);
        }
    }

    @Command(desc = "Denies any pending teleport request.")
    @Restricted(value = MultilingualPlayer.class, msg = "No one wants to teleport to you!")
    public void tpdeny(MultilingualPlayer sender)
    {

        UUID tpa = tl.getToRequest(sender.original());
        UUID tpahere = tl.getFromRequest(sender.original());
        if (tpa != null)
        {
            tl.removeToRequest(sender.original());

            Optional<Player> player = game.getServer().getPlayer(tpa);
            if (!player.isPresent())
            {
                throw new IllegalStateException("Player saved in \"pendingTpToRequest\" was not found!");
            }
            i18n.sendTranslated(player.get(), NEGATIVE, "{user} denied your teleport request!", sender);
            sender.sendTranslated(NEGATIVE, "You denied {user}'s teleport request!", player.get());
        }
        else if (tpahere != null)
        {
            tl.removeFromRequest(sender.original());
            Optional<Player> player = game.getServer().getPlayer(tpahere);
            if (!player.isPresent())
            {
                throw new IllegalStateException("User saved in \"pendingTpFromRequest\" was not found!");
            }
            i18n.sendTranslated(player.get(), NEGATIVE, "{user} denied your request!", sender);
            sender.sendTranslated(NEGATIVE, "You denied {user}'s teleport request", player.get());
        }
        else
        {
            sender.sendTranslated(NEGATIVE, "You don't have any pending requests!");
            return;
        }
        UUID taskID = tl.getRequestTask(sender.original());
        if (taskID != null)
        {
            tl.removeRequestTask(sender.original());
            taskManager.cancelTask(this.module, taskID);
        }
    }
}
