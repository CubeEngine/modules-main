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
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;

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
        if (player.getUniqueId().equals(context.getUniqueId()))
        {
            context.sendTranslated(NEUTRAL, "Teleporting you to yourself? Done.");
            return;
        }
        context.attachOrGet(TeleportAttachment.class, module).removeTpRequestCancelTask();
        if (!player.getPlayer().isPresent())
        {
            context.sendTranslated(NEGATIVE, "{user} is not online!", player);
            return;
        }
        player.sendTranslated(POSITIVE, "{sender} wants to teleport to you!", context);
        player.sendTranslated(NEUTRAL, "Use {text:/tpaccept} to accept or {text:/tpdeny} to deny the request!");
        player.attachOrGet(TeleportAttachment.class, module).setPendingTpToRequest(context.getUniqueId());
        player.attachOrGet(TeleportAttachment.class, module).removePendingTpFromRequest();
        context.sendTranslated(POSITIVE, "Teleport request sent to {user}!", player);
        int waitTime = this.module.getConfig().teleportRequestWait * 20;
        if (waitTime > 0)
        {
            final User sendingUser = context;
            final UUID taskID = taskManager.runTaskDelayed(this.module, (Runnable)() -> {
                player.attachOrGet(TeleportAttachment.class, module).removeTpRequestCancelTask();
                player.attachOrGet(TeleportAttachment.class, module).removePendingTpToRequest();
                sendingUser.sendTranslated(NEGATIVE, "{user} did not accept your teleport request.", player);
                player.sendTranslated(NEGATIVE, "Teleport request of {sender} timed out.", sendingUser);
            }, waitTime); // wait x - seconds
            UUID oldtaskID = player.attachOrGet(TeleportAttachment.class, module).getTpRequestCancelTask();
            if (oldtaskID != null)
            {
                taskManager.cancelTask(this.module, oldtaskID);
            }
            player.attachOrGet(TeleportAttachment.class, module).setTpRequestCancelTask(taskID);
        }
    }

    @Command(desc = "Requests to teleport a player to you.")
    @Restricted(value = User.class, msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tpahere(User context, User player)
    {
        if (player.getUniqueId().equals(context.getUniqueId()))
        {
            context.sendTranslated(NEUTRAL, "Teleporting yourself to you? Done.");
            return;
        }
        context.attachOrGet(TeleportAttachment.class, module).removeTpRequestCancelTask();
        if (!player.getPlayer().isPresent())
        {
            context.sendTranslated(NEGATIVE, "{user} is not online!");
            return;
        }
        player.sendTranslated(POSITIVE, "{sender} wants to teleport you to them!", context);
        player.sendTranslated(NEUTRAL, "Use {text:/tpaccept} to accept or {text:/tpdeny} to deny the request!");
        player.attachOrGet(TeleportAttachment.class, module).setPendingTpFromRequest(context.getUniqueId());
        player.attachOrGet(TeleportAttachment.class, module).removePendingTpToRequest();
        context.sendTranslated(POSITIVE, "Teleport request send to {user}!", player);
        int waitTime = this.module.getConfig().teleportRequestWait * 20;
        if (waitTime > 0)
        {
            final User sendingUser = context;
            final UUID taskID = taskManager.runTaskDelayed(this.module, () -> {
                player.attachOrGet(TeleportAttachment.class, module).removeTpRequestCancelTask();
                player.attachOrGet(TeleportAttachment.class, module).removePendingTpFromRequest();
                sendingUser.sendTranslated(NEGATIVE, "{user} did not accept your teleport request.", player);
                player.sendTranslated(NEGATIVE, "Teleport request of {sender} timed out.", sendingUser);
            }, waitTime); // wait x - seconds
            UUID oldtaskID = player.attachOrGet(TeleportAttachment.class, module).getTpRequestCancelTask();
            if (oldtaskID != null)
            {
                taskManager.cancelTask(this.module, oldtaskID);
            }
            player.get(TeleportAttachment.class).setTpRequestCancelTask(taskID);
        }
    }

    @Command(alias = "tpac", desc = "Accepts any pending teleport request.")
    @Restricted(value = User.class, msg = "No one wants to teleport to you!")
    public void tpaccept(User context)
    {
        UUID uuid = context.attachOrGet(TeleportAttachment.class, module).getPendingTpToRequest();
        if (uuid == null)
        {
            uuid = context.attachOrGet(TeleportAttachment.class, module).getPendingTpFromRequest();
            if (uuid == null)
            {
                context.sendTranslated(NEGATIVE, "You don't have any pending requests!");
                return;
            }
            context.attachOrGet(TeleportAttachment.class, module).removePendingTpFromRequest();
            User user = um.getExactUser(uuid);
            if (user == null || !user.getPlayer().isPresent())
            {
                context.sendTranslated(NEGATIVE, "{user} seems to have disappeared.", user);
                return;
            }
            context.getPlayer().get().setLocation(user.asPlayer().getLocation());
            user.sendTranslated(POSITIVE, "{user} accepted your teleport request!", context);
            context.sendTranslated(POSITIVE, "You accepted a teleport to {user}!", user);
        }
        else
        {
            context.attachOrGet(TeleportAttachment.class, module).removePendingTpToRequest();
            User user = um.getExactUser(uuid);
            if (user == null || !user.getPlayer().isPresent())
            {
                context.sendTranslated(NEGATIVE, "{user} seems to have disappeared.", um.getExactUser(uuid).getName());
                return;
            }
            user.getPlayer().get().setLocation(context.asPlayer().getLocation());
            user.sendTranslated(POSITIVE, "{user} accepted your teleport request!", context);
            context.sendTranslated(POSITIVE, "You accepted a teleport to {user}!", user);
        }
        UUID taskID = context.attachOrGet(TeleportAttachment.class, module).getTpRequestCancelTask();
        if (taskID != null)
        {
            context.attachOrGet(TeleportAttachment.class, module).removeTpRequestCancelTask();
            taskManager.cancelTask(this.module, taskID);
        }
    }

    @Command(desc = "Denies any pending teleport request.")
    @Restricted(value = User.class, msg = "No one wants to teleport to you!")
    public void tpdeny(User sender)
    {
        UUID tpa =  sender.attachOrGet(TeleportAttachment.class, module).getPendingTpToRequest();
        UUID tpahere = sender.attachOrGet(TeleportAttachment.class, module).getPendingTpFromRequest();
        if (tpa != null)
        {
            sender.attachOrGet(TeleportAttachment.class, module).removePendingTpToRequest();
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
            sender.attachOrGet(TeleportAttachment.class, module).removePendingTpFromRequest();
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
        UUID taskID = sender.attachOrGet(TeleportAttachment.class, module).getTpRequestCancelTask();
        if (taskID != null)
        {
            sender.attachOrGet(TeleportAttachment.class, module).removeTpRequestCancelTask();
            taskManager.cancelTask(this.module, taskID);
        }
    }
}
