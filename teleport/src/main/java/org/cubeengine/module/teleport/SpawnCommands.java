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
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parameter.TooFewArgumentsException;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.core.util.math.BlockVector3;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.Broadcaster;
import org.cubeengine.service.world.WorldSetSpawnEvent;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.command.CommandSource;
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
    private I18n i18n;

    public SpawnCommands(Teleport basics, EventManager em, Game game, Broadcaster bc, TeleportListener tl, I18n i18n)
    {
        this.module = basics;
        this.em = em;
        this.game = game;
        this.bc = bc;
        this.tl = tl;
        this.i18n = i18n;
    }

    @Command(desc = "Changes the global respawnpoint")
    public void setSpawn(CommandSource context, @Default World world, @Optional Integer x, @Optional Integer y, @Optional Integer z)
    {
        Vector3d direction = null;
        if (z == null)
        {
            if (!(context instanceof Player))
            {
                throw new TooFewArgumentsException();
            }
            final Location loc = ((Player)context).getLocation();
            x = loc.getBlockX();
            y = loc.getBlockY();
            z = loc.getBlockZ();
            direction = ((Player)context).getRotation();
        }
        em.fireEvent(new WorldSetSpawnEvent(this.module, world, new Location<>(world, x, y, z), direction, context));
        world.getWorldStorage().getWorldProperties().setSpawnPosition(new Vector3i(x, y, z));
        i18n.sendTranslated(context, POSITIVE, "The spawn in {world} is now set to {vector:x\\=:y\\=:z\\=}", world, new BlockVector3(x, y, z));
    }

    @Command(desc = "Teleports all players to spawn")
    public void spawnAll(CommandSource context, World world, @Flag boolean force)
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
    public void spawn(CommandSource context, @Default Player player, @Optional World world, @Flag boolean force)
    {
        // TODO if OptionSubjects available => per role spawn?

        world = world == null ? module.getConfig().getMainWorld() : world;
        if (world == null)
        {
            world = player.getWorld();
        }
        force = force && context.hasPermission(module.perms().CMD_SPAWN_FORCE.getId()) || context.equals( player);
        if (!force && player.hasPermission(module.perms().CMD_SPAWN_PREVENT.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to spawn {user}!", player);
            return;
        }
        final Location<World> spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
        Vector3d rotation = player.getRotation();
        player.setLocation(spawnLocation);
        player.setRotation(rotation);
        i18n.sendTranslated(context, POSITIVE, "You are now standing at the spawn in {world}!", world);
    }

    @Command(desc = "Teleports you to the spawn of given world")
    @Restricted(value = Player.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void tpworld(Player context, World world)
    {
        final Location<World> spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
        if (!context.hasPermission(module.permsTpWorld().getPermission(world.getName()).getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to teleport to this world!");
            return;
        }
        Vector3d rotation = context.getRotation();
        context.setLocation(spawnLocation);
        i18n.sendTranslated(context, POSITIVE, "Teleported to the spawn of world {world}!", world);
        context.setRotation(rotation);
    }
}
