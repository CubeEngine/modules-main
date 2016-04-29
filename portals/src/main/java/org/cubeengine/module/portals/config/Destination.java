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

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.module.portals.Portal;
import org.cubeengine.module.portals.Portals;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.config.WorldTransform;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

public class Destination
{
    public Type type;
    public ConfigWorld world;
    public WorldTransform location;
    public String portal;
    private I18n i18n;

    public Destination(Location<World> location, Vector3d direction, I18n i18n)
    {
        this.i18n = i18n;
        this.location = new WorldTransform(location, direction);
        this.world = new ConfigWorld(location.getExtent());
        this.type = Type.LOCATION;
    }

    public Destination(World world)
    {
        this.world = new ConfigWorld(world);
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
        Location<World> loc;
        Vector3d rotation = null;
        switch (type)
        {
        case PORTAL:
            Portal destPortal = module.getPortal(portal);
            if (destPortal == null)
            {
                if (entity instanceof Player)
                {
                    i18n.sendTranslated(((Player)entity), NEGATIVE, "Destination portal {input} does not exist!", portal);
                }
                return;
            }
            loc = destPortal.getPortalPos();
            break;
        case WORLD:
            loc = world.getWorld().getSpawnLocation();
            loc = new Location<>(loc.getExtent(), loc.getBlockX() + 0.5, loc.getY(), loc.getBlockZ() + 0.5);
            break;
        case LOCATION:
            loc = location.getLocationIn(world.getWorld());
            rotation = location.getRotation();
            break;
        default:
            throw new IllegalStateException();
        }
        // TODO check if this is working when riding on a horse
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
