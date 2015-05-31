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
package de.cubeisland.engine.module.roles.commands;

import java.util.Collections;
import java.util.Set;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.service.command.ContainerCommand;
import de.cubeisland.engine.module.roles.Roles;
import org.spongepowered.api.service.permission.context.Context;

import static de.cubeisland.engine.module.core.util.ChatFormat.GOLD;
import static de.cubeisland.engine.module.core.util.ChatFormat.WHITE;
import static de.cubeisland.engine.module.core.util.ChatFormat.YELLOW;
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

    protected static Set<Context> toSet(Context context)
    {
        return "global".equals(context.getType()) ? GLOBAL_CONTEXT : Collections.singleton(context);
    }
}
