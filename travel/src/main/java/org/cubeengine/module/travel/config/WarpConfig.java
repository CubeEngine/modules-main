package org.cubeengine.module.travel.config;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;

public class WarpConfig extends ReflectedYaml
{
    public List<Warp> warps = new ArrayList<>();
}
