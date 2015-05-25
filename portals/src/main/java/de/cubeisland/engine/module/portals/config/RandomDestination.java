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
package de.cubeisland.engine.module.portals.config;

import java.util.Random;
import de.cubeisland.engine.module.core.CubeEngine;
import de.cubeisland.engine.module.core.sponge.BukkitUtils;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.Pair;
import de.cubeisland.engine.module.service.world.ConfigWorld;
import de.cubeisland.engine.module.portals.PortalManager;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.World;

public class RandomDestination extends Destination
{
    private final Random random = new Random();

    public RandomDestination(WorldManager wm, World world)
    {
        this.type = Type.RANDOM;
        this.world = new ConfigWorld(wm, world);
    }

    protected RandomDestination()
    {
    }

    @Override
    public void teleport(Entity entity, PortalManager manager, boolean safe)
    {
        if (entity instanceof User)
        {
            if (entity.isInsideVehicle())
            {
                entity = entity.getVehicle();
            }
            World world = this.world.getWorld();
            Block block;
            do
            {
                Pair<Integer, Chunk> config = manager.getRandomDestinationSetting(world);
                int x = random.nextInt(2 * config.getLeft() + 1) - config.getLeft();
                int z = random.nextInt(2 * config.getLeft() + 1) - config.getLeft();
                Chunk chunk = world.getChunkAt(config.getRight().getX() + x, config.getRight().getZ() + z);

                block = chunk.getBlock(random.nextInt(16), 0, random.nextInt(16));
                block = block.getWorld().getHighestBlockAt(block.getLocation());
            }
            while (!block.getRelative(BlockFace.DOWN).getType().isSolid()); // do not land on water or lava :)

            if ((entity.getLocation().getWorld() == world || entity instanceof User) && entity.getPassenger() == null)
            {
                entity.teleport(block.getLocation());
            }
            else if (entity instanceof CraftEntity)
            {
                BukkitUtils.teleport(manager.module, ((CraftEntity)entity).getHandle(), block.getLocation());
            }
            else
            {
                manager.module.getLog().warn("Could not teleport entity: {}", entity);
            }
        }
        // else ignore
    }
}
