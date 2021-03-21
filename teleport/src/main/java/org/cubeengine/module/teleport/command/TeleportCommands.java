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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.FirstValueParameter;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.teleport.permission.TeleportPerm;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3d;
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

    public static class DestinationParameter implements FirstValueParameter
    {
        public ServerPlayer player;
        public Vector3d location;

        public ServerLocation position(Entity target) {
            if (player == null)
            {
                return target.serverLocation().world().location(location);
            }
            return player.serverLocation();
        }
    }

    @Alias("teleport")
    @Command(desc = "Teleport directly to a player.")
    public void tp(CommandCause context, @Option @ParameterPermission Collection<Entity> targets, DestinationParameter destination, @Flag boolean force)
    {
        force = force && context.hasPermission(perms.COMMAND_TP_FORCE.getId());

        if (targets != null && !context.hasPermission(perms.COMMAND_TP_TARGETS.getId())) // teleport other persons
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "You are not allowed to teleport other people!");
            return;
        }

        List<Entity> actualTargets;
        if (targets == null)
        {
            if (context.subject() instanceof ServerPlayer)
            {
                actualTargets = Arrays.asList(((ServerPlayer)context.subject()));
            }
            else
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "No target entities provided!");
                return;
            }
        }
        else
        {
            actualTargets = new ArrayList<>(targets);
        }

        if (!force)
        {
            final List<Subject> tpPrevention = actualTargets.stream()
                  .filter(e -> !e.equals(context.subject())) // not self
                  .filter(e -> e instanceof Subject).map(Subject.class::cast) // only subjects
                  .filter(subject -> perms.TELEPORT_PREVENT_TP.check(subject)) // with prevent tp permission
                  .collect(Collectors.toList());
            if (!tpPrevention.isEmpty())
            {
                i18n.sendN(ACTION_BAR, context, NEGATIVE, tpPrevention.size(),
                           "You are not allowed to teleport {user}!",
                           "You are not allowed to teleport {1:integer} users.",
                           tpPrevention.get(0),
                           tpPrevention.size());
            }
            actualTargets.removeAll(tpPrevention);
        }

        if (!force && destination.player != null && !context.audience().equals(destination.player))
        {
            if (perms.TELEPORT_PREVENT_TPTO.check(destination.player)) // teleport to the target
            {
                if (context.hasPermission(perms.COMMAND_TP_FORCE.getId()))
                {
                    i18n.send(ACTION_BAR, context, POSITIVE, "Use the {text:-force (-f)} flag to teleport to this player."); //Show force flag if has permission
                }
                i18n.send(ACTION_BAR, context, NEGATIVE, "You are not allowed to teleport to {user}!", destination.player);
                return;
            }
        } // else equals tphere -> no need to check tpto perm

        if (actualTargets.size() == 1 && actualTargets.contains(destination.player))
        {
            if (context.audience().equals(destination.player))
            {
                i18n.send(ACTION_BAR, context, NEUTRAL, "You found yourself!");
                return;
            }
            i18n.send(ACTION_BAR, context, NEUTRAL, "You just teleported {user} to {user}... Not very useful right?", destination.player, destination.player);
            return;
        }
        actualTargets.remove(destination.player); // Dont bother
        if (actualTargets.isEmpty())
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "Nothing to teleport");
            return;
        }
        for (Entity target : actualTargets)
        {
            target.setLocation(destination.position(target));
            if (target instanceof ServerPlayer)
            {
                if (destination.player != null)
                {
                    i18n.send(ACTION_BAR, (ServerPlayer) target, POSITIVE, "You got teleported to {user}!", destination.player);
                }
                else
                {
                    i18n.send(ACTION_BAR, (ServerPlayer) target, POSITIVE, "You got teleported to {vector}!", destination.location.toInt());
                }
            }
        }
        if (destination.player != null)
        {
            i18n.sendN(ACTION_BAR, context, POSITIVE, actualTargets.size(),
                       "Teleported to {user}!",
                       "Teleported {1:number} entities to {user}", destination.player, actualTargets.size());
        }
        else
        {
            i18n.sendN(ACTION_BAR, context, POSITIVE, actualTargets.size(),
                       "Teleported to {vector}!",
                       "Teleported {1:number} entities to {vector}", destination.location.toInt(), actualTargets.size());
        }

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
        ServerLocation target = player.serverLocation();
        for (ServerPlayer p : Sponge.server().onlinePlayers())
        {
            if (!force && p.hasPermission(perms.TELEPORT_PREVENT_TP.getId()))
            {
                noTp.add(p.name());
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

        player.setLocation(context.serverLocation());
        i18n.send(ACTION_BAR, context, POSITIVE, "You teleported {user} to you!", player);
        i18n.send(ACTION_BAR, player, POSITIVE, "You were teleported to {sender}", context);
    }

    @Command(desc = "Teleport every player directly to you.")
    @Restricted(msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tphereall(ServerPlayer context, @Flag boolean force)
    {
        force = force && context.hasPermission(perms.COMMAND_TPHEREALL_FORCE.getId());
        ArrayList<String> noTp = new ArrayList<>();
        ServerLocation target = context.serverLocation();
        for (ServerPlayer p : Sponge.server().onlinePlayers())
        {
            if (!force && p.hasPermission(perms.TELEPORT_PREVENT_TP.getId()))
            {
                noTp.add(p.name());
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
    public void tppos(CommandCause context, Integer x, @Option Integer y, Integer z, // TODO optional y coord breaks named params
                      @Default @Named({"in"}) ServerWorld world,
                      @Default @Named({"player", "p"}) ServerPlayer player,
                      @Flag boolean unsafe)
    {
        if (y == null)
        {
            y = world.highestYAt(x, z);
        }
        ServerLocation loc = world.location(x,y,z).add(0.5, 0, 0.5);
        unsafe = unsafe && context.hasPermission(perms.COMMAND_TPPOS_UNSAFE.getId());
        if (unsafe)
        {
            player.setLocation(loc);
        }
        else
        {
            final Optional<ServerLocation> safe = Sponge.server().teleportHelper().findSafeLocation(loc);
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

