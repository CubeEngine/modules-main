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
package org.cubeengine.module.zoned;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.math.vector.Vector3d;

public interface ZonedData
{
    Key<Value<String>> ZONE_TYPE = Key.builder().key(ResourceKey.of(PluginZoned.ZONED_ID, "zone-type")).elementType(String.class).build();

    Key<Value<ResourceKey>> ZONE_WORLD = Key.builder().key(ResourceKey.of(PluginZoned.ZONED_ID, "zone-world")).elementType(ResourceKey.class).build();
    Key<Value<Vector3d>> ZONE_MIN = Key.builder().key(ResourceKey.of(PluginZoned.ZONED_ID, "zone-min")).elementType(Vector3d.class).build();
    Key<Value<Vector3d>> ZONE_MAX = Key.builder().key(ResourceKey.of(PluginZoned.ZONED_ID, "zone-max")).elementType(Vector3d.class).build();


    static void register(RegisterDataEvent event)
    {
        {
            final ResourceKey regKey = ResourceKey.of(PluginZoned.ZONED_ID, "zone-type");
            final DataStore dataStore = DataStore.builder().pluginData(regKey).holder(ItemStack.class).keys(ZonedData.ZONE_TYPE).build();
            final DataRegistration registration = DataRegistration.builder()
                                                                  .dataKey(ZonedData.ZONE_TYPE)
                                                                  .store(dataStore)
                                                                  .build();
            event.register(registration);
        }
        {
            final DataStore dataStore = DataStore.builder().pluginData(ResourceKey.of(PluginZoned.ZONED_ID, "saved-zone-type")).holder(ItemStack.class).keys(ZONE_WORLD, ZONE_MAX, ZONE_MIN).build();
            final DataRegistration registration = DataRegistration.builder()
                                                                   .dataKey(ZONE_WORLD, ZONE_MAX, ZONE_MIN)
                                                                   .store(dataStore)
                                                                   .build();
            event.register(registration);
        }

    }

    static boolean isTool(ItemStack stack)
    {
        return stack.get(ZONE_TYPE).isPresent();
    }

    static boolean isSavedSelection(ItemStack stack)
    {
        return stack.get(ZONE_WORLD).isPresent();
    }
}
