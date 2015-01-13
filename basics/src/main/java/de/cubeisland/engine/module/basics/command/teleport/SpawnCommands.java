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
package de.cubeisland.engine.module.basics.command.teleport;

import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.command.methodic.parametric.Named;
import de.cubeisland.engine.command.methodic.parametric.Optional;
import de.cubeisland.engine.command.parameter.TooFewArgumentsException;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.UserList;
import org.bukkit.Location;
import org.bukkit.World;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.core.world.WorldSetSpawnEvent;
import de.cubeisland.engine.module.basics.Basics;

import static de.cubeisland.engine.command.parameter.property.Requirement.OPTIONAL;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;

/**
 * Contains spawn-commands.
 * /setspawn
 * /spawn
 * /tpworld
 */
public class SpawnCommands
{
    private final Basics module;

    public SpawnCommands(Basics basics)
    {
        this.module = basics;
    }

    @Command(desc = "Changes the global respawnpoint")
    public void setSpawn(CommandSender context, @Default World world, @Optional Integer x, @Optional Integer y, @Optional Integer z)
    {
        if (z == null)
        {
            if (!(context instanceof User))
            {
                throw new TooFewArgumentsException();
            }
            final Location loc = ((User)context).getLocation();
            x = loc.getBlockX();
            y = loc.getBlockY();
            z = loc.getBlockZ();
        }
        this.module.getCore().getEventManager().fireEvent(new WorldSetSpawnEvent(this.module.getCore(), world, new Location(world, x,y,z)));
        world.setSpawnLocation(x, y, z);
        context.sendTranslated(POSITIVE, "The spawn in {world} is now set to {vector:x\\=:y\\=:z\\=}", world, new BlockVector3(x, y, z));
    }

    @Command(desc = "Teleport directly to the worlds spawn.")
    public void spawn(CommandSender context, @Optional User player, @Named({"world", "w"}) World world,
                      @Flag boolean force, @Flag boolean all)
    {
        world = world == null ? module.getConfiguration().mainWorld : world;
        if (world == null && context instanceof User)
        {
            world = ((User)context).getWorld();
        }
        if (world == null)
        {
            throw new TooFewArgumentsException();
        }
        force = force && module.perms().COMMAND_SPAWN_FORCE.isAuthorized(context);
        if (all)
        {
            if (!module.perms().COMMAND_SPAWN_ALL.isAuthorized(context))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to spawn everyone!");
                return;
            }
            Location loc = world.getSpawnLocation().add(0.5, 0, 0.5);
            for (User aPlayer : module.getCore().getUserManager().getOnlineUsers())
            {
                if (!force)
                {
                    if (module.perms().COMMAND_SPAWN_PREVENT.isAuthorized(aPlayer))
                    {
                        continue;
                    }
                }
                if (!TeleportCommands.teleport(aPlayer, loc, true, force, true))
                {
                    return;
                }
            }
            this.module.getCore().getUserManager().broadcastTranslated(POSITIVE, "Teleported everyone to the spawn of {world}!", world);
            return;
        }
        if (player == null)
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "{text:Pro Tip}: Teleport does not work IRL!");
                return;
            }
            player = (User)context;
        }
        if (!player.isOnline())
        {
            context.sendTranslated(NEGATIVE, "You cannot teleport an offline player to spawn!");
            return;
        }
        if (!force && module.perms().COMMAND_SPAWN_PREVENT.isAuthorized(player))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to spawn {user}!", player);
            return;
        }
        final Location spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
        final Location userLocation = player.getLocation();
        spawnLocation.setPitch(userLocation.getPitch());
        spawnLocation.setYaw(userLocation.getYaw());
        if (!TeleportCommands.teleport(player, spawnLocation, true, force, true))
        {
            context.sendTranslated(NEGATIVE, "Teleport failed!");
        }
    }

    @Command(desc = "Teleports you to the spawn of given world")
    @Restricted(value = User.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void tpworld(User context, World world)
    {
        final Location spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
        final Location userLocation = context.getLocation();
        spawnLocation.setPitch(userLocation.getPitch());
        spawnLocation.setYaw(userLocation.getYaw());
        if (!module.perms().tpWorld().getPermission(world.getName()).isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to teleport to this world!");
            return;
        }
        if (TeleportCommands.teleport(context, spawnLocation, true, false, true))
        {
            context.sendTranslated(POSITIVE, "Teleported to the spawn of world {world}!", world);
        }
    }
}
