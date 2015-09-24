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
import org.cubeengine.service.user.Broadcaster;
import org.cubeengine.service.user.MultilingualCommandSource;
import org.cubeengine.service.user.MultilingualPlayer;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldSetSpawnEvent;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
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
    private Game game;
    private Broadcaster bc;
    private TeleportListener tl;

    public SpawnCommands(Teleport basics, EventManager em, Game game, Broadcaster bc, TeleportListener tl)
    {
        this.module = basics;
        this.em = em;
        this.game = game;
        this.bc = bc;
        this.tl = tl;
    }

    @Command(desc = "Changes the global respawnpoint")
    public void setSpawn(MultilingualCommandSource context, @Default World world, @Optional Integer x, @Optional Integer y, @Optional Integer z)
    {
        Vector3d direction = null;
        if (z == null)
        {
            if (!(context instanceof MultilingualPlayer))
            {
                throw new TooFewArgumentsException();
            }
            final Location loc = ((MultilingualPlayer)context).original().getLocation();
            x = loc.getBlockX();
            y = loc.getBlockY();
            z = loc.getBlockZ();
            direction = ((MultilingualPlayer)context).original().getRotation();
        }
        em.fireEvent(new WorldSetSpawnEvent(this.module, world, new Location(world, x, y, z), direction));
        world.getWorldStorage().getWorldProperties().setSpawnPosition(new Vector3i(x, y, z));
        context.sendTranslated(POSITIVE, "The spawn in {world} is now set to {vector:x\\=:y\\=:z\\=}", world, new BlockVector3(x, y, z));
    }

    @Command(desc = "Teleports all players to spawn")
    public void spawnAll(MultilingualCommandSource context, World world, @Flag boolean force)
    {
        Location<World> loc = world.getSpawnLocation().add(0.5, 0, 0.5);
        for (Player aPlayer : game.getServer().getOnlinePlayers())
        {
            if (!force && aPlayer.hasPermission(module.perms().CMD_SPAWN_PREVENT.getId()))
            {
                continue;
            }
            aPlayer.setLocation(loc);
        }
        bc.broadcastTranslated(POSITIVE, "Teleported everyone to the spawn of {world}!", world);
    }

    @Command(desc = "Teleports a player to spawn")
    public void spawn(MultilingualCommandSource context, @Default MultilingualPlayer player, @Optional World world, @Flag boolean force)
    {
        world = world == null ? module.getConfig().getMainWorld() : world;
        if (world == null)
        {
            world = player.original().getWorld();
        }
        force = force && context.hasPermission(module.perms().CMD_SPAWN_FORCE.getId()) || context.getSource().equals( player.getSource());
        if (!force && player.hasPermission(module.perms().CMD_SPAWN_PREVENT.getId()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to spawn {user}!", player);
            return;
        }
        final Location spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
        Vector3d rotation = player.original().getRotation();
        player.getSource().setLocation(spawnLocation);
        player.original().setRotation(rotation);
        context.sendTranslated(POSITIVE, "You are now standing at the spawn in {world}!", world);
    }

    @Command(desc = "Teleports you to the spawn of given world")
    @Restricted(value = MultilingualPlayer.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void tpworld(MultilingualPlayer context, World world)
    {
        final Location spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
        if (!context.hasPermission(module.permsTpWorld().getPermission(world.getName()).getId()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to teleport to this world!");
            return;
        }
        Vector3d rotation = context.original().getRotation();
        context.getSource().setLocation(spawnLocation);
        context.sendTranslated(POSITIVE, "Teleported to the spawn of world {world}!", world);
        context.original().setRotation(rotation);
    }
}
