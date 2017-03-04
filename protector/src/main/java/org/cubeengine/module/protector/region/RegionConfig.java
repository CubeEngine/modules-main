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

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.module.protector.listener.MoveListener;
import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;

public class RegionConfig extends ReflectedYaml
{
    public String name;

    public Vector3i corner1;
    public Vector3i corner2;
    public ConfigWorld world;

    public int priority = 0;

    public Settings settings;

    public static class Settings implements Section
    {
        public Map<MoveListener.MoveType, Tristate> move = new HashMap<>();
    }
}
