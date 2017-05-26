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
package org.cubeengine.module.netherportals;

import java.util.HashMap;
import java.util.Map;
import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.cubeengine.libcube.service.config.ConfigWorld;
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
}
