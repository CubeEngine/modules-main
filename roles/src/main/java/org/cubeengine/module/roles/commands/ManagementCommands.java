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
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

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
    public void reload(CommandSource context)
    {
        module.getConfiguration().reload();
        service.getGroupSubjects().reload();
        service.getUserSubjects().reload();

        service.getConfig().reload();
        // TODO remove cached data ; needed?
        i18n.sendTranslated(context, POSITIVE, "{text:Roles} reload complete!");
    }

    @Alias(value = "mansave")
    @Command(desc = "Overrides all configs with current settings")
    public void save(CommandSource context)
    {
        module.getConfiguration().save();
        for (Subject subject : service.getGroupSubjects().getAllSubjects())
        {
            if (subject instanceof RoleSubject)
            {
                ((RoleSubject)subject).getSubjectData().save(true);
            }
        }

        i18n.sendTranslated(context, POSITIVE, "{text:Roles} all configurations saved!");
    }
}
