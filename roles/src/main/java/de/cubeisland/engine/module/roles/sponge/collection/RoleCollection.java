package de.cubeisland.engine.module.roles.sponge.collection;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.RolesConfig;
import de.cubeisland.engine.module.roles.config.RoleConfig;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.subject.RoleSubject;
import de.cubeisland.engine.reflect.Reflector;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;

import static org.spongepowered.api.service.permission.PermissionService.SUBJECTS_GROUP;

public class RoleCollection extends BaseSubjectCollection
{
    private final Map<String, String> mirrors;
    private final Map<String, RoleSubject> subjects = new ConcurrentHashMap<>();
    private Roles module;
    private RolesPermissionService service;
    private Reflector reflector;

    public RoleCollection(Roles module, RolesPermissionService service, Reflector reflector)
    {
        super(SUBJECTS_GROUP);
        this.module = module;
        this.service = service;
        this.reflector = reflector;

        mirrors = readMirrors(service.getConfig().mirrors.roles);

        // TODO fill subjects with roles from files
    }

    @Override
    public RoleSubject get(String identifier)
    {
        // TODO apply mirrors
        RoleSubject roleSubject = subjects.get(identifier);
        if (roleSubject == null)
        {
            if (!identifier.startsWith("role:"))
            {
                // must be <ctxType>|<ctxName>|<roleName>
                throw new IllegalArgumentException("Provided identifier is not a role: " + identifier);
            }
            String name = identifier.substring(5);
            String[] split = name.split("|", -1);
            String ctxType = split[0];
            String ctxName = "";
            if (split.length == 3)
            {
                ctxName = split[1];
                name = split[2];
            }
            else if (split.length == 2)
            {
                name = split[1];
            }
            else
            {
                throw new IllegalArgumentException("Provided identifier has an invalid context: " + identifier);
            }
            Path path = module.getProvided(Path.class).resolve(ctxType);
            if (!ctxName.isEmpty())
            {
                path = path.resolve(ctxName);
            }
            RoleConfig config = reflector.create(RoleConfig.class);
            config.setFile(path.resolve(name + ".yml").toFile());
            roleSubject = new RoleSubject(service, config, "global".equals(ctxType) ? null : new Context(ctxType, ctxName));

            subjects.put(identifier, roleSubject);
        }
        return null;
        // TODO create new role if not found
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
}
