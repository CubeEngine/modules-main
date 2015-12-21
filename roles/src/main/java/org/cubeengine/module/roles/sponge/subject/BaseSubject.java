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
package org.cubeengine.module.roles.sponge.subject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.data.BaseSubjectData;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.context.ContextCalculator;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.spongepowered.api.service.permission.PermissionService.SUBJECTS_ROLE_TEMPLATE;

public abstract class BaseSubject<T extends OptionSubjectData> implements OptionSubject
{
    public static final String PERMISSION_TEMPLATE_PREFIX = "permission:";
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
        Tristate value = getPermissionValue(contexts, permission, getTransientSubjectData(), service, true);
        if (value == Tristate.UNDEFINED)
        {
            return getPermissionValue(contexts, permission, getSubjectData(), service, true);
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
        return Optional.empty();
    }


    private static Tristate getPermissionValue(Set<Context> contexts, String permission, OptionSubjectData data,
                                               PermissionService service, boolean resolve)
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
            List<String> implicits = new ArrayList<>();

            // Search for implicit parents first...
            int lastDot = permission.lastIndexOf(".");
            while (lastDot != -1)
            {
                permission = permission.substring(0, lastDot);
                implicits.add(permission);
                lastDot = permission.lastIndexOf(".");
            }
            Tristate value = Tristate.UNDEFINED;
            for (String parent : implicits)
            {
                value = getPermissionValue(contexts, parent, data, service, false); // not recursive (we got all already)
                if (value != Tristate.UNDEFINED)
                {
                    break;
                }
            }
            if (value == Tristate.FALSE) // Always False return
            {
                return value;
            }
            // else UNDEFINED OR TRUE

            // Seach for explicit parents...
            PermissionDescription perm = service.getDescription(permission).orElse(null);
            if (perm != null)
            {
                List<String> explicits = stream(service.getSubjects(SUBJECTS_ROLE_TEMPLATE).getAllSubjects().spliterator(), false)
                    .filter(s -> s.getSubjectData() != data)
                    .filter(s -> s.getIdentifier().startsWith(PERMISSION_TEMPLATE_PREFIX))
                    .map(s -> s.getIdentifier().substring(PERMISSION_TEMPLATE_PREFIX.length()))
                    .collect(toList());

                for (String parent : explicits)
                {
                    value = value.and(getPermissionValue(contexts, parent, data, service, true)); // recursive
                    if (value == Tristate.FALSE)
                    {
                        return value;
                    }
                }
            }
            return value;
        }
        return Tristate.UNDEFINED;
    }


    @Override
    public Set<Context> getActiveContexts()
    {
        Set<Context> contexts = new HashSet<>();
        for (ContextCalculator calculator : service.getContextCalculators())
        {
            calculator.accumulateContexts(this, contexts);
            // TODO calculator.accumulateContexts(getUser(), contexts); for user
        }
        return contexts;
    }
}
