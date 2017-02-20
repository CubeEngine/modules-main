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

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.libcube.util.Pair;
import org.cubeengine.module.portals.Portals;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class RandomDestination extends Destination
{
    private final Random random = new Random();

    public RandomDestination(World world)
    {
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
        Pair<Integer, Vector3i> config = module.getRandomDestinationSetting(world);
        int x = random.nextInt(2 * config.getLeft() + 1) - config.getLeft();
        int z = random.nextInt(2 * config.getLeft() + 1) - config.getLeft();
        Vector3i pos = config.getRight().add(x, 0, z);
        world.loadChunk(Sponge.getServer().getChunkLayout().forceToChunk(pos.getX(), pos.getY(), pos.getZ()), true);
        Location<World> block = world.getLocation(pos);
        block = Sponge.getGame().getTeleportHelper().getSafeLocation(block, 256, 16).orElse(block);
        entity.setLocation(block);
    }
}
