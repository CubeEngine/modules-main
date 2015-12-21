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
package org.cubeengine.module.roles.sponge.data;

import java.util.Map.Entry;
import java.util.Set;
import org.cubeengine.module.roles.RolesConfig;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.context.Context;

import static java.util.Collections.singleton;

public class DefaultSubjectData extends BaseSubjectData
{
    public DefaultSubjectData(RolesPermissionService service, RolesConfig config)
    {
        super(service);
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
    // TODO overrides change configuration
}
