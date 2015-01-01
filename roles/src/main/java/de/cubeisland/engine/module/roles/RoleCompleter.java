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
package de.cubeisland.engine.module.roles;

import java.util.ArrayList;
import java.util.List;

import de.cubeisland.engine.command.CommandInvocation;
import de.cubeisland.engine.command.completer.Completer;
import de.cubeisland.engine.core.CubeEngine;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.roles.commands.ManagementCommands;
import de.cubeisland.engine.module.roles.role.Role;
import de.cubeisland.engine.module.roles.role.RolesAttachment;

public class RoleCompleter implements Completer
{
    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        // TODO use token??
        Roles module = CubeEngine.getCore().getModuleManager().getModule(Roles.class);
        final CommandSender sender = (CommandSender)invocation.getCommandSource();
        List<String> roles = new ArrayList<>();
        if (sender instanceof User)
        {
            User user = (User)sender;
            if (user.get(RolesAttachment.class).getWorkingWorld() != null)
            {
                for (Role role : module.getRolesManager().getProvider(user.get(RolesAttachment.class).getWorkingWorld()).getRoles())
                {
                    roles.add(role.getName());
                }
            }
            else
            {
                for (Role role : module.getRolesManager().getProvider(user.getWorld()).getRoles())
                {
                    roles.add(role.getName());
                }
            }
        }
        else
        {
            if (ManagementCommands.curWorldOfConsole != null)
            {
                for (Role role : module.getRolesManager().getProvider(ManagementCommands.curWorldOfConsole).getRoles())
                {
                    roles.add(role.getName());
                }
            }
        }
        return roles;
    }
}
