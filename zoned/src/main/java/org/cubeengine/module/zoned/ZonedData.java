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
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.util.TypeTokens;

public interface ZonedData {

    Key<Value<String>> ZONE_TYPE = Key.builder().key(ResourceKey.of(PluginZoned.ZONED_ID, "zone-type"))
            .type(TypeTokens.STRING_VALUE_TOKEN).build();

    static void register(RegisterCatalogEvent<DataRegistration> event) {
        final DataStore dataStore = DataStore.builder()
                .key(ZonedData.ZONE_TYPE, "zone-type")
                .holder(TypeTokens.ITEM_STACK_TOKEN)
                .build();

        final DataRegistration registration = DataRegistration.builder()
                .key(ZonedData.ZONE_TYPE)
                .store(dataStore)
                .key(ResourceKey.of(PluginZoned.ZONED_ID, "zone-type"))
                .build();
        event.register(registration);
    }
}
