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

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.World;

import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "admin", desc = "Manages the module", alias = "manadmin")
public class ManagementCommands extends ContainerCommand
{
    private Roles module;
    private RolesPermissionService service;
    private I18n i18n;

    public ManagementCommands(Roles module, RolesPermissionService service, I18n i18n)
    {
        super(module);
        this.module = module;
        this.service = service;
        this.i18n = i18n;
    }

    @Alias(value = "manload")
    @Command(desc = "Reloads all roles from config")
    public void reload(CommandContext context)
    {
        module.getConfiguration().reload();
        service.getGroupSubjects().reload();
        service.getUserSubjects().reload();
        // TODO remove cached data
        context.sendTranslated(POSITIVE, "{text:Roles} reload complete!");
    }

    @Alias(value = "mansave")
    @Command(desc = "Overrides all configs with current settings")
    public void save(CommandContext context)
    {
        // database is up to date so only saving configs
        module.getConfiguration().save();
        // TODO save RoleSubject Configurations
        context.sendTranslated(POSITIVE, "{text:Roles} all configurations saved!");
    }

    public static World curWorldOfConsole = null;

    @Command(desc = "Sets or resets the current default world")
    public void defaultworld(CommandSource source, @Optional World world)
    {
        if (world == null)
        {
            i18n.sendTranslated(source, NEUTRAL, "Current world for roles resetted!");
        }
        else
        {
            i18n.sendTranslated(source, POSITIVE, "All your roles commands will now have {world} as default world!", world);
        }

        if (source instanceof Player)
        {
            SubjectData data = source.getTransientSubjectData();
            if (data instanceof OptionSubjectData)
            {
                if (world == null)
                {
                    // TODO remove? ((OptionSubjectData)data).setOption(source.getActiveContexts(), "CubeEngine:roles:active-world", null);
                }
                else
                {
                    ((OptionSubjectData)data).setOption(source.getActiveContexts(), "CubeEngine:roles:active-world", world.getName());
                }
            }
            return;
        }
        curWorldOfConsole = world;
    }

    // TODO lookup permissions
}
