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
package org.cubeengine.module.portals;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.Pair;
import org.cubeengine.libcube.util.math.MathHelper;
import org.cubeengine.module.portals.config.PortalConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class Portal
{
    private final Portals module;
    private final String name;
    public final PortalConfig config;
    private final I18n i18n;

    public Portal(Portals module, String name, PortalConfig config, I18n i18n)
    {
        this.module = module;
        this.name = name;
        this.config = config;
        this.i18n = i18n;
    }

    public String getName()
    {
        return name;
    }

    public boolean has(ServerLocation location)
    {
        return location.world().key().equals(config.world.getWorld().key()) &&
            isBetween(config.location.from.getX(), config.location.to.getX(), location.blockX()) &&
            isBetween(config.location.from.getY(), config.location.to.getY(), location.blockY()) &&
            isBetween(config.location.from.getZ(), config.location.to.getZ(), location.blockZ());
    }

    private static boolean isBetween(int a, int b, int x)
    {
        return b > a ? x >= a && x <= b : x >= b && x <= a;
    }

    public void teleport(Entity entity)
    {
        if (this.config.destination == null)
        {
            if (entity instanceof Player)
            {
                i18n.send(((Player)entity), NEUTRAL, "This portal {name} has no destination yet!", this.getName());
                module.getPortalsAttachment(entity.uniqueId()).setInPortal(true);
            }
        }
        else
        {
            this.config.destination.teleport(entity, module, this.config.safeTeleport);
        }
    }

    public ServerLocation getPortalPos()
    {
        if (this.config.location.destination == null)
        {
            Vector3i midpoint = MathHelper.midpoint(this.config.location.to, this.config.location.from);
            return ServerLocation.of(this.config.world.getWorld(), midpoint.getX() + 0.5, midpoint.getY(), midpoint.getZ() + 0.5);
        }
        return this.config.world.getWorld().location(this.config.location.destination.position());
    }

    public Vector3d getPortalRot()
    {
        return this.config.location.destination.rotation();
    }

    public void delete()
    {
        module.removePortal(this);
        this.config.getFile().delete();
    }

    public void showInfo(Audience user)
    {
        i18n.send(user, POSITIVE, "Portal Information for {name#portal}", this.getName());
        if (this.config.safeTeleport)
        {
            i18n.send(user, POSITIVE, "This Portal has safe teleport enabled");
        }
        if (this.config.teleportNonPlayers)
        {
            i18n.send(user, POSITIVE, "This Portal will teleport non-players too");
        }
        i18n.send(user, POSITIVE, "{user} is the owner of this portal", this.config.owner);
        i18n.send(user, POSITIVE, "Location: {vector} to {vector} in {name#world}",
                            new Vector3i(this.config.location.from.getX(), this.config.location.from.getY(), this.config.location.from.getZ()),
                            new Vector3i(this.config.location.to.getX(), this.config.location.to.getY(), this.config.location.to.getZ()), this.config.world.getName());
        if (this.config.destination == null)
        {
            i18n.send(user, POSITIVE, "This portal has no destination yet");
        }
        else
        {
            switch (config.destination.type)
            {
            case PORTAL:
                i18n.send(user, POSITIVE, "This portal teleports to another portal: {name#portal}", config.destination.portal);
                break;
            case WORLD:
                i18n.send(user, POSITIVE, "This portal teleports to the spawn of {name#world}", config.destination.world.getName());
                break;
            case LOCATION:
                i18n.send(user, POSITIVE, "This portal teleports to {vector} in {name#world}",
                          config.destination.position.toInt(), config.destination.world.getName());
                break;
            }

        }
    }

    public ServerWorld getWorld()
    {
        return this.config.world.getWorld();
    }

    public AABB getAABB()
    {
        return this.config.getAABB();
    }
}
