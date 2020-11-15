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

import java.util.UUID;
import io.leangen.geantyref.TypeToken;
import org.cubeengine.module.locker.PluginLocker;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.DataHolder.Mutable;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.ListValue;
import org.spongepowered.api.data.value.MapValue;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.TypeTokens;

public interface LockerData
{
    TypeToken<MapValue<UUID, Integer>> TTMV_UUIDInteger = new TypeToken<MapValue<UUID, Integer>>() {};
    TypeToken<ListValue<UUID>> TTLV_UUID = new TypeToken<ListValue<UUID>>() {};

    Key<Value<String>> MODE = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "mode")).type(TypeTokens.STRING_VALUE_TOKEN).build();

    Key<Value<String>> PASS = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "pass")).type(TypeTokens.STRING_VALUE_TOKEN).build();

    Key<Value<UUID>> OWNER = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "owner")).type(TypeTokens.UUID_VALUE_TOKEN).build();

    /**
     * Bitmasks see {@link ProtectionFlag}
     */
    Key<Value<Integer>> FLAGS = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "flags")).type(TypeTokens.INTEGER_VALUE_TOKEN).build();
    Key<MapValue<UUID, Integer>> ACCESS = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "access")).type(TTMV_UUIDInteger).build();

    Key<ListValue<UUID>> UNLOCKS = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "unlocks")).type(TTLV_UUID).build();

    Key<Value<Long>> LAST_ACCESS = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "last_access")).type(TypeTokens.LONG_VALUE_TOKEN).build();
    Key<Value<Long>> CREATED = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "created")).type(TypeTokens.LONG_VALUE_TOKEN).build();

    static void register(RegisterCatalogEvent<DataRegistration> event)
    {
        event.register(DataRegistration.of(MODE, ItemStack.class));

        event.register(DataRegistration.of(PASS, ItemStack.class, BlockEntity.class, Entity.class));

        event.register(DataRegistration.of(OWNER, ItemStack.class, BlockEntity.class, Entity.class));

        event.register(DataRegistration.of(FLAGS, ItemStack.class, BlockEntity.class, Entity.class));

        final DataStore accessDatastore = DataStore.builder().pluginData(ACCESS.getKey()).holder(ItemStack.class, BlockEntity.class, Entity.class).key(ACCESS).build();
        DataRegistration.builder().key(ACCESS.getKey()).dataKey(ACCESS).store(accessDatastore).build();

        event.register(DataRegistration.of(LAST_ACCESS, BlockEntity.class, Entity.class));
        event.register(DataRegistration.of(CREATED, BlockEntity.class, Entity.class));
    }

    static void purge(Mutable dataHolder)
    {
        dataHolder.remove(PASS);
        dataHolder.remove(OWNER);
        dataHolder.remove(FLAGS);
        dataHolder.remove(ACCESS);
        dataHolder.remove(UNLOCKS);
        dataHolder.remove(LAST_ACCESS);
        dataHolder.remove(CREATED);
    }
}
