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
package org.cubeengine.module.roles.service.subject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.RolesUtil.FoundPermission;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.data.BaseSubjectData;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.StreamSupport.stream;

public abstract class BaseSubject<T extends OptionSubjectData> implements OptionSubject
{
    private BaseSubjectData transientData;
    private final SubjectCollection collection;
    protected RolesPermissionService service;
    private T data;

    public BaseSubject(SubjectCollection collection, RolesPermissionService service, T data)
    {
        this.collection = collection;
        this.service = service;
        this.data = data;
        this.transientData = new BaseSubjectData(service);
    }

    @Override
    public T getSubjectData()
    {
        return data;
    }

    @Override
    public OptionSubjectData getTransientSubjectData()
    {
        return transientData;
    }

    @Override
    public Optional<String> getOption(Set<Context> contexts, String key)
    {
        return RolesUtil.getOption(this, key, contexts).map(found -> found.value);
    }

    @Override
    public Optional<String> getOption(String key)
    {
        return getOption(getActiveContexts(), key);
    }

    @Override
    public boolean hasPermission(Set<Context> contexts, String permission)
    {
        return getPermissionValue(contexts, permission) == Tristate.TRUE;
    }

    @Override
    public boolean hasPermission(String permission)
    {
        return hasPermission(getActiveContexts(), permission);
    }

    @Override
    public Tristate getPermissionValue(Set<Context> contexts, String permission)
    {
        FoundPermission perm = RolesUtil.findPermission(service, this, permission, contexts);
        if (perm != null)
        {
            return Tristate.fromBoolean(perm.value);
        }
        return Tristate.UNDEFINED;
    }

    @Override
    public boolean isChildOf(Subject parent)
    {
        return isChildOf(getActiveContexts(), parent);
    }

    @Override
    public boolean isChildOf(Set<Context> contexts, Subject parent)
    {
        return getTransientSubjectData().getParents(contexts).contains(parent) || getSubjectData().getParents(contexts).contains(parent);
    }

    @Override
    public List<Subject> getParents()
    {
        return getParents(getActiveContexts());
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts)
    {
        List<Subject> parents = new ArrayList<>(getTransientSubjectData().getParents(contexts));
        parents.addAll(getSubjectData().getParents(contexts));
        return unmodifiableList(parents);
    }

    @Override
    public SubjectCollection getContainingCollection()
    {
        return collection;
    }

    @Override
    public Set<Context> getActiveContexts()
    {
        Set<Context> contexts = new HashSet<>();
        for (ContextCalculator<Subject> calculator : service.getContextCalculators())
        {
            calculator.accumulateContexts(this, contexts);
        }
        return contexts;
    }
}
