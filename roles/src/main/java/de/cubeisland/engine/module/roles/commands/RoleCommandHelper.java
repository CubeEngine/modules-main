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

import org.bukkit.World;

import de.cubeisland.engine.core.command.CubeContext;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.world.WorldManager;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.role.Role;
import de.cubeisland.engine.module.roles.role.RoleProvider;
import de.cubeisland.engine.module.roles.role.RolesAttachment;
import de.cubeisland.engine.module.roles.role.RolesManager;

import static de.cubeisland.engine.core.util.ChatFormat.*;
import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.NEUTRAL;

public abstract class RoleCommandHelper extends ContainerCommand
{
    protected static final String GLOBAL_PREFIX = "g:";
    protected final RolesManager manager;
    protected final Roles module;
    protected final WorldManager worldManager;

    protected final String LISTELEM = "- " + YELLOW + "%s";
    protected final String LISTELEM_VALUE = "- " + YELLOW + "%s" + WHITE + ": " + GOLD + "%s";

    public RoleCommandHelper(Roles module)
    {
        super(module, "role", "Manage roles.");
        this.manager = module.getRolesManager();
        this.module = module;
        this.worldManager = module.getCore().getWorldManager();
    }

    protected World getWorld(CubeContext context)
    {
        World world;
        if (!context.hasNamed("in"))
        {
            CommandSender sender = context.getSender();
            if (sender instanceof User)
            {
                User user = (User)sender;
                world = user.attachOrGet(RolesAttachment.class,this.module).getWorkingWorld();
                if (world == null)
                {
                    world = user.getWorld();
                }
                else
                {
                    context.sendTranslated(NEUTRAL, "You are using {world} as current world.", world);
                }
            }
            else
            {
                if (ManagementCommands.curWorldOfConsole == null)
                {
                    context.sendTranslated(NEGATIVE, "You have to provide a world with {text:in world}!");
                    context.sendTranslated(NEUTRAL, "Or you can define a default world with {text:/roles admin defaultworld <world>}");
                    return null;
                }
                world = ManagementCommands.curWorldOfConsole;
                context.sendTranslated(NEUTRAL, "You are using {world} as current world.", world);
            }
        }
        else
        {
            world = context.getArg("in");
            if (world == null)
            {
                context.sendTranslated(NEGATIVE, "World {input} not found!", context.getString("in"));
                return null;
            }
        }
        return world;
    }

    protected Role getRole(CubeContext context, RoleProvider provider, String name, World world)
    {
        Role role = provider.getRole(name);
        if (role == null)
        {
            if (world == null)
            {
                context.sendTranslated(NEGATIVE, "Could not find the global role {name}.", name);
                return null;
            }
            else
            {
                context.sendTranslated(NEGATIVE, "Could not find the role {name} in {world}", name, world);
                return null;
            }
        }
        return role;
    }
}