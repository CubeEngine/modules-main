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

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;

public class SpawnFixListener
{
    private I18n i18n;

    public SpawnFixListener(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Listener
    public void join(final ClientConnectionEvent.Login event)
    {
        Transform<World> playerLoc = event.getToTransform();
        WorldBorder border = playerLoc.getExtent().getWorldBorder();
        Vector3d center = border.getCenter();
        double radius = border.getDiameter() / 2;
        double minX = center.getX() - radius;
        double maxX = center.getX() + radius;
        double minZ = center.getZ() - radius;
        double maxZ = center.getZ() + radius;
        double playerX = playerLoc.getLocation().getPosition().getX();
        double playerZ = playerLoc.getLocation().getPosition().getZ();

        if (playerX > maxX || playerX < minX || playerZ > maxZ || playerZ < minZ)
        {
            event.setToTransform(event.getToTransform().setLocation(playerLoc.getExtent().getSpawnLocation()));
        }
    }
}
