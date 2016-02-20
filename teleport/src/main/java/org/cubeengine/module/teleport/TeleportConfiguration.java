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
package org.cubeengine.module.teleport;

import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;
import org.cubeengine.service.world.ConfigWorld;
import org.spongepowered.api.world.World;

public class TeleportConfiguration extends ReflectedYaml
{
    @Comment({"The world to teleport to when using /spawn",
              "Use {} if you want to use the spawn of the world the player is in."})
    public ConfigWorld mainWorld;

    @Comment({"The seconds until a teleport request is automatically denied.",
              "Use -1 to never automatically deny. (Will lose information after some time when disconnecting)"})
    public int teleportRequestWait = -1;

    public NavigationSection navigation;

    public World getMainWorld()
    {
        return mainWorld == null ? null : mainWorld.getWorld();
    }

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
