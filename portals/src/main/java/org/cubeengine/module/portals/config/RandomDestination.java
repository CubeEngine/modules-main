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
package org.cubeengine.module.portals.config;

import java.util.Random;
import org.cubeengine.libcube.util.Pair;
import org.cubeengine.module.portals.Portals;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class RandomDestination extends Destination
{
    private final Random random = new Random();
    private Game game;

    public RandomDestination(Game game, World world)
    {
        this.game = game;
        this.type = Type.RANDOM;
        this.world = new ConfigWorld(world);
    }

    protected RandomDestination()
    {
    }

    @Override
    public void teleport(Entity entity, Portals module, boolean safe)
    {
        if (!(entity instanceof Player))
        {
            // TODO particles
            return;
        }
        World world = this.world.getWorld();
        Location<World> block;
        Pair<Integer, Chunk> config = module.getRandomDestinationSetting(world);
        Chunk chunk = config.getRight();
        int x = random.nextInt(2 * config.getLeft() + 1) - config.getLeft();
        int z = random.nextInt(2 * config.getLeft() + 1) - config.getLeft();
        chunk = world.loadChunk(chunk.getPosition().getX() + x * 16, 0, chunk.getPosition().getZ() + z * 16, true).get();
        block = world.getLocation(chunk.getPosition().add(random.nextInt(16), 0, random.nextInt(16)));
        block = game.getTeleportHelper().getSafeLocation(block).orElse(block);
        entity.setLocation(block);
    }
}
