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
package org.cubeengine.module.locker.storage;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.monster.Monster;

import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.entity.EntityTypes.*;

public enum ProtectedType
{
    CONTAINER(1, ProtectionFlag.HOPPER_IN, ProtectionFlag.HOPPER_MINECART_IN, ProtectionFlag.HOPPER_MINECART_OUT, ProtectionFlag.HOPPER_OUT),
    DOOR(2, ProtectionFlag.BLOCK_REDSTONE, ProtectionFlag.AUTOCLOSE),
    BLOCK(3, ProtectionFlag.BLOCK_REDSTONE),
    ENTITY_CONTAINER(4, ProtectionFlag.HOPPER_IN, ProtectionFlag.HOPPER_MINECART_IN, ProtectionFlag.HOPPER_MINECART_OUT, ProtectionFlag.HOPPER_OUT),
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
        if (material.equals(CHEST) || material.equals(TRAPPED_CHEST) || material.equals(DISPENSER) || material.equals(
            DROPPER) || material.equals(FURNACE) || material.equals(LIT_FURNACE) || material.equals(BREWING_STAND)
            || material.equals(BEACON) || material.equals(HOPPER))
        {
            return CONTAINER;
        }
        else if (material.equals(WOODEN_DOOR) || material.equals(SPRUCE_DOOR) || material.equals(BIRCH_DOOR)
            || material.equals(JUNGLE_DOOR) || material.equals(ACACIA_DOOR) || material.equals(DARK_OAK_DOOR)
            || material.equals(IRON_DOOR) || material.equals(FENCE_GATE) || material.equals(TRAPDOOR))
        {
            return DOOR;
        }
        return BLOCK;
    }

    public static ProtectedType getProtectedType(EntityType type)
    {
        if (type.equals(CHESTED_MINECART) || type.equals(HOPPER_MINECART))
        {
            return ENTITY_CONTAINER;
        }
        else if (type.equals(HORSE))
        {
            return ENTITY_CONTAINER_LIVING;
        }
        else if (type.equals(LEASH_HITCH) || type.equals(PAINTING) || type.equals(ITEM_FRAME) || type.equals(
            FURNACE_MINECART) || type.equals(TNT_MINECART) || type.equals(MOB_SPAWNER_MINECART))
        {
            return ENTITY;
        }
        else if (type.equals(BOAT) || type.equals(RIDEABLE_MINECART))
        {
            return ENTITY_VEHICLE;
        }
        else
        {
            if (!Monster.class.isAssignableFrom(type.getEntityClass())
            && Living.class.isAssignableFrom(type.getEntityClass()))
            {
                return ENTITY_LIVING;
            }
            throw new IllegalArgumentException(type.getName() + " is not allowed!");
        }
    }
}
