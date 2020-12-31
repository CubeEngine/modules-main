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
package org.cubeengine.module.protector.region;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

public class Region
{

    private ZoneConfig zone;
    private RegionConfig config;
    private Context context;
    private RegionManager manager;

    public Region(@Nullable ZoneConfig zone, RegionConfig config, RegionManager manager)
    {
        this.zone = zone;
        this.config = config;
        this.manager = manager;
    }

    public RegionConfig getConfig() {
        return config;
    }

    public boolean contains(ServerLocation loc)
    {
        if (this.config.world == null)
        {
            return true; // global
        }
        if (this.config.name == null) // world
        {
            return loc.getWorld().equals(this.config.world.getWorld());
        }
        return this.contains(loc.getPosition()); // zone
    }

    public boolean contains(Vector3d pos)
    {
        return zone.shape.contains(pos) || zone.shape.contains(pos.add(0, 1.8, 0)) ;
    }

    public RegionConfig.Settings getSettings()
    {
        return this.config.settings;
    }

    public Cuboid getCuboid()
    {
        if (this.zone == null)
        {
            return null;
        }
        return this.zone.shape.getBoundingCuboid();
    }

    public int getPriority() {
        return config.priority;
    }

    public void save()
    {
        this.config.save();
        this.manager.markDirty();
    }

    public String getName()
    {
        return config.name;
    }

    public ServerWorld getWorld()
    {
        if (this.config.world == null)
        {
            return null;
        }
        return this.config.world.getWorld();
    }

    public String getWorldName()
    {
        if (this.config.world == null)
        {
            return null;
        }
        return this.config.world.getName();
    }

    public Context getContext()
    {
        if (this.context == null)
        {
            if (this.config.world == null)
            {
                return new Context("region", "global");
            }
            if (this.config.name == null)
            {
                return new Context("region", this.config.world.getName().toLowerCase());
            }
            this.context = new Context("region", this.config.world.getName().toLowerCase() + "." + this.config.name.toLowerCase());
        }
        return this.context;
    }

    @Override
    public String toString()
    {
        String prio = " (" + this.config.priority + ")";
        if (this.isGlobal()) {
            return "Global Region" + prio;
        }
        if (this.isWorldRegion()) {
            return "World Region: " + this.config.world.getName() + prio;
        }
        return "Region " + this.config.name + " in " + this.config.world.getName() + prio;
    }

    public void setPriority(int priority) {
        this.config.priority = priority;
    }

    public boolean isGlobal()
    {
        return this.config.world == null;
    }

    public boolean isWorldRegion()
    {
        return this.config.corner1 == null;
    }
}
