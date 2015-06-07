package de.cubeisland.engine.module.teleport;

import java.util.UUID;
import de.cubeisland.engine.module.service.user.UserAttachment;
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
