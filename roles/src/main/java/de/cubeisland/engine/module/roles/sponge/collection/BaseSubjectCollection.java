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
package de.cubeisland.engine.module.roles.sponge.collection;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import de.cubeisland.engine.module.roles.RolesConfig;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;

import static de.cubeisland.engine.module.roles.sponge.subject.RoleSubject.SEPARATOR;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

public abstract class BaseSubjectCollection implements SubjectCollection
{
    private final String identifier;

    public BaseSubjectCollection(String identifier)
    {
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier()
    {
        return identifier;
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(String permission)
    {
        final Map<Subject, Boolean> result = new HashMap<>();
        getAllSubjects().forEach(elem -> collectPerm(elem, permission, elem.getActiveContexts(), result));
        return Collections.unmodifiableMap(result);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(Set<Context> contexts, String permission)
    {
        final Map<Subject, Boolean> result = new HashMap<>();
        getAllSubjects().forEach(elem -> collectPerm(elem, permission, contexts, result));
        return Collections.unmodifiableMap(result);
    }

    protected final String readMirror(String source)
    {
        if (!source.contains(SEPARATOR))
        {
            if (!"global".equals(source))
            {
                return "world" + SEPARATOR + source;
            }
        }
        return source;
    }

    protected final Map<String, String> readMirrors(Map<String, List<String>> config)
    {
        Map<String, String> mirrors = new HashMap<>();
        for (Entry<String, List<String>> roleMirror : config.entrySet())
        {
            String source = readMirror(roleMirror.getKey());
            for (String mirrored : roleMirror.getValue())
            {
                mirrors.put(readMirror(mirrored), source);
            }
        }
        return mirrors;
    }

    private void collectPerm(Subject subject, String permission, Set<Context> contexts, Map<Subject, Boolean> result)
    {
        Tristate state = subject.getPermissionValue(contexts, permission);
        if (state != UNDEFINED)
        {
            result.put(subject, state.asBoolean());
        }
    }
}
