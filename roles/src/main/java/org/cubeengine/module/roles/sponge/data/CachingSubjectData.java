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
package org.cubeengine.module.roles.sponge.data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;

public abstract class CachingSubjectData extends BaseSubjectData
{
    protected abstract void cacheParents(Set<Context> c);
    protected abstract void cacheParents();
    protected abstract void cachePermissions(Set<Context> c);
    protected abstract void cachePermissions();
    protected abstract void cacheOptions(Set<Context> c);
    protected abstract void cacheOptions();
    public abstract boolean save(boolean changed);

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents()
    {
        cacheParents();
        return super.getAllParents();
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts)
    {
        cacheParents(contexts);
        return super.getParents(contexts);
    }

    @Override
    public boolean addParent(Set<Context> contexts, Subject parent)
    {
        cacheParents(contexts);
        return save(super.addParent(contexts, parent));
    }

    @Override
    public boolean removeParent(Set<Context> contexts, Subject parent)
    {
        cacheParents(contexts);
        return save(super.removeParent(contexts, parent));
    }

    @Override
    public boolean clearParents()
    {
        cacheParents();
        return save(super.clearParents());
    }

    @Override
    public boolean clearParents(Set<Context> contexts)
    {
        cacheParents(contexts);
        return save(super.clearParents(contexts));
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions()
    {
        cachePermissions();
        return super.getAllPermissions();
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> contexts)
    {
        cachePermissions(contexts);
        return super.getPermissions(contexts);
    }

    @Override
    public boolean setPermission(Set<Context> contexts, String permission, Tristate value)
    {
        cachePermissions(contexts);
        return save(super.setPermission(contexts, permission, value));
    }

    @Override
    public boolean clearPermissions()
    {
        cachePermissions();
        return save(super.clearPermissions());
    }

    @Override
    public boolean clearPermissions(Set<Context> contexts)
    {
        cachePermissions(contexts);
        return save(super.clearPermissions(contexts));
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions()
    {
        cacheOptions();
        return super.getAllOptions();
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts)
    {
        cacheOptions(contexts);
        return super.getOptions(contexts);
    }

    @Override
    public boolean setOption(Set<Context> contexts, String key, String value)
    {
        cacheOptions(contexts);
        return save(super.setOption(contexts, key, value));
    }

    @Override
    public boolean clearOptions(Set<Context> contexts)
    {
        cacheOptions(contexts);
        return save(super.clearOptions(contexts));
    }

    @Override
    public boolean clearOptions()
    {
        cacheOptions();
        return save(super.clearOptions());
    }

    public void reload()
    {
        this.parents.clear();
        this.options.clear();
        this.permissions.clear();
    }
}
