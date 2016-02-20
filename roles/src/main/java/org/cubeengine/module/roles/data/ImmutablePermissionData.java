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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableListValue;
import org.spongepowered.api.data.value.immutable.ImmutableMapValue;

public class ImmutablePermissionData extends AbstractImmutableData<ImmutablePermissionData, PermissionData> implements IPermissionData
{
    private List<String> parents;
    private Map<String, Boolean> permissions;
    private Map<String, String> options;

    public ImmutablePermissionData(List<String> parents, Map<String, Boolean> permissions, Map<String, String> options)
    {
        this.parents = parents;
        this.permissions = permissions;
        this.options = options;
        registerGetters();
    }

    private ImmutableMapValue<String, String> options()
    {
        return Sponge.getRegistry().getValueFactory().createMapValue(OPTIONS, options).asImmutable();
    }

    public Map<String, String> getOptions()
    {
        return options;
    }

    private ImmutableMapValue<String, Boolean> permissions()
    {
        return Sponge.getRegistry().getValueFactory().createMapValue(PERMISSIONS, permissions).asImmutable();
    }

    public Map<String, Boolean> getPermissions()
    {
        return permissions;
    }

    private ImmutableListValue<String> parents()
    {
        return Sponge.getRegistry().getValueFactory().createListValue(PARENTS, parents).asImmutable();
    }

    public List<String> getParents()
    {
        return parents;
    }

    @Override
    protected void registerGetters()
    {
        registerFieldGetter(PARENTS, ImmutablePermissionData.this::getParents);
        registerKeyValue(PARENTS, ImmutablePermissionData.this::parents);

        registerFieldGetter(PERMISSIONS, ImmutablePermissionData.this::getPermissions);
        registerKeyValue(PERMISSIONS, ImmutablePermissionData.this::permissions);

        registerFieldGetter(OPTIONS, ImmutablePermissionData.this::getOptions);
        registerKeyValue(OPTIONS, ImmutablePermissionData.this::options);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Optional<ImmutablePermissionData> with(Key<? extends BaseValue<E>> key, E value)
    {
        ImmutablePermissionData data = null;
        if (PARENTS.equals(key))
        {
            data = new ImmutablePermissionData((List<String>)value, permissions, options);
        }
        else if (PERMISSIONS.equals(key))
        {
            data = new ImmutablePermissionData(parents, (Map<String, Boolean>)value, options);
        }
        else if (OPTIONS.equals(key))
        {
            data = new ImmutablePermissionData(parents, permissions, (Map<String, String>)value);
        }
        return Optional.ofNullable(data);
    }

    @Override
    public PermissionData asMutable()
    {
        return new PermissionData(parents, permissions, options);
    }

    @Override
    public int compareTo(ImmutablePermissionData o)
    {
        return IPermissionData.compareTo(this, o);
    }

    @Override
    public DataContainer toContainer()
    {
        DataContainer result = super.toContainer();
        if (parents != null)
        {
            result.set(PARENTS, parents);
        }
        if (permissions != null)
        {
            result.set(PERMISSIONS, IPermissionData.replaceKeys(permissions, ".", ":"));
        }
        if (options != null)
        {
            result.set(OPTIONS, IPermissionData.replaceKeys(options, ".", ":"));
        }
        return result;
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }
}
