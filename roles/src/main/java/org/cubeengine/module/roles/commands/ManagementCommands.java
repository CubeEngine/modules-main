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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.DARK_RED;
import static org.spongepowered.api.text.format.TextColors.GOLD;

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.collection.FileBasedCollection;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Command(name = "admin", desc = "Manages the module", alias = "manadmin")
public class ManagementCommands extends ContainerCommand
{
    private Roles module;
    private RolesPermissionService service;
    private I18n i18n;
    private PluginContainer plugin;

    public ManagementCommands(CommandManager base, Roles module, RolesPermissionService service, I18n i18n, PluginContainer plugin)
    {
        super(base, Roles.class);
        this.module = module;
        this.service = service;
        this.i18n = i18n;
        this.plugin = plugin;
    }

    @Alias(value = "manload")
    @Command(desc = "Reloads all roles from config")
    public void reload(CommandSource context)
    {
        module.getConfiguration().reload();
        service.getConfig().reload();

        // TODO
        service.getLoadedCollections().values().stream()
                .filter(c -> c instanceof FileBasedCollection)
                .map(FileBasedCollection.class::cast)
                .forEach(FileBasedCollection::reload);

        service.getUserSubjects().reload();

        // TODO remove cached data ; needed?
        i18n.send(context, POSITIVE, "{text:Roles} reload complete!");
    }

    @Alias(value = "mansave")
    @Command(desc = "Overrides all configs with current settings")
    public void save(CommandSource context)
    {
        module.getConfiguration().save();
        for (Subject subject : service.getGroupSubjects().getLoadedSubjects())
        {
            if (subject instanceof FileSubject)
            {
                ((FileSubject)subject).getSubjectData().save(CompletableFuture.completedFuture(true));
            }
        }

        i18n.send(context, POSITIVE, "{text:Roles} all configurations saved!");
    }

    @Alias(value = "mandebug")
    @Command(desc = "Toggles debug mode")
    public void debug(CommandSource context, @Optional Integer seconds)
    {
        RolesUtil.debug = !RolesUtil.debug;
        if (RolesUtil.debug)
        {
            if (seconds != null)
            {
                seconds = seconds > 60 ? 60 : seconds < 0 ? 1 : seconds; // Min 1 Max 60
                i18n.send(context, POSITIVE, "Debug enabled for {number} seconds", seconds);
                Sponge.getScheduler().createTaskBuilder().delay(seconds, TimeUnit.SECONDS)
                        .execute(() -> RolesUtil.debug = false).submit(plugin);
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
    public void findPermission(CommandSource sender, @Complete(PermissionCompleter.class) String permission)
    {
        PermissionDescription perm = service.getDescription(permission).orElse(null);
        if (perm == null)
        {
            i18n.send(sender, NEGATIVE, "Permission {name} not found!", permission);
        }
        else
        {
            i18n.send(sender, POSITIVE, "Permission {name} found:", permission);
            if (perm.getDescription().isPresent())
            {
                sender.sendMessage(perm.getDescription().get().toBuilder().color(GOLD).build());
            }
            Map<Subject, Boolean> roles = perm.getAssignedSubjects(PermissionService.SUBJECTS_ROLE_TEMPLATE);
            if (!roles.isEmpty())
            {
                i18n.send(sender, POSITIVE, "Permission is assigned to the following templates:");
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
