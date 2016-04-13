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
package org.cubeengine.module.roles.service.subject;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.config.RoleConfig;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.collection.RoleCollection;
import org.cubeengine.module.roles.service.data.RoleSubjectData;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;

import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

public class RoleSubject extends BaseSubject<RoleSubjectData>
{
    public static final String SEPARATOR = "|";
    private Roles module;

    public RoleSubject(Roles module, RolesPermissionService service, RoleCollection collection, RoleConfig config)
    {
        super(collection, service, new RoleSubjectData(service, config));
        this.module = module;
    }

    /**
     * Returns the internal UUID as String or the Subjects Identifier if it is not a RoleSubject
     * @param s the subject
     * @return the internal identifier
     */
    public static String getInternalIdentifier(Subject s)
    {
        return s instanceof RoleSubject ? ((RoleSubject)s).getUUID().toString() : s.getIdentifier();
    }

    public static int compare(RoleSubject o1, RoleSubject o2)
    {
        if (o1 != null && o2 != null)
        {
            // Higher priority first
            return -Integer.compare(o1.getSubjectData().getConfig().priority.value, o2.getSubjectData().getConfig().priority.value);
        }
        return 1;
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
    public Optional<CommandSource> getCommandSource()
    {
        return Optional.empty();
    }

    @Override
    public Set<Context> getActiveContexts()
    {
        return GLOBAL_CONTEXT;
    }

    public boolean canAssignAndRemove(CommandSource source)
    {
        String perm = module.getModularity().provide(PermissionManager.class).getModulePermission(module).getId();
        return source.hasPermission(perm + ".assign." + getIdentifier());
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


}
