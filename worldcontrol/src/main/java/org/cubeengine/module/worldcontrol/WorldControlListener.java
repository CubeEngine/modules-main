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
package org.cubeengine.module.worldcontrol;

import org.cubeengine.module.multiverse.Universe;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class WorldControlListener
{
    // TODO handle different Netherportal / Endportal behaviour
    @Listener
    public void onPortalEnter(EntityEnterPortalEvent event)
    {
        Universe universe = getUniverseFrom(event.getEntity().getWorld());
        //universe.handleNetherTarget()
        //universe.handleEndTarget()
    }

    @Listener
    public void onPortalExit(EntityExitPortalEvent event)
    {
    }


    public Location handleNetherTarget(Location from, TravelAgent agent)
    {
        WorldConfig fromConfig = this.worldConfigs.get(from.getWorld().getName());
        World toWorld = this.wm.getWorld(fromConfig.netherTarget);
        WorldConfig toConfig = this.multiverse.getUniverseFrom(toWorld).getWorldConfig(toWorld);

        double factor = fromConfig.scale / toConfig.scale;
        double searchFactor = toConfig.scale / fromConfig.scale;
        searchFactor = searchFactor > factor ? factor : searchFactor;

        agent.setSearchRadius((int) (128d / (searchFactor * 8)));
        agent.setCreationRadius((int) (16d / (searchFactor * 8)));
        return new Location(toWorld, from.getX() * factor, from.getY(), from.getZ() * factor, from.getYaw(), from.getPitch());
    }

    public Location handleEndTarget(Location from)
    {
        WorldConfig fromConfig = this.worldConfigs.get(from.getWorld().getName());
        World toWorld = this.wm.getWorld(fromConfig.endTarget);
        if (toWorld.getEnvironment() == Environment.THE_END)
        {
            return new Location(toWorld, 100, 50, 0, from.getYaw(), from.getPitch()); // everything else wont work when using the TravelAgent
        }
        Location spawnLocation = toWorld.getSpawnLocation();
        spawnLocation.setYaw(from.getYaw());
        spawnLocation.setPitch(from.getPitch());
        return spawnLocation;
    }

    public boolean hasNetherTarget(World world)
    {
        WorldConfig worldConfig = this.worldConfigs.get(world.getName());
        return worldConfig.netherTarget != null &&
                this.wm.getWorld(worldConfig.netherTarget) != null;
    }

    public boolean hasEndTarget(World world)
    {
        WorldConfig worldConfig = this.worldConfigs.get(world.getName());
        if (worldConfig.endTarget != null)
        {
            World target = this.wm.getWorld(worldConfig.endTarget);
            if (target != null)
            {
                if (world.getEnvironment() != Environment.THE_END)
                {
                    if (target.getEnvironment() == Environment.THE_END)
                    {
                        return true;
                    }
                    this.logger.warn("End target {} coming from {} is not a End world!", target.getName(), world.getName());
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
