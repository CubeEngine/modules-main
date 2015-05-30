package de.cubeisland.engine.module.roles.sponge.data;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import de.cubeisland.engine.module.roles.RolesConfig;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.context.Context;

import static java.util.Collections.singleton;

public class DefaultSubjectData extends BaseSubjectData
{
    public DefaultSubjectData(PermissionService service, RolesConfig config)
    {
        for (Entry<String, Set<String>> entry : config.defaultRoles.entrySet())
        {
            String name = entry.getKey();
            String type = Context.WORLD_KEY;
            if (name.contains("|"))
            {
                type = name.substring(0, name.indexOf("|"));
                name = name.substring(name.indexOf("|") + 1);
            }
            Set<Context> contexts = "global".equals(name) ? GLOBAL_CONTEXT : singleton(new Context(type, name));
            for (String role : entry.getValue())
            {
                addParent(contexts, service.getGroupSubjects().get(role));
            }
        }
    }
}
