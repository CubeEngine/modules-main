package de.cubeisland.engine.module.roles.sponge.subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.google.common.base.Optional;
import de.cubeisland.engine.module.roles.sponge.data.BaseSubjectData;
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

    public BaseSubject(SubjectCollection collection)
    {
        this.collection = collection;
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
        Tristate value = getPermissionValue(contexts, permission, getTransientSubjectData());
        if (value == Tristate.UNDEFINED)
        {
            return getPermissionValue(contexts, permission, getSubjectData());
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


    private static Tristate getPermissionValue(Set<Context> contexts, String permission, OptionSubjectData data)
    {
        Boolean state = data.getPermissions(contexts).get(permission);
        if (state == null)
        {
            for (Subject subject : data.getParents(contexts))
            {
                Tristate value = subject.getPermissionValue(contexts, permission);
                if (value != Tristate.UNDEFINED)
                {
                    return value;
                }
            }
            return Tristate.UNDEFINED;
        }
        return Tristate.fromBoolean(state);
    }
}
