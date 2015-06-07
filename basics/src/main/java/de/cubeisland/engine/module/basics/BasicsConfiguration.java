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
package de.cubeisland.engine.module.basics;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import de.cubeisland.engine.module.service.world.ConfigWorld;
import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.item.ItemType;

import static org.spongepowered.api.block.BlockTypes.*;

@SuppressWarnings("all")
public class BasicsConfiguration extends ReflectedYaml
{
    public CommandsSection commands;

    public class CommandsSection implements Section
    {
        public int spawnmobLimit = 20;

        public int removeDefaultRadius = 20;

        public int butcherDefaultRadius = 20;

        public int nearDefaultRadius = 20;

        public Collection<BlockType> itemBlacklist = new LinkedList<BlockType>()
        {
            {
                this.add(BEDROCK);
                this.add(WATER);
                this.add(FLOWING_WATER);
                this.add(LAVA);
                this.add(FLOWING_LAVA);
                this.add(BED);
                this.add(PISTON_EXTENSION);
                this.add(PISTON_HEAD);
                this.add(REDSTONE_WIRE);
                this.add(WHEAT);
                this.add(STANDING_SIGN);
                this.add(WOODEN_DOOR);
                this.add(WALL_SIGN);
                this.add(IRON_DOOR);
                this.add(UNLIT_REDSTONE_TORCH);
                this.add(PORTAL);
                this.add(CAKE);
                this.add(POWERED_REPEATER);
                this.add(UNPOWERED_REPEATER);
                this.add(PUMPKIN_STEM);
                this.add(MELON_STEM);
                this.add(NETHER_WART);
                this.add(BREWING_STAND);
                this.add(CAULDRON);
                this.add(END_PORTAL);
                this.add(LIT_REDSTONE_LAMP);
                this.add(COCOA);
                this.add(TRIPWIRE);
                this.add(COMMAND_BLOCK);
                this.add(FLOWER_POT);
                this.add(CARROTS);
                this.add(POTATOES);
                this.add(SKULL);
            }
        };

        @Name("ban.disallow-if-offline-mode")
        public boolean disallowBanIfOfflineMode;

        @Name("door.max.radius")
        public int maxDoorRadius = 10;

        public boolean containsBlackListed(ItemType item)
        {
            for (BlockType blItem : itemBlacklist)
            {
                if (blItem.getHeldItem().isPresent())
                {
                    if (blItem.getHeldItem().equals(item))
                    {
                        return true;
                    }
                }
            }
            return false;
        }
    }



    @Name("changepainting.max.distance")
    public int maxChangePaintingDistance = 10;

    @Name("overstacked.prevent-anvil-and-brewing")
    public boolean preventOverstackedItems = true;

    @Override
    public void onLoaded(File loadedFrom)
    {
        for (Iterator<BlockType> it = this.commands.itemBlacklist.iterator(); it.hasNext(); )
        {
            if (it.next() == null)
            {
                it.remove();
            }
        }
    }
}
