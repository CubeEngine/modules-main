/*
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

import java.time.Duration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import com.google.inject.Inject;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Parser;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "admin", desc = "Manages the module", alias = "manadmin")
public class ManagementCommands extends DispatcherCommand
{
    private Roles module;
    private RolesPermissionService service;
    private I18n i18n;
    private final TaskManager taskManager;

    @Inject
    public ManagementCommands(Roles module, RolesPermissionService service, I18n i18n, TaskManager taskManager)
    {
        super(Roles.class);
        this.module = module;
        this.service = service;
        this.i18n = i18n;
        this.taskManager = taskManager;
    }

    @Alias(value = "manload")
    @Command(desc = "Reloads all roles from config")
    public void reload(CommandCause context)
    {
        service.fullReload();

        i18n.send(context, POSITIVE, "{text:Roles} reload complete!");
    }

    @Alias(value = "mansave")
    @Command(desc = "Overrides all configs with current settings")
    public void save(CommandCause context)
    {
        module.getConfiguration().save();
        for (Subject subject : service.groupSubjects().loadedSubjects())
        {
            if (subject instanceof FileSubject)
            {
                ((FileSubject)subject).subjectData().save(CompletableFuture.completedFuture(true));
            }
        }

        i18n.send(context, POSITIVE, "{text:Roles} all configurations saved!");
    }

    @Alias(value = "mandebug")
    @Command(desc = "Toggles debug mode")
    public void debug(CommandCause context, @Option Integer seconds)
    {
        RolesUtil.debug = !RolesUtil.debug;
        if (RolesUtil.debug)
        {
            if (seconds != null)
            {
                seconds = seconds > 60 ? 60 : seconds < 0 ? 1 : seconds; // Min 1 Max 60
                i18n.send(context, POSITIVE, "Debug enabled for {number} seconds", seconds);

                taskManager.runTaskDelayed(() -> RolesUtil.debug = false, Duration.ofSeconds(seconds));
            }
            else
            {
                i18n.send(context, POSITIVE, "Debug enabled");
            }
        }
        else
        {
            i18n.send(context, POSITIVE, "Debug disabled");
        }
    }

    @Command(desc = "Searches for registered Permissions")
    public void findPermission(CommandCause sender, @Parser(completer = PermissionCompleter.class) String permission)
    {
        PermissionDescription perm = service.description(permission).orElse(null);
        if (perm == null)
        {
            i18n.send(sender, NEGATIVE, "Permission {name} not found!", permission);
        }
        else
        {
            i18n.send(sender, POSITIVE, "Permission {name} found:", permission);
            if (perm.description().isPresent())
            {
                sender.sendMessage(Identity.nil(), perm.description().get().color(NamedTextColor.GOLD));
            }
            Map<? extends Subject, Boolean> roles = perm.assignedSubjects(PermissionService.SUBJECTS_ROLE_TEMPLATE);
            if (!roles.isEmpty())
            {
                i18n.send(sender, POSITIVE, "Permission is assigned to the following templates:");
                for (Entry<? extends Subject, Boolean> entry : roles.entrySet())
                {
                    Component.text().append(Component.text("  - "))
                             .append(Component.text(entry.getKey().identifier() + ": ", NamedTextColor.GOLD))
                             .append(Component.text(entry.getValue(), entry.getValue() ? NamedTextColor.DARK_GREEN : NamedTextColor.DARK_RED));
                    // TODO translate entry.getValue true/false
                }
            }
        }
    }
}
