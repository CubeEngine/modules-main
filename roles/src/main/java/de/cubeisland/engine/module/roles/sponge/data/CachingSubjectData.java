package de.cubeisland.engine.module.roles.sponge.data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;

public abstract class CachingSubjectData extends BaseSubjectData
{
    protected abstract void cacheOptions(Set<Context> c);
    protected abstract void cachePermissions(Set<Context> c);
    protected abstract void cacheParents(Set<Context> c);
    protected abstract boolean save(boolean changed);

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions()
    {
        cacheOptions(getContexts());
        return super.getAllOptions();
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts)
    {
        cacheOptions(contexts);
        return super.getOptions(contexts);
    }

    @Override
    public boolean setOption(Set<Context> contexts, String key, String value)
    {
        cacheOptions(contexts);
        return save(super.setOption(contexts, key, value));
    }

    @Override
    public boolean clearOptions(Set<Context> contexts)
    {
        cacheOptions(contexts);
        return save(super.clearOptions(contexts));
    }

    @Override
    public boolean clearOptions()
    {
        cacheOptions(getContexts());
        return save(super.clearOptions());
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions()
    {
        cachePermissions(getContexts());
        return super.getAllPermissions();
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> contexts)
    {
        cachePermissions(contexts);
        return super.getPermissions(contexts);
    }

    @Override
    public boolean setPermission(Set<Context> contexts, String permission, Tristate value)
    {
        cachePermissions(contexts);
        return save(super.setPermission(contexts, permission, value));
    }

    @Override
    public boolean clearPermissions()
    {
        cachePermissions(getContexts());
        return save(super.clearPermissions());
    }

    @Override
    public boolean clearPermissions(Set<Context> contexts)
    {
        cachePermissions(contexts);
        return save(super.clearPermissions(contexts));
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents()
    {
        cacheParents(getContexts());
        return super.getAllParents();
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts)
    {
        cacheParents(contexts);
        return super.getParents(contexts);
    }

    @Override
    public boolean addParent(Set<Context> contexts, Subject parent)
    {
        cacheParents(contexts);
        return save(super.addParent(contexts, parent));
    }

    @Override
    public boolean removeParent(Set<Context> contexts, Subject parent)
    {
        cacheParents(contexts);
        return save(super.removeParent(contexts, parent));
    }

    @Override
    public boolean clearParents()
    {
        cacheParents(getContexts());
        return save(super.clearParents());
    }

    @Override
    public boolean clearParents(Set<Context> contexts)
    {
        cacheParents(contexts);
        return save(super.clearParents(contexts));
    }


}
