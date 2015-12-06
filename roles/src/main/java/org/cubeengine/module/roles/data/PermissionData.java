/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 * <p>
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.roles.data;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.MapValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PermissionData extends AbstractData<PermissionData, ImmutablePermissionData> implements IPermissionData
{
    private List<String> parents;
    private Map<String, Boolean> permissions;
    private Map<String, String> options;

    public PermissionData(List<String> parents, Map<String, Boolean> permissions, Map<String, String> options)
    {
        this.parents = parents;
        this.permissions = permissions;
        this.options = options;
    }

    @Override
    protected void registerGettersAndSetters()
    {
        registerFieldGetter(PARENTS, PermissionData.this::getParents);
        registerFieldSetter(PARENTS, PermissionData.this::setParents);
        registerKeyValue(PARENTS, PermissionData.this::parents);

        registerFieldGetter(PERMISSIONS, PermissionData.this::getPermissions);
        registerFieldSetter(PERMISSIONS, PermissionData.this::setPermissions);
        registerKeyValue(PERMISSIONS, PermissionData.this::permissions);

        registerFieldGetter(OPTIONS, PermissionData.this::getOptions);
        registerFieldSetter(OPTIONS, PermissionData.this::setOptions);
        registerKeyValue(OPTIONS, PermissionData.this::options);
    }

    private MapValue<String, String> options()
    {
        return Sponge.getRegistry().getValueFactory().createMapValue(OPTIONS, options);
    }

    public void setOptions(Map<String, String> options)
    {
        this.options = options;
    }

    public Map<String, String> getOptions()
    {
        return options;
    }

    private MapValue<String, Boolean> permissions()
    {
        return Sponge.getRegistry().getValueFactory().createMapValue(PERMISSIONS, permissions);
    }

    public void setPermissions(Map<String, Boolean> permissions)
    {
        this.permissions = permissions;
    }

    public Map<String, Boolean> getPermissions()
    {
        return permissions;
    }

    private ListValue<String> parents()
    {
        return Sponge.getRegistry().getValueFactory().createListValue(PARENTS, parents);
    }

    public void setParents(List<String> parents)
    {
        this.parents = parents;
    }

    public List<String> getParents()
    {
        return parents;
    }

    @Override
    public Optional<PermissionData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        Optional<List<String>> parents = dataHolder.get(PARENTS);
        Optional<Map<String, Boolean>> permissions = dataHolder.get(PERMISSIONS);
        Optional<Map<String, String>> options = dataHolder.get(OPTIONS);

        if (parents.isPresent() || permissions.isPresent() || options.isPresent())
        {
            PermissionData data = new PermissionData(
                    parents.orElse(null),
                    permissions.orElse(null),
                    options.orElse(null));
            overlap.merge(this, data);
            if (data != this)
            {
                this.parents = data.parents;
                this.permissions = data.permissions;
                this.options = data.options;
            }
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<PermissionData> from(DataContainer container)
    {
        Optional<List<String>> parents = container.getStringList(PARENTS.getQuery());
        Optional<Map<String, Boolean>> permissions = ((Optional<Map<String, Boolean>>) container.getMap(PERMISSIONS.getQuery()));
        Optional<Map<String, String>> options = ((Optional<Map<String, String>>) container.getMap(OPTIONS.getQuery()));

        if (parents.isPresent() || permissions.isPresent() || options.isPresent())
        {
            this.parents = parents.orElse(null);
            this.permissions = permissions.orElse(null);
            this.options = options.orElse(null);
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public PermissionData copy()
    {
        return new PermissionData(parents, permissions, options);
    }

    @Override
    public ImmutablePermissionData asImmutable()
    {
        return new ImmutablePermissionData(parents, permissions, options);
    }

    @Override
    public int compareTo(PermissionData o)
    {
        return IPermissionData.compareTo(this, o);
    }

    @Override
    public DataContainer toContainer()
    {
        return new MemoryDataContainer().set(PARENTS, parents).set(PERMISSIONS, permissions).set(OPTIONS, options);
    }
}
