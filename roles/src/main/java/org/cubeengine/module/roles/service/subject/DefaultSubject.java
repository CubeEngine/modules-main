package org.cubeengine.module.roles.service.subject;

import org.cubeengine.module.roles.service.RolesPermissionService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;

import java.util.Optional;

public class DefaultSubject extends BaseSubject<SubjectData>
{
    private String id;

    public DefaultSubject(String id, SubjectCollection collection, RolesPermissionService service, SubjectData data)
    {
        super(collection, service, data);
        this.id = id;
    }

    @Override
    public String getIdentifier()
    {
        return id;
    }

    @Override
    public Optional<CommandSource> getCommandSource()
    {
        return null;
    }
}
