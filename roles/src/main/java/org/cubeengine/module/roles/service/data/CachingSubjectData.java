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
package org.cubeengine.module.roles.service.data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.cubeengine.module.roles.service.RolesPermissionService;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

public abstract class CachingSubjectData extends BaseSubjectData
{
    public CachingSubjectData(RolesPermissionService service)
    {
        super(service);
    }

    protected abstract void cacheParents();
    protected abstract void cachePermissions();
    protected abstract void cacheOptions();
    public abstract CompletableFuture<Boolean> save(CompletableFuture<Boolean> changed);

    @Override
    public Map<Set<Context>, List<SubjectReference>> getAllParents()
    {
        cacheParents();
        return super.getAllParents();
    }

    @Override
    public List<SubjectReference> getParents(Set<Context> contexts)
    {
        cacheParents();
        return super.getParents(contexts);
    }

    @Override
    public CompletableFuture<Boolean> addParent(Set<Context> contexts, SubjectReference parent)
    {
        cacheParents();
        return save(super.addParent(contexts, parent));
    }

    @Override
    public CompletableFuture<Boolean> removeParent(Set<Context> contexts, SubjectReference parent)
    {
        cacheParents();
        return save(super.removeParent(contexts, parent));
    }

    @Override
    public CompletableFuture<Boolean> clearParents()
    {
        cacheParents();
        return save(super.clearParents());
    }

    @Override
    public CompletableFuture<Boolean> clearParents(Set<Context> contexts)
    {
        cacheParents();
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
        cachePermissions();
        return super.getPermissions(contexts);
    }

    @Override
    public CompletableFuture<Boolean> setPermission(Set<Context> contexts, String permission, Tristate value)
    {
        cachePermissions();
        return save(super.setPermission(contexts, permission, value));
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions()
    {
        cachePermissions();
        return save(super.clearPermissions());
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(Set<Context> contexts)
    {
        cachePermissions();
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
        cacheOptions();
        return super.getOptions(contexts);
    }

    @Override
    public CompletableFuture<Boolean> setOption(Set<Context> contexts, String key, String value)
    {
        cacheOptions();
        return save(super.setOption(contexts, key, value));
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(Set<Context> contexts)
    {
        cacheOptions();
        return save(super.clearOptions(contexts));
    }

    @Override
    public CompletableFuture<Boolean> clearOptions()
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
