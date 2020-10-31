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

import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.Transform;
import org.spongepowered.math.vector.Vector3i;

@SuppressWarnings("all")
public class PortalConfig extends ReflectedYaml
{
    @Comment("When true portal will attempt to find a safe spot nearby if the destination is not safe")
    public boolean safeTeleport = false;
    public boolean teleportNonPlayers = false;
    public String owner;
    public ConfigWorld world;
    public boolean particles;

    public PortalRegion location = new PortalRegion();

    public AABB getAABB()
    {
        return new AABB(location.from, location.to.add(Vector3i.ONE));
    }

    public class PortalRegion implements Section
    {
        public Vector3i from;
        public Vector3i to;

        @Comment("When linking another portal to this one a player will be teleported to this location")
        public Transform destination;
    }

    public Destination destination;
}
