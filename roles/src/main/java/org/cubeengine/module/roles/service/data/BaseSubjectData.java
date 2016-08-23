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
package org.cubeengine.module.roles.service.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.cubeengine.module.roles.exception.CircularRoleDependencyException;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.collection.RoleCollection;
import org.cubeengine.module.roles.service.collection.UserCollection;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.cubeengine.libcube.util.ContextUtil.GLOBAL;
import static org.cubeengine.libcube.util.ContextUtil.toSet;
import static org.spongepowered.api.service.context.Context.WORLD_KEY;

/**
 * The Base for Roles OptionSubjectData without persistence
 */
public class BaseSubjectData implements SubjectData
{
    protected final Map<Context, Map<String, String>> options = new ConcurrentHashMap<>();
    protected final Map<Context, Map<String, Boolean>> permissions = new ConcurrentHashMap<>();
    protected final Map<Context, List<Subject>> parents = new ConcurrentHashMap<>();

    protected final UserCollection userCollection;
    protected final RoleCollection roleCollection;
    protected final RolesPermissionService service;

    public BaseSubjectData(RolesPermissionService service)
    {
        this.service = service;
        userCollection = service.getUserSubjects();
        roleCollection = service.getGroupSubjects();
    }

    public static String stringify(Context c)
    {
        return c.getName().isEmpty() ? c.getType() : c.getType().equals(
            WORLD_KEY) ? c.getName() : c.getType() + "|" + c.getName();
    }

    public static Context asContext(String string)
    {
        String[] context = string.split("\\|");
        if (context.length == 1)
        {
            if (context[0].equals(GLOBAL.getType()))
            {
                return new Context(context[0], "");
            }
            return new Context(WORLD_KEY, context[0]);
        }
        if (context.length == 2)
        {
            return new Context(context[0], context[1]);
        }
        throw new IllegalStateException("Invalid context " + string);
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
        if (value == null)
        {
            return operate(contexts, options, map -> map.remove(key) != null);
        }
        return operate(contexts, options, map -> !value.equals(map.put(key, value)), HashMap::new);
    }

    @Override
    public boolean clearOptions(Set<Context> contexts)
    {
        return operate(contexts, options,  m -> {
            boolean empty = !m.isEmpty();
            m.clear();
            return empty;
        });
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
        if (value == Tristate.UNDEFINED)
        {
            return operate(contexts, permissions, map -> map.remove(permission) != null);
        }
        return operate(contexts, permissions, map -> {
            Boolean replaced = map.put(permission, value.asBoolean());
            return replaced == null || replaced != value.asBoolean();
        }, HashMap::new);
    }

    @Override
    public boolean clearPermissions(Set<Context> contexts)
    {
        return operate(contexts, permissions, m -> {
            boolean empty = !m.isEmpty();
            m.clear();
            return empty;
        });
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

        if (contexts.isEmpty() && parents.get(GLOBAL) != null && parents.get(GLOBAL).contains(parent))
        {
            return false;
        }

        for (Context context : contexts)
        {
            if (parents.containsKey(context) && parents.get(context).contains(parent))
            {
                return false;
            }
        }

        return operate(contexts, parents, l -> l.add(parent), ArrayList::new);
    }

    protected void checkForCircularDependency(Set<Context> contexts, Subject parent, int depth)
    {
        if (this == parent.getSubjectData())
        {
            throw new CircularRoleDependencyException("at", depth); // TODO translatable / show parameter
            // TODO add exceptionhandler to cmd lib?
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
        boolean changed = false;
        for (List<Subject> list : parents.values())
        {
            if (!list.isEmpty())
            {
                list.clear();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean clearParents(Set<Context> contexts)
    {
        return operate(contexts, parents, l -> {
            boolean empty = !l.isEmpty();
            l.clear();
            return empty;
        });
    }

    @FunctionalInterface
    interface Operator<T>
    {
        boolean operate(T mapOrList);
    }

    private <T> boolean operate(Set<Context> contexts, Map<Context, T> all, Operator<T> operator)
    {
        return operate(contexts, all, operator, () -> null);
    }

    private <T> boolean operate(Set<Context> contexts, Map<Context, T> all, Operator<T> operator, Provider<T> provider)
    {
        boolean changed = false;
        if (contexts.isEmpty())
        {
            changed = operateOn(all, operator, provider, changed, GLOBAL);
        }
        for (Context context : contexts)
        {
            changed = operateOn(all, operator, provider, changed, context);
        }
        return changed;
    }

    private <T> boolean operateOn(Map<Context, T> all, Operator<T> operator, Provider<T> provider, boolean changed, Context context)
    {
        T val = all.get(context);
        if (val == null)
        {
            val = provider.get();
            if (val != null)
            {
                all.put(context, val);
            }
        }
        if (val != null)
        {
            changed = operator.operate(val);
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
        T other = all.get(GLOBAL);
        if (other != null)
        {
            accumulator.operate(result, other);
        }

        for (Context context : contexts)
        {
            other = all.get(context);
            if (other != null)
            {
                accumulator.operate(result, other);
            }
        }
        return result;
    }


}
