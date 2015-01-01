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

import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.role.RolesAttachment;

import static de.cubeisland.engine.command.parameter.property.Requirement.OPTIONAL;
import static de.cubeisland.engine.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;

@Command(name = "admin", desc = "Manages the module", alias = "manadmin")
public class ManagementCommands extends CommandContainer
{
    private Roles module;

    public ManagementCommands(Roles module)
    {
        super(module);
        this.module = module;
    }

    @Alias(value = "manload")
    @Command(desc = "Reloads all roles from config")
    public void reload(CommandContext context)
    {
        module.getConfiguration().reload();
        module.getRolesManager().initRoleProviders();
        module.getRolesManager().recalculateAllRoles();
        context.sendTranslated(POSITIVE, "{text:Roles} reload getSuggestions!");
    }

    @Alias(value = "mansave")
    @Command(desc = "Overrides all configs with current settings")
    public void save(CommandContext context)
    {
        // database is up to date so only saving configs
        module.getConfiguration().save();
        module.getRolesManager().saveAll();
        context.sendTranslated(POSITIVE, "{text:Roles} all configurations saved!");
    }

    public static World curWorldOfConsole = null;

    @Command(desc = "Sets or resets the current default world")
    @Params(positional = @Param(req = OPTIONAL, label = "world", type = World.class))
    public void defaultworld(CommandContext context)
    {
        World world = context.get(0);
        if (world == null)
        {
            context.sendTranslated(NEUTRAL, "Current world for roles resetted!");
        }
        else
        {
            context.sendTranslated(POSITIVE, "All your roles commands will now have {world} as default world!", world);
        }
        CommandSender sender = context.getSource();
        if (sender instanceof User)
        {
            ((User)sender).get(RolesAttachment.class).setWorkingWorld(world);
            return;
        }
        curWorldOfConsole = world;
    }

    // TODO lookup permissions
}
