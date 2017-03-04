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
package org.cubeengine.module.protector.region;

import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Region
{
    private Cuboid cuboid;
    private Location<World> origin;
    private RegionConfig config;
    private Context context;

    public Region(Location<World> origin, Cuboid cuboid)
    {
        this.origin = origin;
        this.cuboid = cuboid;
    }

    public Region(RegionConfig config)
    {
        this.config = config;
        this.origin = new Location<>(config.world, config.corner1);
        this.cuboid = new Cuboid(config.corner1.toDouble(), config.corner2.min(config.corner1).toDouble());
    }

    public boolean contains(Location<World> loc)
    {
        return loc.getExtent().equals(this.origin.getExtent()) && this.cuboid.contains(loc.getPosition());
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
    }

    public String getName()
    {
        return config.name;
    }

    public Context getContext()
    {
        if (this.context == null)
        {
            this.context = new Context("region", this.config.world.getName() + "." + this.config.name);
        }
        return this.context;
    }
}
