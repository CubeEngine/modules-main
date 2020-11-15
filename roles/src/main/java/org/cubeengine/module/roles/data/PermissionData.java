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
package org.cubeengine.module.roles.data;

import io.leangen.geantyref.TypeToken;
import org.cubeengine.module.roles.PluginRoles;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.ListValue;
import org.spongepowered.api.data.value.MapValue;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.util.TypeTokens;

public interface PermissionData
{

    TypeToken<ListValue<String>> TTLV_String = new TypeToken<ListValue<String>>() {};
    TypeToken<MapValue<String, Boolean>> TTMV_StringBool = new TypeToken<MapValue<String, Boolean>>() {};
    TypeToken<MapValue<String, String>> TTMV_StringString = new TypeToken<MapValue<String, String>>() {};

    Key<ListValue<String>> PARENTS = Key.builder().key(ResourceKey.of(PluginRoles.ROLES_ID, "parents")).type(TTLV_String).build();
    Key<MapValue<String, Boolean>> PERMISSIONS = Key.builder().key(ResourceKey.of(PluginRoles.ROLES_ID, "permissions")).type(TTMV_StringBool).build();
    Key<MapValue<String, String>> OPTIONS = Key.builder().key(ResourceKey.of(PluginRoles.ROLES_ID, "options")).type(TTMV_StringString).build();

    static void register(RegisterCatalogEvent<DataRegistration> event)
    {
        final ResourceKey rkey = ResourceKey.of(PluginRoles.ROLES_ID, "permissiondata");
        final DataStore dataStore = DataStore.builder()
                                             .pluginData(rkey)
                                             .holder(TypeTokens.SERVER_PLAYER_TOKEN, TypeTokens.USER_TOKEN)

                      .key(PARENTS, "parents")
                                             .key(PERMISSIONS, "permissions")
                                             .key(OPTIONS, "options")
                                             .build();

        final DataRegistration registration = DataRegistration.builder()
                                                              .dataKey(PARENTS)
                                                              .dataKey(PERMISSIONS)
                                                              .dataKey(OPTIONS)
                                                              .store(dataStore)
                                                              .key(rkey)
                                                              .build();
        event.register(registration);
    }

}
