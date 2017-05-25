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
package org.cubeengine.module.protector.region;

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.module.protector.listener.SettingsListener;
import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.util.Tristate;

import java.util.HashMap;
import java.util.Map;

public class RegionConfig extends ReflectedYaml
{
    public String name;

    public Vector3i corner1;
    public Vector3i corner2;
    public ConfigWorld world;

    public int priority = 0;

    public Settings settings = new Settings();

    public static class Settings implements Section
    {

        public Map<SettingsListener.MoveType, Tristate> move = new HashMap<>();
        public Tristate build = Tristate.UNDEFINED;
        public Use use = new Use();
        public static class Use implements Section
        {
            public Map<BlockType, Tristate> block = new HashMap<>();

            public Map<ItemType, Tristate> item = new HashMap<>();

            public All all = new All();

            public static class All implements Section
            {
                public Tristate block = Tristate.UNDEFINED;
                public Tristate item = Tristate.UNDEFINED;
                public Tristate container = Tristate.UNDEFINED;
                public Tristate open = Tristate.UNDEFINED;
                public Tristate redstone = Tristate.UNDEFINED;
            }
        }
        public Spawn spawn = new Spawn();
        public static class Spawn implements Section
        {

            public Map<EntityType, Tristate> naturally = new HashMap<>();
            public Map<EntityType, Tristate> player = new HashMap<>();
            public Map<EntityType, Tristate> plugin = new HashMap<>();
        }
        public BlockDamage blockDamage = new BlockDamage();

        public static class BlockDamage implements Section
        {
            public Tristate allExplosion = Tristate.UNDEFINED;
            public Tristate playerExplosion = Tristate.UNDEFINED;
            public Map<BlockType, Tristate> block = new HashMap<>();
            public Tristate monster = Tristate.UNDEFINED;
        }

        public Map<String, Tristate> blockedCommands = new HashMap<>();
        public Tristate deadCircuit = Tristate.UNDEFINED;

        public EntityDamage entityDamage = new EntityDamage();

        public static class EntityDamage implements Section
        {
            public Tristate all = Tristate.UNDEFINED;
            public Tristate pvp = Tristate.UNDEFINED;
            public Tristate byLiving = Tristate.UNDEFINED;
            public Map<EntityType, Tristate> byEntity = new HashMap<>();
        }
    }

    public static <T> void setOrUnset(Map<T, Tristate> map, T key, Tristate set)
    {
        if (set == Tristate.UNDEFINED)
        {
            map.remove(key);
        }
        else
        {
            map.put(key, set);
        }
    }
}
