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
    @Params(positional = {@Param(req = false, label = "world", type = World.class),
              @Param(req = false, label = "x", type = Integer.class),
              @Param(req = false, label = "y", type = Integer.class),
              @Param(req = false, label = "z", type = Integer.class)})
    public void setSpawn(CommandContext context)
    {
        User sender = null;
        if (context.getSource() instanceof User)
        {
            sender = (User)context.getSource();
        }
        Integer x;
        Integer y;
        Integer z;
        World world;
        if (context.hasPositional(0))
        {
            world = context.get(0);
        }
        else
        {
            if (sender == null)
            {
                context.sendTranslated(NEGATIVE, "If not used ingame you have to specify a world and coordinates!");
                return;
            }
            world = sender.getWorld();
        }

        if (context.hasPositional(3))
        {
            x = context.get(1, null);
            y = context.get(2, null);
            z = context.get(3, null);
            if (x == null || y == null || z == null)
            {
                context.sendTranslated(NEGATIVE, "Coordinates are invalid!");
                return;
            }
            this.module.getCore().getEventManager().fireEvent(new WorldSetSpawnEvent(this.module.getCore(), world, new Location(world, x,y,z)));
        }
        else
        {
            if (sender == null)
            {
                context.sendTranslated(NEGATIVE, "If not used ingame you have to specify a world and coordinates!");
                return;
            }
            final Location loc = sender.getLocation();
            this.module.getCore().getEventManager().fireEvent(new WorldSetSpawnEvent(this.module.getCore(), world, loc));
            x = loc.getBlockX();
            y = loc.getBlockY();
            z = loc.getBlockZ();
        }
        world.setSpawnLocation(x, y, z);
        context.sendTranslated(POSITIVE, "The spawn in {world} is now set to {vector:x\\=:y\\=:z\\=}", world, new BlockVector3(x, y, z));
    }

    @Command(desc = "Teleport directly to the worlds spawn.")
    @Params(positional = @Param(req = false, label = "player", type = User.class),
            nonpositional = @Param(names = {"world", "w"}, type = World.class))
    @Flags({@Flag(longName = "force", name = "f"),
            @Flag(longName = "all", name = "a")})
    public void spawn(CommandContext context)
    {
        User user = null;
        if (context.getSource() instanceof User)
        {
            user = (User)context.getSource();
        }
        World world = module.getConfiguration().mainWorld;
        if (world == null && user != null)
        {
            world = user.getWorld();
        }
        boolean force = false;
        if (context.hasFlag("f") && module.perms().COMMAND_SPAWN_FORCE.isAuthorized(context.getSource()))
        {
            force = true; // if not allowed ignore flag
        }
        if (context.hasNamed("world"))
        {
            world = context.get("world", null);
            if (world == null)
            {
                context.sendTranslated(NEGATIVE, "World {input#world} not found!", context.getString("world"));
                return;
            }
        }
        if (world == null)
        {
            context.sendTranslated(NEGATIVE, "You have to specify a world!");
            return;
        }
        if (context.hasFlag("a"))
        {
            if (!module.perms().COMMAND_SPAWN_ALL.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to spawn everyone!");
                return;
            }
            Location loc = world.getSpawnLocation().add(0.5, 0, 0.5);
            for (User player : context.getCore().getUserManager().getOnlineUsers())
            {
                if (!force)
                {
                    if (module.perms().COMMAND_SPAWN_PREVENT.isAuthorized(player))
                    {
                        continue;
                    }
                }
                if (!TeleportCommands.teleport(player, loc, true, force, true))
                {
                    return;
                }
            }
            this.module.getCore().getUserManager().broadcastTranslated(POSITIVE, "Teleported everyone to the spawn of {world}!", world);
            return;
        }
        if (context.hasPositional(0))
        {
            user = context.get(0);
            if (!user.isOnline())
            {
                context.sendTranslated(NEGATIVE, "You cannot teleport an offline player to spawn!");
                return;
            }
            if (!force && module.perms().COMMAND_SPAWN_PREVENT.isAuthorized(user))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to spawn {user}!", user);
                return;
            }
        }
        else if (user == null)
        {
            context.sendTranslated(NEGATIVE, "{text:Pro Tip}: Teleport does not work IRL!");
            return;
        }
        final Location spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
        final Location userLocation = user.getLocation();
        spawnLocation.setPitch(userLocation.getPitch());
        spawnLocation.setYaw(userLocation.getYaw());
        if (!TeleportCommands.teleport(user, spawnLocation, true, force, true))
        {
            context.sendTranslated(NEGATIVE, "Teleport failed!");
        }
    }

    @Command(desc = "Teleports you to the spawn of given world")
    @Params(positional = @Param(label = "world", type = World.class))
    public void tpworld(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            User sender = (User)context.getSource();
            World world = context.get(0);
            final Location spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
            final Location userLocation = sender.getLocation();
            spawnLocation.setPitch(userLocation.getPitch());
            spawnLocation.setYaw(userLocation.getYaw());
            if (!module.perms().tpWorld().getPermission(world.getName()).isAuthorized(sender))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to teleport to this world!");
                return;
            }
            if (TeleportCommands.teleport(sender, spawnLocation, true, false, true))
            {
                context.sendTranslated(POSITIVE, "Teleported to the spawn of world {world}!", world);
            }
            return;
        }
        context.sendTranslated(NEUTRAL, "Pro Tip: Teleport does not work IRL!");
    }
}
