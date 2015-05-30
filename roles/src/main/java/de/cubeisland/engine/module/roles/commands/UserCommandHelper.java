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
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.service.command.ContainerCommand;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.spongepowered.api.service.permission.context.Context;

import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

public class UserCommandHelper extends ContainerCommand
{
    protected final WorldManager worldManager;
    protected final Roles module;

    protected final String LISTELEM = "- " + ChatFormat.YELLOW + "%s";
    protected final String LISTELEM_VALUE = "- " + ChatFormat.YELLOW + "%s" + ChatFormat.WHITE + ": " + ChatFormat.GOLD + "%s";

    public UserCommandHelper(Roles module, WorldManager wm)
    {
        super(module);
        this.worldManager = wm;
        this.module = module;
    }

    protected Set<Context> toSet(Context context)
    {
        return "global".equals(context.getType()) ? GLOBAL_CONTEXT : Collections.singleton(context);
    }
}
