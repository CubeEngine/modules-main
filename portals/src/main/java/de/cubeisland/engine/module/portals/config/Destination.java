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

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Optional;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.module.core.util.WorldLocation;
import de.cubeisland.engine.service.world.ConfigWorld;
import de.cubeisland.engine.module.portals.Portal;
import de.cubeisland.engine.module.portals.Portals;
import de.cubeisland.engine.service.world.WorldManager;
import org.spongepowered.api.data.manipulator.entity.PassengerData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;

public class Destination
{
    public Type type;
    public ConfigWorld world;
    public WorldLocation location;
    public String portal;

    public Destination(WorldManager wm, Location location, Vector3d direction)
    {
        this.location = new WorldLocation(location, direction);
        this.world = new ConfigWorld(wm, (World)location.getExtent());
        this.type = Type.LOCATION;
    }

    public Destination(WorldManager wm, World world)
    {
        this.world = new ConfigWorld(wm, world);
        this.type = Type.WORLD;
    }

    public Destination(Portal portal)
    {
        this.portal = portal.getName();
        this.type = Type.PORTAL;
    }

    protected Destination()
    {}

    public void teleport(Entity entity, Portals module, boolean safe)
    {
        Location loc = null;
        Vector3d rotation = null;
        switch (type)
        {
        case PORTAL:
            Portal destPortal = module.getPortal(portal);
            if (destPortal == null)
            {
                if (entity instanceof User)
                {
                    ((User)entity).sendTranslated(NEGATIVE, "Destination portal {input} does not exist!", portal);
                }
                return;
            }
            loc = destPortal.getPortalPos();
            break;
        case WORLD:
            loc = world.getWorld().getSpawnLocation();
            loc = new Location(loc.getExtent(), loc.getBlockX() + 0.5, loc.getY(), loc.getBlockZ() + 0.5);
            break;
        case LOCATION:
            loc = location.getLocationIn(world.getWorld());
            rotation = location.getRotation();
            break;
        }
        Optional<PassengerData> passenger = entity.getData(PassengerData.class);
        if (passenger.isPresent())
        {
            entity = passenger.get().getBaseVehicle();
        }
        if (safe)
        {
            entity.setLocationSafely(loc);
        }
        else
        {
            entity.setLocation(loc);
        }
        if (rotation != null)
        {
            entity.setRotation(rotation);
        }
    }

    public enum Type
    {
        PORTAL, WORLD, LOCATION, RANDOM
    }

    // TODO completer for destination
}
