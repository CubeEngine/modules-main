package org.cubeengine.module.protector.region;

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.module.protector.listener.MoveListener;
import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.Map;

public class RegionConfig extends ReflectedYaml
{
    public String name;

    public Vector3i corner1;
    public Vector3i corner2;
    public World world;

    public int priority = 0;

    public Settings settings;

    public static class Settings implements Section
    {
        public Map<MoveListener.MoveType, Tristate> move;
    }
}
