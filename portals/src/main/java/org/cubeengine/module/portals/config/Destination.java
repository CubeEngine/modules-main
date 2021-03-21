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
package org.cubeengine.module.portals.config;

import java.util.Optional;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.portals.Portal;
import org.cubeengine.module.portals.Portals;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

public class Destination
{
    public Type type;
    public ConfigWorld world;
    public Vector3d position;
    public Vector3d rotation;
    public String portal;
    private I18n i18n;

    public Destination(ServerLocation location, Vector3d rotation, I18n i18n)
    {
        this.i18n = i18n;
        this.world = new ConfigWorld(location.world());
        this.position = location.position();
        this.rotation = rotation;
        this.type = Type.LOCATION;
    }

    public Destination(ServerWorld world)
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
        ServerLocation loc;
        Vector3d rotation = null;
        final ServerWorld world = this.world.getWorld();
        switch (type)
        {
        case PORTAL:
            Portal destPortal = module.getPortal(portal);
            if (destPortal == null)
            {
                if (entity instanceof ServerPlayer)
                {
                    i18n.send(((ServerPlayer)entity), NEGATIVE, "Destination portal {input} does not exist!", portal);
                }
                return;
            }
            loc = destPortal.getPortalPos();
            rotation = destPortal.getPortalRot();
            break;
        case WORLD:
            loc = world.location(world.properties().spawnPosition()).add(0.5, 0, 0.5);
            break;
        case LOCATION:
            if (this.position == null)
            {
                return;
            }
            loc = world.location(this.position);
            rotation = this.rotation;
            break;
        default:
            throw new IllegalStateException();
        }
        // TODO check if this is working when riding on a horse
        if (safe)
        {
            final Optional<ServerLocation> safeLoc = Sponge.server().teleportHelper().findSafeLocation(loc);
            if (safeLoc.isPresent())
            {
                entity.setLocation(safeLoc.get());
            }
            else if (entity instanceof ServerPlayer)
            {
                i18n.send((ServerPlayer)entity, NEGATIVE, "Target destination is unsafe");
            }
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
}
