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

public class OpCommands
{

    /* TODO override op commands in separate module
    @Command(desc = "Makes a player an operator")
    @CommandPermission(permDefault = PermDefault.FALSE)
    public void op(CommandSender context, @Optional org.spongepowered.api.entity.player.User player, @Flag boolean force) // TODO gameprofile instead?
    {
        if (player == null)
        {
            // else list operators
            Set<org.spongepowered.api.entity.player.User> ops = this.core.getServer().getOperators();
            if (ops.isEmpty())
            {
                context.sendTranslated(NEUTRAL, "There are currently no operators!");
                return;
            }
            context.sendTranslated(NEUTRAL, "The following users are operators:");
            context.sendMessage(" ");
            for (org.spongepowered.api.entity.player.User opPlayer : ops)
            {
                context.sendTranslated(POSITIVE, " - {user} (Last seen: {date:notime})", opPlayer, opPlayer.getData(JoinData.class).get().getLastPlayed());
            }
            return;
        }
        if (!(player.getData(JoinData.class).isPresent() || player.isOnline()) && !force)
        {
            context.sendTranslated(NEGATIVE, "{user} has never played on this server!", player);
            context.sendTranslated(NEGATIVE, "If you still want to op him, use the -force flag.");
            return;
        }
        if (player.isOp())
        {
            context.sendTranslated(NEUTRAL, "{user} is already an operator.", player);
            return;
        }
        player.setOp(true);
        if (player.isOnline())
        {
            um.getExactUser(player.getUniqueId()).sendTranslated(POSITIVE, "You were opped by {sender}", context);
        }
        context.sendTranslated(POSITIVE, "{user} is now an operator!", player);

        for (User onlineUser : um.getOnlineUsers())
        {
            if (onlineUser.getUniqueId().equals(player.getUniqueId()) ||
                onlineUser.getUniqueId().equals(context.getUniqueId()) ||
                !core.perms().COMMAND_OP_NOTIFY.isAuthorized(onlineUser))
            {
                continue;
            }
            onlineUser.sendTranslated(NEUTRAL, "User {user} has been opped by {sender}!", player, context);
        }
        this.core.getLog().info("Player {} has been opped by {}", player.getName(), context.getName());
    }

    @Command(desc = "Revokes the operator status of a player")
    @CommandPermission(permDefault = PermDefault.FALSE)
    public void deop(CommandContext context, org.spongepowered.api.entity.player.User player)
    {
        if (!context.getSource().getUniqueId().equals(player.getUniqueId()))
        {
            context.ensurePermission(core.perms().COMMAND_DEOP_OTHER);
        }
        if (!player.isOp())
        {
            context.sendTranslated(NEGATIVE, "The player you tried to deop is not an operator.");
            return;
        }
        player.setOp(false);
        if (player.isOnline())
        {
            um.getExactUser(player.getUniqueId()).sendTranslated(POSITIVE, "You were deopped by {user}.",
                                                                 context.getSource());
        }
        context.sendTranslated(POSITIVE, "{user} is no longer an operator!", player);

        for (User onlineUser : um.getOnlineUsers())
        {
            if (onlineUser.getUniqueId().equals(player.getUniqueId()) ||
                onlineUser.getUniqueId().equals(context.getSource().getUniqueId()) ||
                !core.perms().COMMAND_DEOP_NOTIFY.isAuthorized(onlineUser))
            {
                continue;
            }
            onlineUser.sendTranslated(POSITIVE, "User {user} has been deopped by {sender}!", player,
                                      context.getSource());
        }

        this.core.getLog().info("Player {} has been deopped by {}", player.getName(),
                                context.getSource().getName());
    }
    */


}
