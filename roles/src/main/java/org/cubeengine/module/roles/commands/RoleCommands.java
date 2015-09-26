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
package org.cubeengine.module.roles.commands;

import java.util.Collections;
import java.util.Set;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.service.command.ContainerCommand;
import org.spongepowered.api.service.permission.context.Context;

import static org.cubeengine.module.core.util.ChatFormat.GOLD;
import static org.cubeengine.module.core.util.ChatFormat.WHITE;
import static org.cubeengine.module.core.util.ChatFormat.YELLOW;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

@Command(name = "roles", desc = "Manages the roles")
public class RoleCommands extends ContainerCommand
{
    protected final static String LISTELEM = "- " + YELLOW + "%s";
    protected final static String LISTELEM_VALUE = "- " + YELLOW + "%s" + WHITE + ": " + GOLD + "%s";

    public RoleCommands(Roles module)
    {
        super(module);
    }

    public static Set<Context> toSet(Context context)
    {
        return "global".equals(context.getType()) ? GLOBAL_CONTEXT : Collections.singleton(context);
    }
}
