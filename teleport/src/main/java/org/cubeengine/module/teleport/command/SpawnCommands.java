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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.teleport.Teleport;
import org.cubeengine.module.teleport.permission.TeleportPerm;
import org.cubeengine.module.teleport.permission.TpWorldPermissions;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType.ACTION_BAR;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Contains spawn-commands.
 * /setspawn
 * /spawn
 * /tpworld
 */
@Singleton
public class SpawnCommands
{
    private final Teleport module;
    private Broadcaster bc;
    private I18n i18n;
    private TeleportPerm perms;
    private TpWorldPermissions worldPerms;

    @Inject
    public SpawnCommands(Teleport basics, Broadcaster bc, I18n i18n, TeleportPerm perms, TpWorldPermissions worldPerms)
    {
        this.module = basics;
        this.bc = bc;
        this.i18n = i18n;
        this.perms = perms;
        this.worldPerms = worldPerms;
    }

    @Command(desc = "Changes the global respawnpoint")
    public void setSpawn(CommandCause context, @Default ServerWorld world, @Option Integer x, @Option Integer y, @Option Integer z)
    {
        Vector3d direction = null;
        if (z == null)
        {
            if (!(context instanceof ServerPlayer))
            {
                i18n.send(context, CRITICAL,"Too few arguments!");
                return;
//                throw new TooFewArgumentsException();
            }
            final ServerLocation loc = ((ServerPlayer)context).getServerLocation();
            x = loc.getBlockX();
            y = loc.getBlockY();
            z = loc.getBlockZ();
            direction = ((Player)context).getRotation();
        }
        //em.fireEvent(new WorldSetSpawnEvent(this.module, world, new Location<>(world, x, y, z), direction, context));
        world.getProperties().setSpawnPosition(new Vector3i(x, y, z));
        i18n.send(context, POSITIVE, "The spawn in {world} is now set to {vector:x\\=:y\\=:z\\=}", world, new Vector3i(x, y, z));
    }

    @Command(desc = "Teleports all players to spawn")
    public void spawnAll(CommandCause context, ServerWorld world, @Flag boolean force)
    {
        ServerLocation loc = world.getLocation(world.getProperties().getSpawnPosition().add(0.5, 0, 0.5));
        for (ServerPlayer aPlayer : Sponge.getServer().getOnlinePlayers())
        {
            if (!force && aPlayer.hasPermission(perms.CMD_SPAWN_PREVENT.getId()))
            {
                continue;
            }
            aPlayer.setLocation(loc);
        }
        bc.broadcastTranslated(POSITIVE, "Teleported everyone to the spawn of {world}!", world);
    }

    @Command(desc = "Teleports a player to spawn")
    public void spawn(CommandCause context, @Default ServerPlayer player, @Option ServerWorld world, @Flag boolean force)
    {
        // TODO if OptionSubjects available => per role spawn?

        world = world == null ? module.getConfig().getMainWorld() : world;
        if (world == null)
        {
            world = player.getWorld();
        }
        force = force && context.hasPermission(perms.CMD_SPAWN_FORCE.getId()) || context.equals( player);
        if (!force && player.hasPermission(perms.CMD_SPAWN_PREVENT.getId()))
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "You are not allowed to spawn {user}!", player);
            return;
        }
        final ServerLocation spawnLocation = world.getLocation(world.getProperties().getSpawnPosition().add(0.5, 0, 0.5));
        Vector3d rotation = player.getRotation();
        player.setLocation(spawnLocation);
        player.setRotation(rotation);
        i18n.send(ACTION_BAR, context, POSITIVE, "You are now standing at the spawn in {world}!", world);
    }

    @Command(desc = "Teleports you to the spawn of given world")
    @Restricted(msg = "Pro Tip: Teleport does not work IRL!")
    public void tpworld(ServerPlayer context, ServerWorld world)
    {
        final ServerLocation spawnLocation = world.getLocation(world.getProperties().getSpawnPosition().add(0.5, 0, 0.5));
        final ResourceKey worldKey = world.getProperties().getKey();
        if (!context.hasPermission(worldPerms.getPermission(worldKey.getNamespace() + "." + worldKey.getValue()).getId()))
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "You are not allowed to teleport to this world!");
            return;
        }
        Vector3d rotation = context.getRotation();
        context.setLocation(spawnLocation);
        i18n.send(ACTION_BAR, context, POSITIVE, "Teleported to the spawn of world {world}!", world);
        context.setRotation(rotation);
    }
}
