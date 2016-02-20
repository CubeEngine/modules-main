package de.cubeisland.engine.module.netherportals;

import java.util.HashMap;
import java.util.Map;
import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;
import org.cubeengine.service.world.ConfigWorld;
import org.spongepowered.api.Sponge;

@SuppressWarnings("all")
public class NetherportalsConfig extends ReflectedYaml
{
    @Comment("Portal behaviour setting per world")
    public Map<ConfigWorld, WorldSection> worldSettings = new HashMap<>();

    public static class WorldSection implements Section
    {
        @Comment("If enabled vanilla behaviour will be replaced")
        public boolean enablePortalRouting = false;

        @Comment("Sets the target of netherportals to this world")
        public ConfigWorld netherTarget;
        @Comment("Sets the scale of netherportals to the other world\n"
            + "For default vanilla scale leave this value empty")
        public Integer netherTargetScale;

        @Comment("Sets the target of enderportals to this world")
        public ConfigWorld endTarget;
    }

    @Override
    public void onLoad()
    {
        worldSettings.put(new ConfigWorld(Sponge.getServer().getDefaultWorldName()), new WorldSection());
    }
}
