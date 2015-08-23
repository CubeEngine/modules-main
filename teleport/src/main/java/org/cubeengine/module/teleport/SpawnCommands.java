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

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parameter.TooFewArgumentsException;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Optional;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.core.util.math.BlockVector3;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldSetSpawnEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Contains spawn-commands.
 * /setspawn
 * /spawn
 * /tpworld
 */
public class SpawnCommands
{
    private final Teleport module;
    private EventManager em;
    private UserManager um;

    public SpawnCommands(Teleport basics, EventManager em, UserManager um)
    {
        this.module = basics;
        this.em = em;
        this.um = um;
    }

    @Command(desc = "Changes the global respawnpoint")
    public void setSpawn(CommandSender context, @Default World world, @Optional Integer x, @Optional Integer y, @Optional Integer z)
    {
        Vector3d direction = null;
        if (z == null)
        {
            if (!(context instanceof User))
            {
                throw new TooFewArgumentsException();
            }
            final Location loc = ((User)context).asPlayer().getLocation();
            x = loc.getBlockX();
            y = loc.getBlockY();
            z = loc.getBlockZ();
            direction = ((User)context).asPlayer().getRotation();
        }
        em.fireEvent(new WorldSetSpawnEvent(this.module, world, new Location(world, x, y, z), direction));
        world.getWorldStorage().getWorldProperties().setSpawnPosition(new Vector3i(x, y, z));
        context.sendTranslated(POSITIVE, "The spawn in {world} is now set to {vector:x\\=:y\\=:z\\=}", world, new BlockVector3(x, y, z));
    }

    @Command(desc = "Teleports all players to spawn")
    public void spawnAll(CommandSender context, World world, @Flag boolean force)
    {
        Location loc = world.getSpawnLocation().add(0.5, 0, 0.5);
        for (User aPlayer : um.getOnlineUsers())
        {
            if (!force && aPlayer.hasPermission(module.perms().CMD_SPAWN_PREVENT.getId()))
            {
                continue;
            }
            aPlayer.getPlayer().get().setLocation(loc);
        }
        um.broadcastTranslated(POSITIVE, "Teleported everyone to the spawn of {world}!", world);
    }

    @Command(desc = "Teleports a player to spawn")
    public void spawn(CommandSender context, @Default User player, @Optional World world, @Flag boolean force)
    {
        world = world == null ? module.getConfig().getMainWorld() : world;
        if (world == null)
        {
            world = player.asPlayer().getWorld();
        }
        force = force && context.hasPermission(module.perms().CMD_SPAWN_FORCE.getId()) || context.getUniqueId().equals(
            player.getUniqueId());
        if (!player.getPlayer().isPresent())
        {
            context.sendTranslated(NEGATIVE, "You cannot teleport an offline player to spawn!");
            return;
        }
        if (!force && player.hasPermission(module.perms().CMD_SPAWN_PREVENT.getId()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to spawn {user}!", player);
            return;
        }
        final Location spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
        Vector3d rotation = player.asPlayer().getRotation();
        player.getPlayer().get().setLocation(spawnLocation);
        player.asPlayer().setRotation(rotation);
        context.sendTranslated(POSITIVE, "You are now standing at the spawn in {world}!", world);
    }

    @Command(desc = "Teleports you to the spawn of given world")
    @Restricted(value = User.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void tpworld(User context, World world)
    {
        final Location spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
        if (!context.hasPermission(module.permsTpWorld().getPermission(world.getName()).getId()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to teleport to this world!");
            return;
        }
        Vector3d rotation = context.asPlayer().getRotation();
        context.getPlayer().get().setLocation(spawnLocation);
        context.sendTranslated(POSITIVE, "Teleported to the spawn of world {world}!", world);
        context.asPlayer().setRotation(rotation);
    }
}