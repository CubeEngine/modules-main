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
package org.cubeengine.module.roles.service.subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.RolesUtil.FoundPermission;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.data.BaseSubjectData;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.unmodifiableList;

public abstract class BaseSubject<T extends SubjectData> implements Subject
{
    private BaseSubjectData transientData;
    private final SubjectCollection collection;
    protected RolesPermissionService service;

    private SubjectReference ref;

    public BaseSubject(SubjectCollection collection, RolesPermissionService service)
    {
        this.collection = collection;
        this.service = service;
        this.transientData = new BaseSubjectData(service, this,true);
    }

    @Override
    public SubjectData transientSubjectData()
    {
        return transientData;
    }

    @Override
    public Tristate permissionValue(String permission, Cause cause)
    {
        return this.permissionValue(permission, service.contextsFor(cause));
    }

    @Override
    public Tristate permissionValue(String permission, Set<Context> contexts)
    {
        if (permission == null)
        {
            return Tristate.UNDEFINED;
        }
        FoundPermission perm = RolesUtil.findPermission(service, this, permission, contexts);
        if (perm != null)
        {
            return Tristate.fromBoolean(perm.value);
        }
        return Tristate.UNDEFINED;
    }

    @Override
    public boolean isChildOf(SubjectReference parent, Cause cause)
    {
        return this.isChildOf(parent, service.contextsFor(cause));
    }

    @Override
    public boolean isChildOf(SubjectReference parent, Set<Context> contexts)
    {
        return transientSubjectData().parents(contexts).contains(parent) || subjectData().parents(contexts).contains(parent);
    }

    @Override
    public Optional<String> option(String key, Cause cause)
    {
        return this.option(key, service.contextsFor(cause));
    }

    @Override
    public Optional<String> option(String key, Set<Context> contexts)
    {
        return RolesUtil.getOption(service, this, key, contexts, true).map(found -> found.value);
    }

    @Override
    public List<? extends SubjectReference> parents(Cause cause)
    {
        return this.parents(service.contextsFor(cause));
    }

    @Override
    public List<SubjectReference> parents(Set<Context> contexts)
    {
        List<SubjectReference> parents = new ArrayList<>(transientSubjectData().parents(contexts));
        parents.addAll(subjectData().parents(contexts));
        return unmodifiableList(parents);
    }

    @Override
    public SubjectReference asSubjectReference()
    {
        if (this.ref == null)
        {
            this.ref = new RolesSubjectReference(identifier(), containingCollection());
        }
        return this.ref;
    }

    @Override
    public SubjectCollection containingCollection()
    {
        return collection;
    }

}
