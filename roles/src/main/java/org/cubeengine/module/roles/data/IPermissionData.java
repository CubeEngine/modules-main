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
package org.cubeengine.module.roles.data;

import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.MapValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

public interface IPermissionData
{
    Key<ListValue<String>> PARENTS = KeyFactory.makeListKey(String.class, DataQuery.of("parents"));
    Key<MapValue<String, Boolean>> PERMISSIONS = KeyFactory.makeMapKey(String.class, Boolean.class, DataQuery.of("permissions"));
    Key<MapValue<String, String>> OPTIONS = KeyFactory.makeMapKey(String.class, String.class, DataQuery.of("options"));

    List<String> getParents();
    Map<String, Boolean> getPermissions();
    Map<String, String> getOptions();

    static <E extends IPermissionData> int compareTo(E o1, E o2)
    {
        int compare = Integer.compare(o1.getParents().size(), o2.getParents().size());
        if (compare != 0)
        {
            return compare;
        }
        for (int i = 0; i < o1.getParents().size(); i++)
        {
            compare = o1.getParents().get(i).compareTo(o2.getParents().get(i));
            if (compare != 0)
            {
                return compare;
            }
        }

        compare = Integer.compare(o1.getPermissions().size(), o2.getPermissions().size());
        if (compare != 0)
        {
            return compare;
        }

        compare = Boolean.compare(o1.getPermissions().entrySet().containsAll(o2.getPermissions().entrySet()),
                o2.getPermissions().entrySet().containsAll(o1.getPermissions().entrySet()));
        if (compare != 0)
        {
            return compare;
        }

        compare = Integer.compare(o1.getOptions().size(), o2.getOptions().size());
        if (compare != 0)
        {
            return compare;
        }

        compare = Boolean.compare(o1.getOptions().entrySet().containsAll(o2.getOptions().entrySet()),
                o2.getOptions().entrySet().containsAll(o1.getOptions().entrySet()));
        if (compare != 0)
        {
            return compare;
        }

        return 0;
    }

    static <T> Optional<Map<String, T>> replaceKeys(Optional<Map<String, T>> opt, String orig, String repl)
    {
        return opt.map(m -> replaceKeys(m, orig, repl));
    }

    static <T> Map<String, T> replaceKeys(Map<String, T> map, String orig, String repl)
    {
        return map.entrySet().stream().collect(toMap(e -> e.getKey().replace(orig, repl), Map.Entry::getValue));
    }
}
