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
import org.spongepowered.api.world.border.WorldBorder;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3d;

public class SpawnFixListener
{
    @Listener
    public void join(final ServerSideConnectionEvent.Login event)
    {
        final ServerLocation toLocation = event.toLocation();
        final ServerWorld toWorld = toLocation.world();
        final WorldBorder border = toWorld.border();
        Vector2d center = border.center();
        double radius = border.diameter() / 2;
        double minX = center.x() - radius;
        double maxX = center.x() + radius;
        double minZ = center.y() - radius;
        double maxZ = center.y() + radius;
        double playerX = toLocation.position().x();
        double playerZ = toLocation.position().z();

        if (playerX > maxX || playerX < minX || playerZ > maxZ || playerZ < minZ)
        {
            event.setToLocation(toWorld.location(toWorld.properties().spawnPosition()));
        }
    }
}
