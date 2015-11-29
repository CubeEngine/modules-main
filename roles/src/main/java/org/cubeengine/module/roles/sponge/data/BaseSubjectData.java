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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.cubeengine.module.roles.commands.RoleCommands;
import org.cubeengine.module.roles.exception.CircularRoleDependencyException;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * The Base for Roles OptionSubjectData without persistence
 */
public class BaseSubjectData implements OptionSubjectData
{
    protected final Map<Set<Context>, Map<String, String>> options = new ConcurrentHashMap<>();
    protected final Map<Set<Context>, Map<String, Boolean>> permissions = new ConcurrentHashMap<>();
    protected final Map<Set<Context>, List<Subject>> parents = new ConcurrentHashMap<>();

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions()
    {
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
        return unCache(operate(contexts, options, map -> map.put(key, value)), contexts, options.keySet());
    }

    @Override
    public boolean clearOptions(Set<Context> contexts)
    {
        return unCache(operate(contexts, options, Map::clear), contexts, options.keySet());
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
        return unmodifiableMap(permissions);
    }

    @Override
    public boolean setPermission(Set<Context> contexts, String permission, Tristate value)
    {
        return unCache(operate(contexts, permissions, map -> {
            if (value == Tristate.UNDEFINED)
            {
                map.remove(permission);
            }
            else
            {
                map.put(permission, value.asBoolean());
            }
        }), contexts, permissions.keySet());
    }

    @Override
    public boolean clearPermissions(Set<Context> contexts)
    {
        return unCache(operate(contexts, permissions, Map::clear), contexts, permissions.keySet());
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
        return unmodifiableMap(parents);
    }

    @Override
    public boolean addParent(Set<Context> contexts, Subject parent)
    {
        checkForCircularDependency(contexts, parent, 0);
        if (parents.get(contexts).contains(parent))
        {
            return false;
        }
        return unCache(operate(contexts, parents, l -> l.add(parent)), contexts, parents.keySet());
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
        return unCache(operate(contexts, parents, l -> l.remove(parent)), contexts, parents.keySet());
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
        return unCache(operate(contexts, parents, List::clear), contexts, parents.keySet());
    }

    private boolean unCache(boolean changed, Set<Context> contexts, Set<Set<Context>> keySet)
    {
        if (changed)
        {
            for (Iterator<Set<Context>> it = keySet.iterator(); it.hasNext(); )
            {
                final Set<Context> set = it.next();
                if (set.size() > 1 && !Collections.disjoint(set, contexts))
                {
                    it.remove();
                }
            }
        }
        return changed;
    }

    @FunctionalInterface
    interface Operator<T>
    {
        void operate(T mapOrList);
    }

    private <T> boolean operate(Set<Context> contexts, Map<Set<Context>, T> all, Operator<T> operator)
    {
        boolean changed = false;
        for (Context context : contexts)
        {
            T map = all.get(RoleCommands.toSet(context));
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

    private <T> T accumulate(Set<Context> contexts, Map<Set<Context>, T> all, T result, Accumulator<T> accumulator)
    {
        if (all.containsKey(contexts))
        {
            return all.get(contexts);
        }
        for (Context context : contexts)
        {
            T other = all.get(RoleCommands.toSet(context));
            if (other != null)
            {
                accumulator.operate(result, other);
            }
        }
        all.put(contexts, result);
        return result;
    }

    public Set<Context> getContexts()
    {
        return GLOBAL_CONTEXT;
    }
}
