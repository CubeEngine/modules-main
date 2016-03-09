package org.cubeengine.module.travel.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;

public class HomeConfig extends ReflectedYaml
{
    public List<Home> homes = new ArrayList<>();
    public Map<UUID, List<UUID>> globalInvites = new HashMap<>();
}
