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
package org.cubeengine.module.locker.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.EntityType;

import static org.cubeengine.module.locker.data.ProtectionFlag.BLOCK_BREAK;
import static org.cubeengine.module.locker.data.ProtectionFlag.BLOCK_EXPLOSION;
import static org.cubeengine.module.locker.data.ProtectionFlag.BLOCK_INTERACT;
import static org.cubeengine.module.locker.data.ProtectionFlag.BLOCK_REDSTONE;
import static org.cubeengine.module.locker.data.ProtectionFlag.ENTITY_DAMAGE;
import static org.cubeengine.module.locker.data.ProtectionFlag.ENTITY_INTERACT;
import static org.cubeengine.module.locker.data.ProtectionFlag.INVENTORY_HOPPER_PUT;
import static org.cubeengine.module.locker.data.ProtectionFlag.INVENTORY_HOPPER_TAKE;
import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.entity.EntityTypes.*;

public enum ProtectedType
{
    CONTAINER(BLOCK_BREAK, BLOCK_EXPLOSION, BLOCK_INTERACT, BLOCK_REDSTONE, INVENTORY_HOPPER_PUT, INVENTORY_HOPPER_TAKE),
    BLOCK(BLOCK_BREAK, BLOCK_EXPLOSION, BLOCK_INTERACT, BLOCK_REDSTONE),
    ENTITY_CONTAINER(ENTITY_INTERACT, ENTITY_DAMAGE),
    ENTITY_LIVING(ENTITY_INTERACT, ENTITY_DAMAGE),
    ENTITY_VEHICLE(ENTITY_INTERACT, ENTITY_DAMAGE),
    ENTITY(ENTITY_INTERACT, ENTITY_DAMAGE),
    ENTITY_CONTAINER_LIVING(ENTITY_INTERACT, ENTITY_DAMAGE),
    ;

    private final static Map<BlockType, ProtectedType> blocks = new HashMap<>();
    private final static Map<EntityType, ProtectedType> entities = new HashMap<>();

    public final Collection<ProtectionFlag> supportedFlags;

    static
    {
        blocks.put(CHEST.get(), CONTAINER);
        blocks.put(TRAPPED_CHEST.get(), CONTAINER);
        blocks.put(DISPENSER.get(), CONTAINER);
        blocks.put(DROPPER.get(), CONTAINER);
        blocks.put(FURNACE.get(), CONTAINER);
        blocks.put(BREWING_STAND.get(), CONTAINER);
        blocks.put(DROPPER.get(), CONTAINER);
        blocks.put(BEACON.get(), CONTAINER);
        blocks.put(HOPPER.get(), CONTAINER);

        entities.put(CHEST_MINECART.get(), ENTITY_CONTAINER);
        entities.put(HOPPER_MINECART.get(), ENTITY_CONTAINER);
        entities.put(HORSE.get(), ENTITY_CONTAINER_LIVING);
        entities.put(LEASH_KNOT.get(), ENTITY);
        entities.put(PAINTING.get(), ENTITY);
        entities.put(ITEM_FRAME.get(), ENTITY);
        entities.put(FURNACE_MINECART.get(), ENTITY);
        entities.put(TNT_MINECART.get(), ENTITY);
        entities.put(SPAWNER_MINECART.get(), ENTITY);
        entities.put(BOAT.get(), ENTITY_VEHICLE);
        entities.put(MINECART.get(), ENTITY_VEHICLE);
    }

    ProtectedType(ProtectionFlag... supportedFlags)
    {
        this.supportedFlags = Arrays.asList(supportedFlags);
    }

    public static ProtectedType getProtectedType(BlockType material)
    {
        return blocks.getOrDefault(material, BLOCK);
    }

    public static ProtectedType getProtectedType(EntityType type)
    {
        return entities.getOrDefault(type, ENTITY);
    }
}
