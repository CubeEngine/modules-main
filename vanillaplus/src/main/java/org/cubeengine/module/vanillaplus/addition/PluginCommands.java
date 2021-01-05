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
package org.cubeengine.module.vanillaplus.addition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.LibCube;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.metadata.PluginMetadata;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.Platform.Component.API;
import static org.spongepowered.api.Platform.Component.GAME;
import static org.spongepowered.api.Platform.Component.IMPLEMENTATION;

@Singleton
public class PluginCommands extends PermissionContainer
{
    private I18n i18n;
    private ModuleManager mm;

    @Inject
    public PluginCommands(I18n i18n, PermissionManager pm, ModuleManager mm)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
        this.mm = mm;
    }

    private final Permission COMMAND_VERSION_PLUGINS = register("command.version.plugins", "", null);

    @Command(desc = "Lists all loaded plugins")
    public void plugins(CommandCause context)
    {
        List<PluginContainer> plugins = new ArrayList<>(Sponge.getPluginManager().getPlugins());
        List<PluginContainer> modules = new ArrayList<>(mm.getModulePlugins().values());
        plugins.removeAll(modules);
        PluginContainer core = mm.getPlugin(LibCube.class).get();
        modules.remove(core);

        PaginationList.Builder builder = PaginationList.builder().header(i18n
                .translate(context, NEUTRAL, "Plugin-List ({amount})", plugins.size() + 1));

        context.sendMessage(Identity.nil(), Component.empty());

        // TODO no pagination for console and hover id for player
        List<Component> list = new ArrayList<>();

        final PluginContainer game = Sponge.getGame().getPlatform().getContainer(GAME);
        final PluginContainer api = Sponge.getGame().getPlatform().getContainer(API);
        final PluginContainer impl = Sponge.getGame().getPlatform().getContainer(IMPLEMENTATION);
        plugins.remove(game);
        plugins.remove(api);
        plugins.remove(impl);
        Collections.sort(plugins, Comparator.comparing(p -> p.getMetadata().getName().orElse(p.getMetadata().getId())));
        Collections.sort(modules, Comparator.comparing(p -> p.getMetadata().getName().orElse(p.getMetadata().getId())));

        for (PluginContainer plugin : plugins)
        {
            final PluginMetadata meta = plugin.getMetadata();
            list.add(Component.text(" - ").append(Component.text(meta.getName().orElse(meta.getId()), NamedTextColor.GREEN))
                         .append(Component.space())
                         .append(Component.text(meta.getId(), NamedTextColor.GRAY))
                         .append(Component.text(" (" + getVersionOf(plugin) + ")")));
        }

        list.add(Component.empty());

        list.add(Component.text(" - ").append(Component.text("CubeEngine", NamedTextColor.GREEN))
                     .append(Component.space())
                     .append(Component.text(core.getMetadata().getId(), NamedTextColor.GRAY))
                     .append(Component.space())
                     .append(Component.text("(" + getVersionOf(core) + ")"))
                     .append(i18n.translate(context, NEUTRAL, " with {amount} Modules:", modules.size())));

        for (PluginContainer module : modules)
        {
            final PluginMetadata meta = module.getMetadata();
            list.add(Component.text("  - ").append(Component.text(simplifyCEName(meta.getName().orElse(meta.getId())), NamedTextColor.YELLOW))
                         .append(Component.space())
                         .append(Component.text(meta.getId(), NamedTextColor.GRAY))
                         .append(Component.space())
                         .append(Component.text("(" + getVersionOf(module) + ")")));
        }

        builder.contents(list).sendTo(context.getAudience());
    }

    private String simplifyCEName(String name)
    {
        if (name.startsWith("CubeEngine - "))
        {
            name = name.substring(13);
        }
        return name;
    }

    private String getVersionOf(PluginContainer core)
    {
        return core.getMetadata().getVersion();
    }

    private String simplifyVersion(String version)
    {
        if (version.endsWith("SNAPSHOT"))
        {
            version = version.substring(0, version.length() - 7);
        }
        return version;
    }

    @Command(desc = "Displays the version of the server or a given plugin")
    public void version(CommandCause context, @Option String plugin) // TODO completer
    {
        if (plugin == null)
        {
            Platform platform = Sponge.getGame().getPlatform();
            PluginContainer impl = platform.getContainer(IMPLEMENTATION);
            final PluginMetadata meta = impl.getMetadata();
            switch (platform.getType())
            {
                case CLIENT:
                    i18n.send(context, NEUTRAL, "This client is running {name#server} {name#version:color=INDIGO} {name#version:color=INDIGO}",
                                        meta.getName().orElse(meta.getId()), platform.getMinecraftVersion().getName(), meta.getVersion());
                    break;
                case SERVER:
                    i18n.send(context, NEUTRAL, "This server is running {name#server} {name#version:color=INDIGO} {name#version:color=INDIGO}",
                                        meta.getName().orElse(meta.getId()), platform.getMinecraftVersion().getName(), meta.getVersion());
                    break;
                case UNKNOWN:
                    i18n.send(context, NEUTRAL, "Unknown platform running {name#server} {name#version:color=INDIGO} {name#version:color"
                                    + "=INDIGO}",
                                        meta.getName().orElse(meta.getId()), platform.getMinecraftVersion().getName(), meta.getVersion());
            }

            i18n.send(context, NEUTRAL, "Sponge API: {input#version:color=INDIGO}", platform.getContainer(API).getMetadata().getVersion());
            context.sendMessage(Identity.nil(), Component.empty());

            i18n.send(context, NEUTRAL, "with {text:CubeEngine:color=BRIGHT_GREEN} version {input#version:color=INDIGO}", this.mm.getPlugin(LibCube.class).get().getMetadata().getVersion());
            return;
        }
        if (!COMMAND_VERSION_PLUGINS.check(context))
        {
            i18n.send(context, NEGATIVE, "You don't have permissions to show plugin versions");
            return;
        }
        java.util.Optional<PluginContainer> instance = Sponge.getPluginManager().getPlugin(plugin);
        if (!instance.isPresent())
        {
            List<PluginContainer> plugins = Sponge.getPluginManager().getPlugins().stream()
                  .filter(container -> container.getMetadata().getName().orElse(container.getMetadata().getId()).toLowerCase().startsWith(plugin.toLowerCase()))
                  .collect(Collectors.toList());
            i18n.send(context, NEGATIVE, "The given plugin doesn't seem to be loaded, have you typed it correctly (casing does matter)?");
            if (!plugins.isEmpty())
            {
                i18n.send(context, NEGATIVE, "You might want to try one of these:");
                for (PluginContainer p : plugins)
                {
                    context.sendMessage(Identity.nil(), Component.text(" - " + p.getMetadata().getName()));
                }
            }
            return;
        }
        final PluginMetadata meta = instance.get().getMetadata();
        i18n.send(context, NEUTRAL, "{name#plugin} is currently running in version {input#version:color=INDIGO}.",
                  meta.getName().orElse(meta.getId()), meta.getVersion());
        context.sendMessage(Identity.nil(), Component.empty());
// TODO
        //        i18n.send(context, NEUTRAL, "Plugin information:");
//        context.sendMessage(Identity.nil(), Component.empty());
    }

}
