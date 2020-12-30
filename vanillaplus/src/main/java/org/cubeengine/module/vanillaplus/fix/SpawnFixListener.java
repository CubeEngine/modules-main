/*
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
package org.cubeengine.module.vanillaplus.fix;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.world.WorldBorder;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

public class SpawnFixListener
{
    @Listener
    public void join(final ServerSideConnectionEvent.Login event)
    {
        final ServerLocation toLocation = event.getToLocation();
        final ServerWorld toWorld = toLocation.getWorld();
        final WorldBorder border = toWorld.getBorder();
        Vector3d center = border.getCenter();
        double radius = border.getDiameter() / 2;
        double minX = center.getX() - radius;
        double maxX = center.getX() + radius;
        double minZ = center.getZ() - radius;
        double maxZ = center.getZ() + radius;
        double playerX = event.getUser().getPosition().getX();
        double playerZ = event.getUser().getPosition().getZ();

        if (playerX > maxX || playerX < minX || playerZ > maxZ || playerZ < minZ)
        {
            event.setToLocation(toWorld.getLocation(toWorld.getProperties().getSpawnPosition()));
        }
    }
}
