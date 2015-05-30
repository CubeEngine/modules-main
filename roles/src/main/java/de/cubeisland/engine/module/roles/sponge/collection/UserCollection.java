package de.cubeisland.engine.module.roles.sponge.collection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.google.common.base.Optional;
import de.cubeisland.engine.module.roles.RolesConfig;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.subject.UserSubject;
import de.cubeisland.engine.module.service.database.Database;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.player.User;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorage;

public class UserCollection extends BaseSubjectCollection
{
    private final Map<String, String> assignedMirrors;
    private final Map<String, String> directMirrors;
    private final Map<String, UserSubject> subjects = new ConcurrentHashMap<>();
    private RolesPermissionService service;
    private Game game;

    public UserCollection(RolesPermissionService service, Game game)
    {
        super(PermissionService.SUBJECTS_USER);
        this.service = service;
        this.game = game;
        assignedMirrors = readMirrors(service.getConfig().mirrors.assigned);
        directMirrors = readMirrors(service.getConfig().mirrors.direct);
        // TODO use mirrors when resolving context
    }

    @Override
    public Subject get(String identifier)
    {
        UserSubject subject = subjects.get(identifier);
        if (subject == null)
        {
            try
            {
                UUID uuid = UUID.fromString(identifier);
                Optional<User> user = game.getServiceManager().provideUnchecked(UserStorage.class).get(uuid);
                if (!user.isPresent())
                {
                    subject = new UserSubject(service, uuid);
                }
                else
                {
                    subject = new UserSubject(service, user.get());
                }
            }
            catch (IllegalArgumentException e)
            {
                throw new IllegalArgumentException("Provided identifier must be a uuid, was " + identifier);
            }
        }
        return subject;
    }

    @Override
    public boolean hasRegistered(String identifier)
    {
        // TODO DB-Lookup ?
        return false;
    }

    @Override
    public Iterable<Subject> getAllSubjects()
    {
        // TODO lazy DB-Lookup
        return null;
    }
}
