package de.cubeisland.engine.module.roles;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;

import static org.spongepowered.api.util.Tristate.UNDEFINED;

public abstract class BaseSubjectCollection implements SubjectCollection
{
    private final String identifier;

    public BaseSubjectCollection(String identifier)
    {
        this.identifier = identifier;
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
}
