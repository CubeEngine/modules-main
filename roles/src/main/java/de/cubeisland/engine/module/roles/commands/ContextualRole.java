package de.cubeisland.engine.module.roles.commands;

import java.util.Set;
import org.spongepowered.api.service.permission.context.Context;

import static java.util.Collections.singleton;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

public class ContextualRole
{
    public String contextType;
    public String contextName;
    public String roleName;

    public String getIdentifier()
    {
        return "role:" + contextType + (contextName.isEmpty() ? "" : "|" + contextName) + "|" + roleName;
    }

    public Context getContext()
    {
        return new Context(contextType, contextName);
    }
}
