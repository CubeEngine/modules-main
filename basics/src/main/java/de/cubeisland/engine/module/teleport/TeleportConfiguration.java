package de.cubeisland.engine.module.teleport;

import de.cubeisland.engine.module.service.world.ConfigWorld;
import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;

public class TeleportConfiguration extends ReflectedYaml
{


    @Comment({"The world to teleport to when using /spawn",
              "Use {} if you want to use the spawn of the world the player is in."})
    public ConfigWorld mainWorld;

    @Comment({"The seconds until a teleport request is automatically denied.",
              "Use -1 to never automatically deny. (Will lose information after some time when disconnecting)"})
    public int teleportRequestWait = -1;



    public NavigationSection navigation;

    public class NavigationSection implements Section
    {
        public ThruSection thru;

        public class ThruSection implements Section
        {
            public int maxRange = 15;

            public int maxWallThickness = 15;
        }

        @Name("jumpto.max-range")
        public int jumpToMaxRange = 300;
    }
}
