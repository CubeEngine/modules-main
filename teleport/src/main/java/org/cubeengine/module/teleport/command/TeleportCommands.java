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

import java.util.ArrayList;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.teleport.permission.TeleportPerm;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType.ACTION_BAR;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.cubeengine.libcube.util.ChatFormat.DARK_GREEN;
import static org.cubeengine.libcube.util.ChatFormat.WHITE;

/**
 * Contains commands to teleport to players/worlds/position.
 * /tp
 * /tpall
 * /tphere
 * /tphereall
 * /tppos
 */
@Singleton
public class TeleportCommands
{
    private Broadcaster bc;
    private I18n i18n;
    private TeleportPerm perms;

    @Inject
    public TeleportCommands(Broadcaster bc, I18n i18n, TeleportPerm perms)
    {
        this.bc = bc;
        this.i18n = i18n;
        this.perms = perms;
    }

    @Command(desc = "Teleport directly to a player.")
    public void tp(CommandCause context, ServerPlayer player, @Option ServerPlayer target, @Flag boolean force)
    {
        if (target == null)
        {
            target = player;
            if (!(context.getAudience() instanceof ServerPlayer))
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "You have to provide both players");
                return;
            }
            player = (ServerPlayer)context.getAudience();
        }
        force = force && context.hasPermission(perms.COMMAND_TP_FORCE.getId());
        if (!context.getAudience().equals(player) && !context.hasPermission(perms.COMMAND_TP_OTHER.getId())) // teleport other persons
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "You are not allowed to teleport other people!");
            return;
        }

        if (!context.getAudience().equals(player))
        {
            if (!force && player.hasPermission(perms.TELEPORT_PREVENT_TP.getId())) // teleport the user
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "You are not allowed to teleport {user}!", player);
                return;
            }
        } // else equals tp -> no need to check tp perm
        if (!context.getAudience().equals(target))
        {
            if (!force && target.hasPermission(perms.TELEPORT_PREVENT_TPTO.getId())) // teleport to the target
            {
                if (context.hasPermission(perms.COMMAND_TP_FORCE.getId()))
                {
                    i18n.send(ACTION_BAR, context, POSITIVE, "Use the {text:-force (-f)} flag to teleport to this player."); //Show force flag if has permission
                }
                i18n.send(ACTION_BAR, context, NEGATIVE, "You are not allowed to teleport to {user}!", target);
                return;
            }
        } // else equals tphere -> no need to check tpto perm

        if (player.equals(target))
        {
            if (context.getAudience().equals(player))
            {
                i18n.send(ACTION_BAR, context, NEUTRAL, "You found yourself!");
                return;
            }
            i18n.send(ACTION_BAR, context, NEUTRAL, "You just teleported {user} to {user}... Not very useful right?", player, player);
            return;
        }
        player.setLocation(target.getServerLocation());
        i18n.send(ACTION_BAR, context, POSITIVE, "You teleported to {user}!", target);
    }

    @Command(desc = "Teleports everyone directly to a player.")
    public void tpall(CommandCause context, ServerPlayer player, @Flag boolean force)
    {
        force = force && context.hasPermission(perms.COMMAND_TPALL_FORCE.getId());
        if (!force && player.hasPermission(perms.TELEPORT_PREVENT_TPTO.getId()))
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "You are not allowed to teleport to {user}!", player);
            return;
        }
        ArrayList<String> noTp = new ArrayList<>();
        ServerLocation target = player.getServerLocation();
        for (ServerPlayer p : Sponge.getServer().getOnlinePlayers())
        {
            if (!force && p.hasPermission(perms.TELEPORT_PREVENT_TP.getId()))
            {
                noTp.add(p.getName());
                continue;
            }
            p.setLocation(target);
        }
        bc.broadcastTranslated(POSITIVE, "Teleporting everyone to {user}", player);
        if (!noTp.isEmpty())
        {
            i18n.send(context, NEUTRAL, "The following players were not teleported: \n{user#list}", StringUtils.implode(WHITE + "," + DARK_GREEN, noTp));
        }
    }

    @Command(desc = "Teleport a player directly to you.")
    @Restricted(msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tphere(ServerPlayer context, ServerPlayer player, @Flag boolean force)
    {
        force = force && context.hasPermission(perms.COMMAND_TPHERE_FORCE.getId());
        if ( context.equals(player))
        {
            i18n.send(ACTION_BAR, context, NEUTRAL, "You found yourself!");
            return;
        }
        if (!force && player.hasPermission(perms.TELEPORT_PREVENT_TP.getId()))
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "You are not allowed to teleport {user}!", player);
            return;
        }

        player.setLocation(context.getServerLocation());
        i18n.send(ACTION_BAR, context, POSITIVE, "You teleported {user} to you!", player);
        i18n.send(ACTION_BAR, player, POSITIVE, "You were teleported to {sender}", context);
    }

    @Command(desc = "Teleport every player directly to you.")
    @Restricted(msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tphereall(ServerPlayer context, @Flag boolean force)
    {
        force = force && context.hasPermission(perms.COMMAND_TPHEREALL_FORCE.getId());
        ArrayList<String> noTp = new ArrayList<>();
        ServerLocation target = context.getServerLocation();
        for (ServerPlayer p : Sponge.getServer().getOnlinePlayers())
        {
            if (!force && p.hasPermission(perms.TELEPORT_PREVENT_TP.getId()))
            {
                noTp.add(p.getName());
                continue;
            }
            p.setLocation(target);
        }
        i18n.send(ACTION_BAR, context, POSITIVE, "You teleported everyone to you!");
        bc.broadcastTranslated(POSITIVE, "Teleporting everyone to {sender}", context);
        if (!noTp.isEmpty())
        {
            i18n.send(context, NEUTRAL, "The following players were not teleported: \n{user#list}", StringUtils.implode(
                WHITE + "," + DARK_GREEN, noTp));
        }
    }

    @Command(desc = "Direct teleport to a coordinate.")
    public void tppos(CommandCause context, Integer x, @Option Integer y, Integer z, // TODO optional y coord
                      @Default @Named({"world", "w"}) ServerWorld world, // TODO named param not working
                      @Default @Named({"player", "p"}) ServerPlayer player,
                      @Flag boolean unsafe)
    {
        ServerLocation loc = world.getLocation(x,y,z).add(0.5, 0, 0.5);
        unsafe = unsafe && context.hasPermission(perms.COMMAND_TPPOS_UNSAFE.getId());
        if (unsafe)
        {
            player.setLocation(loc);
        }
        else
        {
            final Optional<ServerLocation> safe = Sponge.getServer().getTeleportHelper().getSafeLocation(loc);
            if (!safe.isPresent())
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "Unsafe Target!");
                return;
            }
            player.setLocation(safe.get());
        }
        i18n.send(context, POSITIVE, "Teleported to {vector:x\\=:y\\=:z\\=} in {world}!", new Vector3i(x, y, z), world);
    }
}

