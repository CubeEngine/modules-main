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

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.protector.RegionManager;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Region
{
    private Cuboid cuboid;
    private RegionConfig config;
    private Context context;
    private RegionManager manager;

    public Region(RegionConfig config, RegionManager manager)
    {
        this.config = config;
        this.manager = manager;
        if (config.corner1 != null && config.corner2 != null)
        {
            this.cuboid = new Cuboid(config.corner1.toDouble(), config.corner2.sub(config.corner1).toDouble());
        }
    }

    public boolean contains(Location<World> loc)
    {
        Vector3d pos = loc.getPosition();
        return (this.config.world == null || loc.getExtent().equals(this.config.world.getWorld()))
                && (this.cuboid == null || (this.cuboid.contains(pos.toInt().toDouble())
                 || this.cuboid.contains(pos.add(0,1,0).toInt().toDouble())
                 || this.cuboid.contains(pos.add(0,1.8,0).toInt().toDouble())));
    }

    public RegionConfig.Settings getSettings()
    {
        return this.config.settings;
    }

    public Cuboid getCuboid()
    {
        return this.cuboid;
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

    public World getWorld()
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

    public void setCuboid(Cuboid cuboid)
    {
        this.cuboid = cuboid;
        this.config.corner1 = cuboid.getMinimumPoint().toInt();
        this.config.corner2 = cuboid.getMaximumPoint().toInt();
    }
}
