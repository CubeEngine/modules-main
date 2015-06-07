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
package de.cubeisland.engine.module.teleport;

import java.util.UUID;
import com.google.common.base.Optional;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.module.service.task.TaskManager;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;

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

    public TeleportRequestCommands(Teleport module, TaskManager taskManager, UserManager um)
    {
        this.module = module;
        this.taskManager = taskManager;
        this.um = um;
    }

    @Command(desc = "Requests to teleport to a player.")
    @Restricted(value = User.class, msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tpa(User context, User player)
    {
        context.get(TeleportAttachment.class).removeTpRequestCancelTask();
        if (!player.isOnline())
        {
            context.sendTranslated(NEGATIVE, "{user} is not online!", player);
            return;
        }
        player.sendTranslated(POSITIVE, "{sender} wants to teleport to you!", context);
        player.sendTranslated(NEUTRAL, "Use {text:/tpaccept} to accept or {text:/tpdeny} to deny the request!");
        player.get(TeleportAttachment.class).setPendingTpToRequest(context.getUniqueId());
        player.get(TeleportAttachment.class).removePendingTpFromRequest();
        context.sendTranslated(POSITIVE, "Teleport request sent to {user}!", player);
        int waitTime = this.module.getConfig().teleportRequestWait * 20;
        if (waitTime > 0)
        {
            final User sendingUser = context;
            final Optional<UUID> taskID = taskManager.runTaskDelayed(this.module, (Runnable)() -> {
                player.get(TeleportAttachment.class).removeTpRequestCancelTask();
                player.get(TeleportAttachment.class).removePendingTpToRequest();
                sendingUser.sendTranslated(NEGATIVE, "{user} did not accept your teleport request.", player);
                player.sendTranslated(NEGATIVE, "Teleport request of {sender} timed out.", sendingUser);
            }, waitTime); // wait x - seconds
            UUID oldtaskID = player.get(TeleportAttachment.class).getTpRequestCancelTask();
            if (oldtaskID != null)
            {
                taskManager.cancelTask(this.module, oldtaskID);
            }
            player.get(TeleportAttachment.class).setTpRequestCancelTask(taskID.get());
        }
    }

    @Command(desc = "Requests to teleport a player to you.")
    @Restricted(value = User.class, msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tpahere(User context, User player)
    {
        context.get(TeleportAttachment.class).removeTpRequestCancelTask();
        if (!player.isOnline())
        {
            context.sendTranslated(NEGATIVE, "{user} is not online!");
            return;
        }
        player.sendTranslated(POSITIVE, "{sender} wants to teleport you to them!", context);
        player.sendTranslated(NEUTRAL, "Use {text:/tpaccept} to accept or {text:/tpdeny} to deny the request!");
        player.get(TeleportAttachment.class).setPendingTpFromRequest(context.getUniqueId());
        player.get(TeleportAttachment.class).removePendingTpToRequest();
        context.sendTranslated(POSITIVE, "Teleport request send to {user}!", player);
        int waitTime = this.module.getConfig().teleportRequestWait * 20;
        if (waitTime > 0)
        {
            final User sendingUser = context;
            final Optional<UUID> taskID = taskManager.runTaskDelayed(this.module, () -> {
                player.get(TeleportAttachment.class).removeTpRequestCancelTask();
                player.get(TeleportAttachment.class).removePendingTpFromRequest();
                sendingUser.sendTranslated(NEGATIVE, "{user} did not accept your teleport request.", player);
                player.sendTranslated(NEGATIVE, "Teleport request of {sender} timed out.", sendingUser);
            }, waitTime); // wait x - seconds
            UUID oldtaskID = player.get(TeleportAttachment.class).getTpRequestCancelTask();
            if (oldtaskID != null)
            {
                taskManager.cancelTask(this.module, oldtaskID);
            }
            player.get(TeleportAttachment.class).setTpRequestCancelTask(taskID.get());
        }
    }

    @Command(alias = "tpac", desc = "Accepts any pending teleport request.")
    @Restricted(value = User.class, msg = "No one wants to teleport to you!")
    public void tpaccept(User context)
    {
        UUID uuid = context.get(TeleportAttachment.class).getPendingTpToRequest();
        if (uuid == null)
        {
            uuid = context.get(TeleportAttachment.class).getPendingTpFromRequest();
            if (uuid == null)
            {
                context.sendTranslated(NEGATIVE, "You don't have any pending requests!");
                return;
            }
            context.get(TeleportAttachment.class).removePendingTpFromRequest();
            User user = um.getExactUser(uuid);
            if (user == null || !user.isOnline())
            {
                context.sendTranslated(NEGATIVE, "{user} seems to have disappeared.", user);
                return;
            }
            if (!TeleportCommands.teleport(context, user.getLocation(), true, false, true))
            {
                return;
            }
            user.sendTranslated(POSITIVE, "{user} accepted your teleport request!", context);
            context.sendTranslated(POSITIVE, "You accepted a teleport to {user}!", user);
        }
        else
        {
            context.get(TeleportAttachment.class).removePendingTpToRequest();
            User user = um.getExactUser(uuid);
            if (user == null || !user.isOnline())
            {
                context.sendTranslated(NEGATIVE, "{user} seems to have disappeared.", um.getExactUser(uuid).getName());
                return;
            }
            if (!TeleportCommands.teleport(user, context.getLocation(), true, false, true))
            {
                return;
            }
            user.sendTranslated(POSITIVE, "{user} accepted your teleport request!", context);
            context.sendTranslated(POSITIVE, "You accepted a teleport to {user}!", user);
        }
        UUID taskID = context.get(TeleportAttachment.class).getTpRequestCancelTask();
        if (taskID != null)
        {
            context.get(TeleportAttachment.class).removeTpRequestCancelTask();
            taskManager.cancelTask(this.module, taskID);
        }
    }

    @Command(desc = "Denies any pending teleport request.")
    @Restricted(value = User.class, msg = "No one wants to teleport to you!")
    public void tpdeny(User sender)
    {
        UUID tpa =  sender.get(TeleportAttachment.class).getPendingTpToRequest();
        UUID tpahere = sender.get(TeleportAttachment.class).getPendingTpFromRequest();
        if (tpa != null)
        {
            sender.get(TeleportAttachment.class).removePendingTpToRequest();
            User user = um.getExactUser(tpa);
            if (user == null)
            {
                throw new IllegalStateException("Player saved in \"pendingTpToRequest\" was not found!");
            }
            user.sendTranslated(NEGATIVE, "{user} denied your teleport request!", sender);
            sender.sendTranslated(NEGATIVE, "You denied {user}'s teleport request!", user);
        }
        else if (tpahere != null)
        {
            sender.get(TeleportAttachment.class).removePendingTpFromRequest();
            User user = um.getExactUser(tpahere);
            if (user == null)
            {
                throw new IllegalStateException("User saved in \"pendingTpFromRequest\" was not found!");
            }
            user.sendTranslated(NEGATIVE, "{user} denied your request!", sender);
            sender.sendTranslated(NEGATIVE, "You denied {user}'s teleport request", user);
        }
        else
        {
            sender.sendTranslated(NEGATIVE, "You don't have any pending requests!");
            return;
        }
        UUID taskID = sender.get(TeleportAttachment.class).getTpRequestCancelTask();
        if (taskID != null)
        {
            sender.get(TeleportAttachment.class).removeTpRequestCancelTask();
            taskManager.cancelTask(this.module, taskID);
        }
    }
}
