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

import java.util.UUID;
import org.cubeengine.service.user.UserAttachment;
import org.spongepowered.api.world.Location;

public class TeleportAttachment extends UserAttachment
{
    private Location deathLocation;

    public void setDeathLocation(Location deathLocation)
    {
        this.deathLocation = deathLocation;
    }

    /**
     * Also nulls the location
     *
     * @return the location
     */
    public Location getDeathLocation()
    {
        Location loc = deathLocation;
        deathLocation = null;
        return loc;
    }

    private Location lastLocation = null;

    public Location getLastLocation()
    {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation)
    {
        this.lastLocation = lastLocation;
    }

    private UUID tpRequestCancelTask;
    private UUID pendingTpToRequest;
    private UUID pendingTpFromRequest;

    public void setTpRequestCancelTask(UUID tpRequestCancelTask)
    {
        this.tpRequestCancelTask = tpRequestCancelTask;
    }

    public UUID getTpRequestCancelTask()
    {
        return tpRequestCancelTask;
    }

    public void removeTpRequestCancelTask()
    {
        this.tpRequestCancelTask = null;
    }

    public void setPendingTpToRequest(UUID pendingTpToRequest)
    {
        this.pendingTpToRequest = pendingTpToRequest;
    }

    public UUID getPendingTpToRequest()
    {
        return pendingTpToRequest;
    }

    public void removePendingTpToRequest()
    {
        pendingTpToRequest = null;
    }

    public void setPendingTpFromRequest(UUID pendingTpFromRequest)
    {
        this.pendingTpFromRequest = pendingTpFromRequest;
    }

    public UUID getPendingTpFromRequest()
    {
        return pendingTpFromRequest;
    }

    public void removePendingTpFromRequest()
    {
        pendingTpFromRequest = null;
    }
}
