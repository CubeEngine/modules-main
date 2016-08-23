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
package org.cubeengine.module.roles.service.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.util.Tristate;

import static org.spongepowered.api.util.Tristate.UNDEFINED;

public abstract class BaseSubjectCollection<T extends Subject> implements SubjectCollection
{
    private final String identifier;

    protected final Map<String, T> subjects = new ConcurrentHashMap<>();

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

    private void collectPerm(Subject subject, String permission, Set<Context> contexts, Map<Subject, Boolean> result)
    {
        Tristate state = subject.getPermissionValue(contexts, permission);
        if (state != UNDEFINED)
        {
            result.put(subject, state.asBoolean());
        }
    }

    @Override
    public final T get(String identifier)
    {
        T subject = subjects.get(identifier);
        if (subject == null)
        {
            subject = createSubject(identifier);
            subjects.put(identifier, subject);
        }
        return subject;
    }

    @Override
    public boolean hasRegistered(String identifier)
    {
        return subjects.containsKey(identifier);
    }

    @Override
    public Iterable<Subject> getAllSubjects()
    {
        return new ArrayList<>(subjects.values());
    }

    protected abstract T createSubject(String identifier);
}
