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

import static java.util.stream.Collectors.toMap;
import static org.spongepowered.api.data.DataQuery.of;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.MapValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IPermissionData
{

    TypeToken<ListValue<String>> TTLV_String = new TypeToken<ListValue<String>>() {};
    TypeToken<MapValue<String, Boolean>> TTMV_StringBool = new TypeToken<MapValue<String, Boolean>>() {};
    TypeToken<MapValue<String, String>> TTMV_StringString = new TypeToken<MapValue<String, String>>() {};

    Key<ListValue<String>> PARENTS = Key.builder().type(TTLV_String).query(of("parents")).id("permission-parent").name("Parents").build();
    Key<MapValue<String, Boolean>> PERMISSIONS = Key.builder().type(TTMV_StringBool).query(of("permissions")).id("permission-perms").name("Permission").build();
    Key<MapValue<String, String>> OPTIONS = Key.builder().type(TTMV_StringString).query(of("options")).id("permission-opts").name("Options").build();

    List<String> getParents();
    Map<String, Boolean> getPermissions();
    Map<String, String> getOptions();

    static <T> Optional<Map<String, T>> replaceKeys(Optional<Map<String, T>> opt, String orig, String repl)
    {
        return opt.map(m -> replaceKeys(m, orig, repl));
    }

    static <T> Map<String, T> replaceKeys(Map<String, T> map, String orig, String repl)
    {
        return map.entrySet().stream().collect(toMap(e -> e.getKey().replace(orig, repl), Map.Entry::getValue));
    }
}
