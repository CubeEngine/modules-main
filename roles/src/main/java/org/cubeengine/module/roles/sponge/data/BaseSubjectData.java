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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.cubeengine.module.roles.commands.RoleCommands;
import org.cubeengine.module.roles.exception.CircularRoleDependencyException;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.collection.RoleCollection;
import org.cubeengine.module.roles.sponge.collection.UserCollection;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.cubeengine.module.roles.commands.RoleCommands.toSet;

/**
 * The Base for Roles OptionSubjectData without persistence
 */
public class BaseSubjectData implements OptionSubjectData
{
    protected final Map<Context, Map<String, String>> options = new ConcurrentHashMap<>();
    protected final Map<Context, Map<String, Boolean>> permissions = new ConcurrentHashMap<>();
    protected final Map<Context, List<Subject>> parents = new ConcurrentHashMap<>();

    protected final UserCollection userCollection;
    protected final RoleCollection roleCollection;

    public BaseSubjectData(RolesPermissionService service)
    {
        userCollection = service.getUserSubjects();
        roleCollection = service.getGroupSubjects();
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions()
    {
        Map<Set<Context>, Map<String, String>> options = this.options.entrySet().stream()
                .collect(Collectors.toMap(e -> toSet(e.getKey()), Map.Entry::getValue));
        return unmodifiableMap(options);
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts)
    {
        return unmodifiableMap(accumulate(contexts, options, new HashMap<>(), Map::putAll));
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> contexts)
    {
        return unmodifiableMap(accumulate(contexts, permissions, new HashMap<>(), Map::putAll));
    }


    @Override
    public List<Subject> getParents(Set<Context> contexts)
    {
        return unmodifiableList(accumulate(contexts, parents, new ArrayList<>(), List::addAll));
    }

    @Override
    public boolean setOption(Set<Context> contexts, String key, String value)
    {
        return operate(contexts, options, map -> map.put(key, value));
    }

    @Override
    public boolean clearOptions(Set<Context> contexts)
    {
        return operate(contexts, options, Map::clear);
    }

    @Override
    public boolean clearOptions()
    {
        boolean changed = false;
        for (Map<String, String> map : options.values())
        {
            if (!map.isEmpty())
            {
                changed = true;
            }
            map.clear();
        }
        return changed;
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions()
    {
        Map<Set<Context>, Map<String, Boolean>> permissions = this.permissions.entrySet().stream()
                .collect(Collectors.toMap(e -> toSet(e.getKey()), Map.Entry::getValue));

        return unmodifiableMap(permissions);
    }

    @Override
    public boolean setPermission(Set<Context> contexts, String permission, Tristate value)
    {
        return operate(contexts, permissions, map -> {
            if (value == Tristate.UNDEFINED)
            {
                map.remove(permission);
            }
            else
            {
                map.put(permission, value.asBoolean());
            }
        });
    }

    @Override
    public boolean clearPermissions(Set<Context> contexts)
    {
        return operate(contexts, permissions, Map::clear);
    }

    @Override
    public boolean clearPermissions()
    {
        boolean changed = false;
        for (Map<String, Boolean> map : permissions.values())
        {
            if (!map.isEmpty())
            {
                changed = true;
            }
            map.clear();
        }
        return changed;
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents()
    {
        Map<Set<Context>, List<Subject>> parents = this.parents.entrySet().stream()
                .collect(Collectors.toMap(e -> toSet(e.getKey()), Map.Entry::getValue));

        return unmodifiableMap(parents);
    }

    @Override
    public boolean addParent(Set<Context> contexts, Subject parent)
    {
        checkForCircularDependency(contexts, parent, 0);

        for (Context context : contexts)
        {
            if (parents.containsKey(context))
            {
                if (parents.get(context).contains(parent))
                {
                    return false;
                }
            }
        }

        return operate(contexts, parents, l -> l.add(parent));
    }

    protected void checkForCircularDependency(Set<Context> contexts, Subject parent, int depth)
    {
        if (this == parent.getSubjectData())
        {
            throw new CircularRoleDependencyException("at", depth); // TODO translatable / show parameter
            // add exceptionhandler to cmd lib?
        }
        depth++;
        for (Subject parentParents : parent.getParents(contexts))
        {
            checkForCircularDependency(contexts, parentParents, depth);
        }
    }

    @Override
    public boolean removeParent(Set<Context> contexts, Subject parent)
    {
        return operate(contexts, parents, l -> l.remove(parent));
    }

    @Override
    public boolean clearParents()
    {
        parents.values().forEach(List::clear);
        return true;
    }

    @Override
    public boolean clearParents(Set<Context> contexts)
    {
        return operate(contexts, parents, List::clear);
    }

    @FunctionalInterface
    interface Operator<T>
    {
        void operate(T mapOrList);
    }

    private <T> boolean operate(Set<Context> contexts, Map<Context, T> all, Operator<T> operator)
    {
        boolean changed = false;
        for (Context context : contexts)
        {
            T map = all.get(context);
            if (map != null)
            {
                operator.operate(map);
                changed = true;
            }
        }
        return changed;
    }

    @FunctionalInterface
    interface Accumulator<T>
    {
        void operate(T mapOrList, T other);
    }

    private <T> T accumulate(Set<Context> contexts, Map<Context, T> all, T result, Accumulator<T> accumulator)
    {
        for (Context context : contexts)
        {
            context = getMirror(context, result); // only for role-data
            T other = all.get(context);
            if (other != null)
            {
                accumulator.operate(result, other);
            }
        }
        return result;
    }

    protected Context getMirror(Context context, Object result)
    {
        return context;
    }
}
