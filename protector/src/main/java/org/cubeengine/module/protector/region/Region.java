package org.cubeengine.module.protector.region;

import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Region
{
    private Cuboid cuboid;
    private Location<World> origin;
    private RegionConfig config;

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
}
