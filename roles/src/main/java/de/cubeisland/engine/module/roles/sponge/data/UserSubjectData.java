/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
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
    public boolean save(boolean changed)
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
