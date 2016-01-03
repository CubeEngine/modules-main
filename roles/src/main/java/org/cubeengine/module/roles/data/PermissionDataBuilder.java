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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.util.persistence.InvalidDataException;

import static org.cubeengine.module.roles.data.IPermissionData.*;

public class PermissionDataBuilder implements DataManipulatorBuilder<PermissionData, ImmutablePermissionData>
{
    @Override
    public PermissionData create()
    {
        return new PermissionData(new ArrayList<>(), new HashMap<>(), new HashMap<>());
    }

    @Override
    public Optional<PermissionData> createFrom(DataHolder dataHolder)
    {
        return create().fill(dataHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<PermissionData> build(DataView container) throws InvalidDataException
    {
        Optional<List<String>> parents = container.getStringList(PARENTS.getQuery());
        Optional<Map<String, Boolean>> permissions = ((Optional<Map<String, Boolean>>) container.getMap(PERMISSIONS.getQuery()));
        Optional<Map<String, String>> options = ((Optional<Map<String, String>>) container.getMap(OPTIONS.getQuery()));

        permissions = replaceKeys(permissions, ":", ".");
        options = replaceKeys(options, ":", ".");

        if (parents.isPresent() || permissions.isPresent() || options.isPresent())
        {
            return Optional.of(new PermissionData(parents.orElse(null), permissions.orElse(null), options.orElse(null)));
        }
        return Optional.empty();
    }
}
