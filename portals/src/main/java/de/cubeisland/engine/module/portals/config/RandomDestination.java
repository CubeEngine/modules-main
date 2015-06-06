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
import com.google.common.base.Optional;
import de.cubeisland.engine.module.core.util.Pair;
import de.cubeisland.engine.module.portals.Portals;
import de.cubeisland.engine.module.service.world.ConfigWorld;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.entity.PassengerData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class RandomDestination extends Destination
{
    private final Random random = new Random();
    private Game game;

    public RandomDestination(Game game, WorldManager wm, World world)
    {
        this.game = game;
        this.type = Type.RANDOM;
        this.world = new ConfigWorld(wm, world);
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
        Optional<PassengerData> vehicle = entity.getData(PassengerData.class);
        if (vehicle.isPresent())
        {
            entity = vehicle.get().getBaseVehicle();
        }
        World world = this.world.getWorld();
        Location block;
        Pair<Integer, Chunk> config = module.getRandomDestinationSetting(world);
        Chunk chunk = config.getRight();
        int x = random.nextInt(2 * config.getLeft() + 1) - config.getLeft();
        int z = random.nextInt(2 * config.getLeft() + 1) - config.getLeft();
        chunk = world.loadChunk(chunk.getPosition().getX() + x * 16, 0, chunk.getPosition().getZ() + z * 16, true).get();
        block = world.getFullBlock(chunk.getPosition().add(random.nextInt(16), 0, random.nextInt(16)));
        game.getTeleportHelper().getSafeLocation(chunk.getFullBlock(random.nextInt(16), world.getBuildHeight() / 4,
                                                                    random.nextInt(16)));
        entity.setLocation(block);
    }
}
