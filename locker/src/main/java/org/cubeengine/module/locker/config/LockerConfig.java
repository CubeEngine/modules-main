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
package org.cubeengine.module.locker.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.annotations.Name;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.cubeengine.libcube.service.config.ConfigWorld;

import static org.cubeengine.module.locker.data.ProtectionFlag.*;
import static org.cubeengine.module.locker.data.ProtectionFlag.BLOCK_INTERACT;
import static org.cubeengine.module.locker.data.ProtectionFlag.BLOCK_REDSTONE;
import static org.cubeengine.module.locker.data.ProtectionFlag.ENTITY_INTERACT;
import static org.cubeengine.module.locker.data.ProtectionFlag.INVENTORY_HOPPER_PUT;
import static org.cubeengine.module.locker.data.ProtectionFlag.INVENTORY_HOPPER_TAKE;
import static org.spongepowered.api.block.BlockTypes.BARREL;
import static org.spongepowered.api.block.BlockTypes.BLAST_FURNACE;
import static org.spongepowered.api.block.BlockTypes.BREWING_STAND;
import static org.spongepowered.api.block.BlockTypes.CHEST;
import static org.spongepowered.api.block.BlockTypes.DISPENSER;
import static org.spongepowered.api.block.BlockTypes.DROPPER;
import static org.spongepowered.api.block.BlockTypes.FURNACE;
import static org.spongepowered.api.block.BlockTypes.HOPPER;
import static org.spongepowered.api.block.BlockTypes.OAK_SIGN;
import static org.spongepowered.api.block.BlockTypes.SMOKER;
import static org.spongepowered.api.block.BlockTypes.TRAPPED_CHEST;
import static org.spongepowered.api.entity.EntityTypes.CHEST_MINECART;
import static org.spongepowered.api.entity.EntityTypes.DONKEY;
import static org.spongepowered.api.entity.EntityTypes.HOPPER_MINECART;
import static org.spongepowered.api.entity.EntityTypes.HORSE;
import static org.spongepowered.api.entity.EntityTypes.ITEM_FRAME;
import static org.spongepowered.api.entity.EntityTypes.MINECART;
import static org.spongepowered.api.entity.EntityTypes.MULE;

@SuppressWarnings("all")
public class LockerConfig extends ReflectedYaml
{
    @Name("protections.block")
    public BlockEntitySection block = new BlockEntitySection();

    public static class BlockEntitySection implements Section
    {
        @Comment("Set this to false if you wish to disable BlockProtection completely")
        public boolean enable = true;

        @Comment("A List of all block-entities that can be protected with Locker\n" +
            "use the auto-protect option to automatically create a protection when placing the block\n" +
            "additionally you can set default flags which will also be automatically applied")
        public List<BlockLockConfig> blocks;
    }

    @Name("protections.entity")
    public EntitySection entity = new EntitySection();

    public static class EntitySection implements Section
    {
        @Comment("Set this to false if you wish to disable EntityProtection completely")
        public boolean enable = true;

        @Comment("A list of all entities that can be protected with Locker\n" +
            "auto-protect only applies onto entities that can be tamed")
        public List<EntityLockConfig> entities;
    }

    @Comment("Worlds to disable auto-protect in")
    public List<ConfigWorld> disableAutoProtect = new ArrayList<>();

    @Override
    public void onLoaded(File loadFrom)
    {
        if (block.enable)
        {
            if (block.blocks == null || block.blocks.isEmpty())
            {
                this.initBlocks();
            }
        }
        else
        {
            block.blocks = null;
        }
        if (entity.enable)
        {
            if (entity.entities == null || entity.entities.isEmpty())
            {
                this.initEntities();
            }
        }
        else
        {
            entity.entities = null;
        }
    }

    private void initEntities()
    {
        entity.entities = new ArrayList<>();
        entity.entities.add(new EntityLockConfig(HORSE.get()).autoProtect().defaultFlags(ENTITY_INTERACT));
        entity.entities.add(new EntityLockConfig(DONKEY.get()).autoProtect().defaultFlags(ENTITY_INTERACT));
        entity.entities.add(new EntityLockConfig(MULE.get()).autoProtect().defaultFlags(ENTITY_INTERACT));
        entity.entities.add(new EntityLockConfig(CHEST_MINECART.get()).defaultFlags(ENTITY_INTERACT, ENTITY_DAMAGE));
        entity.entities.add(new EntityLockConfig(HOPPER_MINECART.get()).defaultFlags(ENTITY_INTERACT, ENTITY_DAMAGE));
        entity.entities.add(new EntityLockConfig(MINECART.get()).defaultFlags(ENTITY_DAMAGE));
        entity.entities.add(new EntityLockConfig(ITEM_FRAME.get()).defaultFlags(ENTITY_INTERACT, ENTITY_DAMAGE));
    }

    private void initBlocks()
    {
        block.blocks = new ArrayList<>();
        block.blocks.add(new BlockLockConfig(CHEST.get()).autoProtect().defaultFlags(BLOCK_INTERACT, BLOCK_BREAK, BLOCK_EXPLOSION));
        block.blocks.add(new BlockLockConfig(TRAPPED_CHEST.get()).autoProtect().defaultFlags(BLOCK_INTERACT, BLOCK_BREAK, BLOCK_EXPLOSION));
        block.blocks.add(new BlockLockConfig(BARREL.get()).defaultFlags(BLOCK_INTERACT, BLOCK_BREAK, BLOCK_EXPLOSION));
        block.blocks.add(new BlockLockConfig(FURNACE.get()).defaultFlags(BLOCK_INTERACT, BLOCK_BREAK, BLOCK_EXPLOSION));
        block.blocks.add(new BlockLockConfig(SMOKER.get()).defaultFlags(BLOCK_INTERACT, BLOCK_BREAK, BLOCK_EXPLOSION));
        block.blocks.add(new BlockLockConfig(BLAST_FURNACE.get()).defaultFlags(BLOCK_INTERACT, BLOCK_BREAK, BLOCK_EXPLOSION));
        block.blocks.add(new BlockLockConfig(BREWING_STAND.get()).defaultFlags(BLOCK_INTERACT, BLOCK_BREAK, BLOCK_EXPLOSION));
        block.blocks.add(new BlockLockConfig(DISPENSER.get()).defaultFlags(BLOCK_INTERACT, BLOCK_REDSTONE, BLOCK_BREAK, BLOCK_EXPLOSION));
        block.blocks.add(new BlockLockConfig(DROPPER.get()).defaultFlags(BLOCK_INTERACT, BLOCK_REDSTONE, BLOCK_BREAK, BLOCK_EXPLOSION));
        block.blocks.add(new BlockLockConfig(HOPPER.get()).defaultFlags(BLOCK_INTERACT, INVENTORY_HOPPER_PUT, INVENTORY_HOPPER_TAKE, BLOCK_BREAK, BLOCK_EXPLOSION));
        block.blocks.add(new BlockLockConfig(OAK_SIGN.get()).defaultFlags(BLOCK_INTERACT, BLOCK_BREAK, BLOCK_EXPLOSION));
    }
}

