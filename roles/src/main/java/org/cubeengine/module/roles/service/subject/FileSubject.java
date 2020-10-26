/*
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
package org.cubeengine.module.roles.service.subject;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.config.RoleConfig;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.collection.FileBasedCollection;
import org.cubeengine.module.roles.service.data.FileSubjectData;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;

import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

public class FileSubject extends BaseSubject<FileSubjectData>
{
    public static final String SEPARATOR = "|";
    private FileSubjectData data;

    public FileSubject(RolesPermissionService service, FileBasedCollection collection, RoleConfig config)
    {
        super(collection, service);
        this.data = new FileSubjectData(service, config, this);
    }

    @Override
    public FileSubjectData getSubjectData()
    {
        return this.data;
    }

    /**
     * Returns the internal UUID as String or the Subjects Identifier if it is not a RoleSubject
     * @param s the subject
     * @return the internal identifier
     */
    public static String getInternalIdentifier(SubjectReference s)
    {
        try
        {
            Subject subject = s.resolve().get();
            return subject instanceof FileSubject ? ((FileSubject)subject).getUUID().toString() : subject.getIdentifier();
        }
        catch (InterruptedException | ExecutionException e)
        {
            throw new IllegalStateException(e);
        }
    }

    public static int compare(Subject o1, Subject o2)
    {
        if (o1 instanceof FileSubject && o2 instanceof FileSubject) // Higher priority first
        {
            return -Integer.compare(((FileSubject) o1).getSubjectData().getConfig().priority.value,
                                    ((FileSubject) o2).getSubjectData().getConfig().priority.value);
        }
        if (o1 instanceof FileSubject)
        {
            return 1;
        }
        if (o2 instanceof FileSubject)
        {
            return -1;
        }
        if (o1 == null && o2 == null)
        {
            return 0;
        }
        if (o1 == null)
        {
            return -1;
        }
        if (o2 == null)
        {
            return 1;
        }
        String i1 = o1.getContainingCollection().getIdentifier() + o1.getIdentifier();
        String i2 = o2.getContainingCollection().getIdentifier() + o2.getIdentifier();
        return i1.compareTo(i2);
    }

    protected UUID getUUID()
    {
        return getSubjectData().getConfig().identifier;
    }

    @Override
    public String getIdentifier()
    {
        return getSubjectData().getConfig().roleName;
    }

    @Override
    public Optional<String> getFriendlyIdentifier()
    {
        return Optional.of(getSubjectData().getConfig().roleName);
    }

    @Override
    public Set<Context> getActiveContexts()
    {
        return GLOBAL_CONTEXT;
    }

    public boolean canAssignAndRemove(CommandCause source)
    {
        String perm = service.getPermissionManager().getBasePermission(Roles.class).getId();
        return source.hasPermission(perm + ".assign." + getIdentifier()); // TODO register permission + assign base
    }

    public void setPriorityValue(int value)
    {
        getSubjectData().getConfig().priority = Priority.getByValue(value);
        getSubjectData().getConfig().save(); // TODO async
    }

    public Priority prio()
    {
        return getSubjectData().getConfig().priority;
    }

    @Override
    public String toString() {
        return "RoleSubject: " + this.getIdentifier();
    }

    @Override
    public boolean isSubjectDataPersisted() {
        return true;
    }
}
