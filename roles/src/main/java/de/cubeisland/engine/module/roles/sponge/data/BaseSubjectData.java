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
package de.cubeisland.engine.module.roles.sponge.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.emptyMap;
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
        return options.containsKey(contexts) ? unmodifiableMap(options.get(contexts)) : emptyMap();
    }

    @Override
    public boolean setOption(Set<Context> contexts, String key, String value)
    {
        Map<String, String> map = options.get(contexts);
        if (map == null)
        {
            return false;
        }
        map.put(key, value);
        return true;
    }

    @Override
    public boolean clearOptions(Set<Context> contexts)
    {
        Map<String, String> map = options.get(contexts);
        if (map == null)
        {
            return false;
        }
        map.clear();
        return true;
    }

    @Override
    public boolean clearOptions()
    {
        boolean isEmpty = true;
        for (Map<String, String> map : options.values())
        {
            if (!map.isEmpty())
            {
                isEmpty = false;
            }
        }
        if (isEmpty)
        {
            return false;
        }
        options.values().forEach(Map::clear);
        return true;
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions()
    {
        return unmodifiableMap(permissions);
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> contexts)
    {
        return permissions.containsKey(contexts) ? unmodifiableMap(permissions.get(contexts)) : emptyMap();
    }

    @Override
    public boolean setPermission(Set<Context> contexts, String permission, Tristate value)
    {
        Map<String, Boolean> map = permissions.get(contexts);
        if (map == null)
        {
            return false;
        }
        if (value == Tristate.UNDEFINED)
        {
            map.remove(permission);
        }
        else
        {
            map.put(permission, value.asBoolean());
        }
        return true;
    }

    @Override
    public boolean clearPermissions()
    {
        boolean isEmpty = true;
        for (Map<String, Boolean> map : permissions.values())
        {
            if (!map.isEmpty())
            {
                isEmpty = false;
            }
        }
        if (isEmpty)
        {
            return false;
        }
        permissions.values().forEach(Map::clear);
        return true;
    }

    @Override
    public boolean clearPermissions(Set<Context> contexts)
    {
        Map<String, Boolean> map = permissions.get(contexts);
        if (map == null)
        {
            return false;
        }
        map.clear();
        return true;
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents()
    {
        return unmodifiableMap(parents);
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts)
    {
        List<Subject> subjects = parents.get(contexts);
        if (subjects == null)
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(subjects);
    }

    @Override
    public boolean addParent(Set<Context> contexts, Subject parent)
    {
        List<Subject> list = parents.get(contexts);
        if (list == null)
        {
            return false;
        }
        list.add(parent);
        return true;
    }

    @Override
    public boolean removeParent(Set<Context> contexts, Subject parent)
    {
        List<Subject> list = parents.get(contexts);
        if (list == null)
        {
            return false;
        }
        list.remove(parent);
        return true;
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
        List<Subject> list = parents.get(contexts);
        if (list == null)
        {
            return false;
        }
        parents.clear();
        return true;
    }

    public Set<Context> getContexts()
    {
        return GLOBAL_CONTEXT;
    }
}
