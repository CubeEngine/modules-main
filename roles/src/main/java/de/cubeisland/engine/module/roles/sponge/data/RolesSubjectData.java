package de.cubeisland.engine.module.roles.sponge.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import de.cubeisland.engine.module.roles.config.RoleConfig;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.collection.RoleCollection;
import de.cubeisland.engine.module.roles.sponge.subject.RoleSubject;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;

import static java.util.Collections.singleton;

public class RolesSubjectData extends CachingSubjectData
{
    private final RoleConfig config;
    private Context context;
    private final Set<Context> contexts;
    private final RoleCollection collection;

    public RolesSubjectData(RolesPermissionService service, RoleConfig config, Context context)
    {
        this.config = config;
        this.context = context;
        this.contexts = context == null ? GLOBAL_CONTEXT : singleton(context);
        this.collection = service.getGroupSubjects();
    }

    @Override
    protected void cacheOptions(Set<Context> c)
    {
        if (getContexts().equals(c) && !options.containsKey(getContexts()))
        {
            options.put(getContexts(), config.metadata);
        }
    }

    @Override
    protected void cachePermissions(Set<Context> c)
    {
        if (getContexts().equals(c) && !permissions.containsKey(getContexts()))
        {
            permissions.put(getContexts(), config.perms.getPermissions());
        }
    }

    @Override
    protected void cacheParents(Set<Context> c)
    {
        if (getContexts().equals(c) && !parents.containsKey(getContexts()))
        {
            List<RoleSubject> parents = new ArrayList<>();
            for (String parent : config.parents)
            {
                if (!parent.contains(RoleSubject.SEPARATOR))
                {
                    parent = context.getKey() + RoleSubject.SEPARATOR + context.getValue() + RoleSubject.SEPARATOR + parent;
                }
                parents.add(collection.get("role:" + parent));
            }
            Collections.sort(parents);
            this.parents.put(contexts, new ArrayList<>(parents));
        }
    }

    @Override
    protected boolean save(boolean changed)
    {
        if (changed)
        {
            List<Subject> list = parents.get(contexts);
            if (list != null)
            {
                config.parents.clear();
                for (Subject subject : list)
                {
                    if (!(subject instanceof RoleSubject))
                    {
                        // TODO WARN: Subject that is not a role will not be persisted
                        continue;
                    }
                    config.parents.add(subject.getIdentifier().substring(5));
                }
            }
            config.save();// TODO async
        }
        return changed;
    }

    public RoleConfig getConfig()
    {
        return config;
    }

    @Override
    public Set<Context> getContexts()
    {
        return contexts;
    }
}
