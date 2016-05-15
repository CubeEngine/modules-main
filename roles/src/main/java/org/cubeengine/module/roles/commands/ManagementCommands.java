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

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.service.subject.UserSubject;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.DARK_RED;
import static org.spongepowered.api.text.format.TextColors.GOLD;

@Command(name = "admin", desc = "Manages the module", alias = "manadmin")
public class ManagementCommands extends ContainerCommand
{
    private Roles module;
    private RolesPermissionService service;
    private I18n i18n;

    public ManagementCommands(CommandManager base, Roles module, RolesPermissionService service, I18n i18n)
    {
        super(base, Roles.class);
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

        service.getDefaultData().load();

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

    @Command(desc = "Searches for registered Permissions")
    public void findPermission(CommandSource sender, @Complete(PermissionCompleter.class) String permission)
    {
        PermissionDescription perm = service.getDescription(permission).orElse(null);
        if (perm == null)
        {
            i18n.sendTranslated(sender, NEGATIVE, "Permission {name} not found!", permission);
        }
        else
        {
            i18n.sendTranslated(sender, POSITIVE, "Permission {name} found:", permission);
            sender.sendMessage(perm.getDescription().toBuilder().color(GOLD).build());
            Map<Subject, Boolean> roles = perm.getAssignedSubjects(PermissionService.SUBJECTS_ROLE_TEMPLATE);
            if (!roles.isEmpty())
            {
                i18n.sendTranslated(sender, POSITIVE, "Permission is assigned to the following templates:");
                for (Entry<Subject, Boolean> entry : roles.entrySet())
                {
                    sender.sendMessage(Text.of("  - ", GOLD, entry.getKey().getIdentifier(), ": ",
                                                entry.getValue() ? DARK_GREEN : DARK_RED,
                                                entry.getValue() ? "true" : "false")); // TODO translate
                }
            }
        }
    }
}
