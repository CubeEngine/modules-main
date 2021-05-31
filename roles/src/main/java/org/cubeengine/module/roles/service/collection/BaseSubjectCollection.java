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
package org.cubeengine.module.roles.service.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RolesSubjectReference;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

import static org.spongepowered.api.util.Tristate.UNDEFINED;

public abstract class BaseSubjectCollection implements SubjectCollection
{
    protected final RolesPermissionService service;
    private final String identifier;

    protected final Map<String, Subject> subjects = new ConcurrentHashMap<>();

    public BaseSubjectCollection(RolesPermissionService service, String identifier)
    {
        this.service = service;
        this.identifier = identifier;
    }

    @Override
    public String identifier()
    {
        return identifier;
    }

    @Override
    public Predicate<String> identifierValidityPredicate()
    {
        return this::isValid;
    }

    protected boolean isValid(String identifier)
    {
        return true;
    }

    @Override
    public CompletableFuture<Subject> loadSubject(String identifier)
    {
        return CompletableFuture.supplyAsync(() -> loadSubject0(identifier));
    }

    protected abstract Subject loadSubject0(String identifier);

    @Override
    public final Optional<Subject> subject(String identifier)
    {
        return Optional.ofNullable(subjects.get(identifier));
    }

    @Override
    public CompletableFuture<Map<String, Subject>> loadSubjects(Iterable<String> identifiers)
    {
        return CompletableFuture.supplyAsync(() -> this.loadSubjects0(identifiers));
    }

    private Map<String, Subject> loadSubjects0(Iterable<String> ids)
    {
        Map<String, Subject> map = new HashMap<>();
        for (String id : ids)
        {
            Optional<Subject> subject = this.subject(id);
            if (subject.isPresent())
            {
                map.put(id, subject.get());
            }
            else
            {
                map.put(id, loadSubject0(id));
            }
        }
        return map;
    }

    @Override
    public Collection<Subject> loadedSubjects()
    {
        List<Subject> list = new ArrayList<>();
        list.addAll(this.subjects.values());
        return Collections.unmodifiableCollection(list);
    }

    @Override
    public SubjectReference newSubjectReference(String subjectIdentifier)
    {
        if (this.identifierValidityPredicate().test(subjectIdentifier))
        {
            return new RolesSubjectReference(subjectIdentifier, this);
        }
        throw new IllegalArgumentException("Invalid Identifier");
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> allWithPermission(String permission)
    {
        return allIdentifiers().thenApply(this::loadSubjects0).thenApply(s -> {
            final Map<SubjectReference, Boolean> result = new HashMap<>();
            s.values().forEach(elem -> collectPermRef(elem, permission, elem.contextCause(), result));
            return Collections.unmodifiableMap(result);
        });
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> allWithPermission(String permission, Cause cause)
    {
        return allIdentifiers().thenApply(this::loadSubjects0).thenApply(s -> {
            final Map<SubjectReference, Boolean> result = new HashMap<>();
            s.values().forEach(elem -> collectPermRef(elem, permission, cause, result));
            return Collections.unmodifiableMap(result);
        });
    }

    @Override
    public Map<Subject, Boolean> loadedWithPermission(String permission)
    {
        final Map<Subject, Boolean> result = new HashMap<>();
        subjects.values().forEach(elem -> collectPerm(elem, permission, elem.contextCause(), result));
        return Collections.unmodifiableMap(result);
    }

    @Override
    public Map<Subject, Boolean> loadedWithPermission(String permission, Cause cause)
    {
        final Map<Subject, Boolean> result = new HashMap<>();
        subjects.values().forEach(elem -> collectPerm(elem, permission, cause, result));
        return Collections.unmodifiableMap(result);
    }

    @Override
    public Subject defaults()
    {
        try
        {
            return service.collection(PermissionService.SUBJECTS_DEFAULT).get().loadSubject(identifier()).get();
        }
        catch (ExecutionException | InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private void collectPerm(Subject subject, String permission, Cause cause, Map<Subject, Boolean> result)
    {
        Tristate state = subject.permissionValue(permission, service.contextsFor(cause));
        if (state != UNDEFINED)
        {
            result.put(subject, state.asBoolean());
        }
    }

    private void collectPermRef(Subject subject, String permission, Cause cause, Map<SubjectReference, Boolean> result)
    {
        Tristate state = subject.permissionValue(permission, service.contextsFor(cause));
        if (state != UNDEFINED)
        {
            result.put(subject.containingCollection().newSubjectReference(subject.identifier()), state.asBoolean());
        }
    }
}
