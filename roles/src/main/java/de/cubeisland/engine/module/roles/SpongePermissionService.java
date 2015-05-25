package de.cubeisland.engine.module.roles;

import java.util.Map;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.ContextCalculator;

// TODO move to roles
public class SpongePermissionService implements PermissionService
{
    @Override
    public SubjectCollection getUserSubjects()
    {
        return null;
    }

    @Override
    public SubjectCollection getGroupSubjects()
    {
        return null;
    }

    @Override
    public SubjectData getDefaultData()
    {
        return null;
    }

    @Override
    public SubjectCollection getSubjects(String identifier)
    {
        return null;
    }

    @Override
    public Map<String, SubjectCollection> getKnownSubjects()
    {
        return null;
    }

    @Override
    public void registerContextCalculator(ContextCalculator calculator)
    {

    }
}
