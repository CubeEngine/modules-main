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
package org.cubeengine.module.roles.sponge.subject;

import java.util.Optional;
import java.util.Set;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.config.RoleConfig;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.collection.RoleCollection;
import org.cubeengine.module.roles.sponge.data.RoleSubjectData;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;

import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

public class RoleSubject extends BaseSubject<RoleSubjectData> implements Comparable<RoleSubject>
{
    public static final String SEPARATOR = "|";
    private final String identifier;
    private Roles module;

    public RoleSubject(Roles module, RolesPermissionService service, RoleCollection collection, RoleConfig config)
    {
        super(collection, service, new RoleSubjectData(service, config));
        this.module = module;
        this.identifier = "role:" + config.roleName;
    }

    @Override
    public String getIdentifier()
    {
        return identifier;
    }

    public String getName()
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

    @Override
    public int compareTo(RoleSubject o)
    {
        // Higher priority first
        return -Integer.compare(getSubjectData().getConfig().priority.value, o.getSubjectData().getConfig().priority.value);
    }

    public boolean canAssignAndRemove(CommandSource source, Context context)
    {
        String perm = module.getModularity().provide(PermissionManager.class).getModulePermission(module).getId();
        perm += "." + context.getType() + "." + context.getName();
        if (!perm.endsWith(".")) // in case of global (or no context name)
        {
            perm += ".";
        }
        perm += identifier;
        return source.hasPermission(perm);
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
