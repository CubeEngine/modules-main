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
package de.cubeisland.engine.module.roles.sponge.subject;

import java.util.Set;
import com.google.common.base.Optional;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.config.Priority;
import de.cubeisland.engine.module.roles.config.RoleConfig;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.data.RoleSubjectData;
import de.cubeisland.engine.service.command.CommandSender;
import de.cubeisland.engine.service.permission.Permission;
import de.cubeisland.engine.service.permission.PermissionManager;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.command.CommandSource;

import static java.util.Collections.singleton;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

public class RoleSubject extends BaseSubject implements Comparable<RoleSubject>
{
    public static final String SEPARATOR = "|";
    private final RoleSubjectData data;
    private final String roleName;
    private final Set<Context> contexts;
    private Roles module;
    private Context context;

    public RoleSubject(Roles module, RolesPermissionService service, RoleConfig config, Context context, PermissionManager manager)
    {
        super(service.getGroupSubjects(), manager);
        this.module = module;
        this.context = context;
        this.contexts = "global".equals(context.getType()) ? GLOBAL_CONTEXT : singleton(context);
        this.data = new RoleSubjectData(service, config, context);
        this.roleName = "role:" + context.getKey() + SEPARATOR +  (context.getName().isEmpty() ? "" : context.getName() + SEPARATOR) + config.roleName;
    }

    @Override
    public RoleSubjectData getSubjectData()
    {
        return data;
    }

    @Override
    public String getIdentifier()
    {
        return roleName;
    }

    @Override
    public Optional<CommandSource> getCommandSource()
    {
        return Optional.absent();
    }

    @Override
    public Set<Context> getActiveContexts()
    {
        return contexts;
    }

    @Override
    public int compareTo(RoleSubject o)
    {
        // Higher priority first
        return -Integer.compare(data.getConfig().priority.value, o.data.getConfig().priority.value);
    }

    public String getName()
    {
        return roleName.substring(roleName.lastIndexOf("|") + 1);
    }

    public boolean canAssignAndRemove(CommandSender source)
    {
        Permission perm = module.getProvided(Permission.class);
        perm = perm.childWildcard(context.getType());
        if (!context.getName().isEmpty())
        {
            perm = perm.childWildcard(context.getName());
        }
        return source.hasPermission(perm.child(roleName).getFullName());
    }

    public void setPriorityValue(int value)
    {
        data.getConfig().priority = Priority.getByValue(value);
        data.getConfig().save(); // TODO async
    }
}
