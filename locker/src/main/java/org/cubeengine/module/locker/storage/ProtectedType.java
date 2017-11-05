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
package org.cubeengine.module.locker.storage;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.monster.Monster;

import static org.cubeengine.module.locker.storage.ProtectionFlag.*;
import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.entity.EntityTypes.*;

public enum ProtectedType
{
    CONTAINER(1, HOPPER_IN, HOPPER_MINECART_IN, HOPPER_MINECART_OUT, HOPPER_OUT, BLOCK_REDSTONE),
    DOOR(2, BLOCK_REDSTONE, AUTOCLOSE),
    BLOCK(3, BLOCK_REDSTONE),
    ENTITY_CONTAINER(4, HOPPER_IN, HOPPER_MINECART_IN, HOPPER_MINECART_OUT, HOPPER_OUT),
    ENTITY_LIVING(5),
    ENTITY_VEHICLE(6),
    ENTITY(7),
    ENTITY_CONTAINER_LIVING(8),
    ;

    private final static Map<Byte, ProtectedType> protectedTypes = new HashMap<>();
    private final static Map<BlockType, ProtectedType> blocks = new HashMap<>();
    private final static Map<EntityType, ProtectedType> entities = new HashMap<>();

    public final byte id;
    public final Collection<ProtectionFlag> supportedFlags;

    static
    {
        for (ProtectedType protectedType : ProtectedType.values())
        {
            protectedTypes.put(protectedType.id, protectedType);
        }

        blocks.put(CHEST, CONTAINER);
        blocks.put(TRAPPED_CHEST, CONTAINER);
        blocks.put(DISPENSER, CONTAINER);
        blocks.put(DROPPER, CONTAINER);
        blocks.put(FURNACE, CONTAINER);
        blocks.put(LIT_FURNACE, CONTAINER);
        blocks.put(BREWING_STAND, CONTAINER);
        blocks.put(DROPPER, CONTAINER);
        blocks.put(BEACON, CONTAINER);
        blocks.put(HOPPER, CONTAINER);

        blocks.put(WOODEN_DOOR, DOOR);
        blocks.put(SPRUCE_DOOR, DOOR);
        blocks.put(BIRCH_DOOR, DOOR);
        blocks.put(JUNGLE_DOOR, DOOR);
        blocks.put(ACACIA_DOOR, DOOR);
        blocks.put(DARK_OAK_DOOR, DOOR);
        blocks.put(IRON_DOOR, DOOR);
        blocks.put(FENCE_GATE, DOOR);
        blocks.put(TRAPDOOR, DOOR);
        blocks.put(ACACIA_FENCE_GATE, DOOR);
        blocks.put(BIRCH_FENCE_GATE, DOOR);
        blocks.put(DARK_OAK_FENCE_GATE, DOOR);
        blocks.put(JUNGLE_FENCE_GATE, DOOR);
        blocks.put(SPRUCE_FENCE_GATE, DOOR);

        entities.put(CHESTED_MINECART, ENTITY_CONTAINER);
        entities.put(HOPPER_MINECART, ENTITY_CONTAINER);
        entities.put(HORSE, ENTITY_CONTAINER_LIVING);
        entities.put(LEASH_HITCH, ENTITY);
        entities.put(PAINTING, ENTITY);
        entities.put(ITEM_FRAME, ENTITY);
        entities.put(FURNACE_MINECART, ENTITY);
        entities.put(TNT_MINECART, ENTITY);
        entities.put(MOB_SPAWNER_MINECART, ENTITY);
        entities.put(BOAT, ENTITY_VEHICLE);
        entities.put(RIDEABLE_MINECART, ENTITY_VEHICLE);
    }

    ProtectedType(int id, ProtectionFlag... supportedFlags)
    {
        this.supportedFlags = Arrays.asList(supportedFlags);
        this.id = (byte)id;
    }

    public static ProtectedType forByte(Byte protectedType)
    {
        return protectedTypes.get(protectedType);
    }

    public static ProtectedType getProtectedType(BlockType material)
    {
        return blocks.getOrDefault(material, BLOCK);
    }

    public static ProtectedType getProtectedType(EntityType type)
    {
        ProtectedType pType = entities.get(type);
        if (pType != null)
        {
            return pType;
        }
        if (!Monster.class.isAssignableFrom(type.getEntityClass())
                && Living.class.isAssignableFrom(type.getEntityClass()))
        {
            return ENTITY_LIVING;
        }
        throw new IllegalArgumentException(type.getName() + " is not allowed!");
    }
}
