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
package de.cubeisland.engine.module.roles.sponge.subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.google.common.base.Optional;
import de.cubeisland.engine.module.roles.sponge.data.BaseSubjectData;
import de.cubeisland.engine.module.service.permission.Permission;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.unmodifiableList;

public abstract class BaseSubject implements OptionSubject
{
    private BaseSubjectData transientData = new BaseSubjectData();
    private final SubjectCollection collection;
    private PermissionManager permissionManager;

    public BaseSubject(SubjectCollection collection, PermissionManager permissionManager)
    {
        this.collection = collection;
        this.permissionManager = permissionManager;
    }

    @Override
    public OptionSubjectData getTransientSubjectData()
    {
        return transientData;
    }

    @Override
    public Optional<String> getOption(Set<Context> contexts, String key)
    {
        Optional<String> result = getOption(contexts, key, getTransientSubjectData());
        if (result.isPresent() || getTransientSubjectData() == getSubjectData())
        {
            return result;
        }
        return getOption(contexts, key, getSubjectData());
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
        Tristate value = getPermissionValue(contexts, permission, getTransientSubjectData(), permissionManager, true);
        if (value == Tristate.UNDEFINED)
        {
            return getPermissionValue(contexts, permission, getSubjectData(), permissionManager, true);
        }
        return value;
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

    private static Optional<String> getOption(Set<Context> contexts, String key, OptionSubjectData data)
    {
        String result = data.getOptions(contexts).get(key);
        if (result != null)
        {
            return Optional.of(result);
        }
        for (Subject subject : data.getParents(contexts))
        {
            if (subject instanceof OptionSubject)
            {
                Optional<String> option = ((OptionSubject)subject).getOption(contexts, key);
                if (option.isPresent())
                {
                    return option;
                }
            }
        }
        return Optional.absent();
    }


    private static Tristate getPermissionValue(Set<Context> contexts, String permission, OptionSubjectData data,
                                               PermissionManager manager, boolean resolve)
    {
        Boolean state = data.getPermissions(contexts).get(permission);
        if (state != null)
        {
            return Tristate.fromBoolean(state);
        }
        for (Subject subject : data.getParents(contexts))
        {
            Tristate value = subject.getPermissionValue(contexts, permission);
            if (value != Tristate.UNDEFINED)
            {
                return value;
            }
        }
        if (resolve)
        {
            Optional<Permission> perm = manager.getPermission(permission);
            if (perm.isPresent())
            {
                for (String parent : perm.get().allParents())
                {
                    Tristate value = getPermissionValue(contexts, parent, data, manager, false);
                    if (value != Tristate.UNDEFINED)
                    {
                        return value;
                    }
                }
            }
            else // attempt to find * permissions higher up
            {
                int lastDot = permission.lastIndexOf(".");
                while (lastDot != -1)
                {
                    permission = permission.substring(0, lastDot);
                    Tristate value = getPermissionValue(contexts, permission + ".*", data, manager, false);
                    if (value != Tristate.UNDEFINED)
                    {
                        return value;
                    }
                    lastDot = permission.lastIndexOf(".");
                }
                Tristate value = getPermissionValue(contexts, "*", data, manager, false);
                if (value != Tristate.UNDEFINED)
                {
                    return value;
                }
            }
        }
        return Tristate.UNDEFINED;
    }
}
