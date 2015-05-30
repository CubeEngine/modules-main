package de.cubeisland.engine.module.roles.sponge.data;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.collection.RoleCollection;
import de.cubeisland.engine.module.roles.sponge.subject.RoleSubject;
import de.cubeisland.engine.module.roles.storage.UserRole;
import de.cubeisland.engine.module.roles.storage.UserOption;
import de.cubeisland.engine.module.roles.storage.UserPermission;
import de.cubeisland.engine.module.service.database.Database;
import org.spongepowered.api.service.permission.context.Context;

import static de.cubeisland.engine.module.roles.storage.TableOption.TABLE_META;
import static de.cubeisland.engine.module.roles.storage.TablePerm.TABLE_PERM;
import static de.cubeisland.engine.module.roles.storage.TableRole.TABLE_ROLE;

public class UserSubjectData extends CachingSubjectData
{
    private final Database db;
    private final UUID uuid;
    private RoleCollection roleCollection;

    public UserSubjectData(RolesPermissionService service, UUID uuid)
    {
        this.roleCollection = service.getGroupSubjects();
        this.db = service.getDB();
        this.uuid = uuid;
    }

    @Override
    protected void cacheOptions(Set<Context> c)
    {
        if (getContexts().equals(c) && !options.containsKey(getContexts()))
        {
            String context = c.toString(); // TODO
            RecordBackedMap<String, String, UserOption> map =
                new RecordBackedMap<>(db, TABLE_META,
                                      TABLE_META.KEY, TABLE_META.VALUE,
                                      TABLE_META.USER, uuid,
                                      TABLE_META.CONTEXT, context);
            options.put(getContexts(), map);
        }
    }

    @Override
    protected void cachePermissions(Set<Context> c)
    {
        if (getContexts().equals(c) && !permissions.containsKey(getContexts()))
        {
            String context = c.toString(); // TODO
            RecordBackedMap<String, Boolean, UserPermission> map =
                new RecordBackedMap<>(db, TABLE_PERM,
                                      TABLE_PERM.PERM, TABLE_PERM.ISSET,
                                      TABLE_PERM.USER, uuid,
                                      TABLE_PERM.CONTEXT, context);
            permissions.put(getContexts(), map);
        }
    }

    @Override
    protected void cacheParents(Set<Context> c)
    {
        if (getContexts().equals(c) && !parents.containsKey(getContexts()))
        {
            String context = c.toString(); // TODO
            RecordBackedList<UserRole> list =
                new RecordBackedList<>(roleCollection, db,
                                       TABLE_ROLE, TABLE_ROLE.ROLE,
                                       TABLE_ROLE.USER, uuid,
                                       TABLE_ROLE.CONTEXT, context);
            Collections.sort(list, (o1, o2) -> {
                if (o1 instanceof RoleSubject && o2 instanceof RoleSubject)
                {
                    return ((RoleSubject)o1).compareTo((RoleSubject)o2);
                }
                return 1;
            });
            this.parents.put(getContexts(), list);
        }
    }

    @Override
    protected boolean save(boolean changed)
    {
        if (changed)
        {
            permissions.values().stream()
                       .filter(map -> map instanceof RecordBackedList)
                       .map(map -> (RecordBackedList)map)
                       .forEach(RecordBackedList::save);
            options.values().stream().filter(map -> map instanceof RecordBackedList)
                       .map(map -> (RecordBackedList)map)
                       .forEach(RecordBackedList::save);
            parents.values().stream().filter(map -> map instanceof RecordBackedList)
                       .map(map -> (RecordBackedList)map)
                       .forEach(RecordBackedList::save);
        }
        return changed;
    }
}
