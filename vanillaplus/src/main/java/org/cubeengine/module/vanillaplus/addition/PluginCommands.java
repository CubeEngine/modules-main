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
import java.util.List;
import java.util.stream.Collectors;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.LibCube;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.command.exception.PermissionDeniedException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.Platform.Component.API;
import static org.spongepowered.api.Platform.Component.IMPLEMENTATION;
import static org.spongepowered.api.text.format.TextColors.GRAY;
import static org.spongepowered.api.text.format.TextColors.GREEN;
import static org.spongepowered.api.text.format.TextColors.RESET;

public class PluginCommands extends PermissionContainer
{
    private I18n i18n;
    private ModuleManager mm;

    public PluginCommands(I18n i18n, PermissionManager pm, ModuleManager mm)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
        this.mm = mm;
    }

    private final Permission COMMAND_VERSION_PLUGINS = register("command.version.plugins", "", null);

    @Command(desc = "Lists all loaded plugins")
    public void plugins(CommandSource context)
    {
        Collection<PluginContainer> plugins = new ArrayList<>(Sponge.getPluginManager().getPlugins());
        Collection<PluginContainer> modules = new ArrayList<>(mm.getModulePlugins().values());
        plugins.removeAll(modules);
        PluginContainer core = mm.getPlugin(LibCube.class).get();
        modules.remove(core);

        PaginationList.Builder builder = PaginationList.builder().header(i18n
                .translate(context, NEUTRAL, "Plugin-List ({amount})", plugins.size() + 1));

        context.sendMessage(Text.EMPTY);

        // TODO no pagination for console and hover id for player
        List<Text> list = new ArrayList<>();
        list.add(Text.of(" - ", GREEN, "CubeEngine", " " ,GRAY, core.getId(), RESET, " (" + getVersionOf(core) + ") ",
                i18n.translate(context, NEUTRAL, "with {amount} Modules:", modules.size())));

        for (PluginContainer m : modules)
        {
            list.add(Text.of("   - ", GREEN, simplifyCEName(m.getName()), " ", GRAY, m.getId(), RESET, " (" + getVersionOf(m) + ")"));
        }

        for (PluginContainer plugin : plugins)
        {
            list.add(Text.of(" - ", GREEN, plugin.getName(), " ", GRAY, plugin.getId(), RESET, " (" + getVersionOf(plugin) + ")"));
        }

        builder.contents(list).sendTo(context);
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
        return core.getVersion().map(this::simplifyVersion).orElse("unknown");
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
    public void version(CommandSource context, @Optional String plugin)
    {
        if (plugin == null)
        {
            Platform platform = Sponge.getGame().getPlatform();
            PluginContainer impl = platform.getContainer(IMPLEMENTATION);
            switch (platform.getType())
            {
                case CLIENT:
                    i18n.send(context, NEUTRAL, "This client is running {name#server} {name#version:color=INDIGO} {name#version:color=INDIGO}",
                                        impl.getName(), platform.getMinecraftVersion().getName(), impl.getVersion().orElse(""));
                    break;
                case SERVER:
                    i18n.send(context, NEUTRAL, "This server is running {name#server} {name#version:color=INDIGO} {name#version:color=INDIGO}",
                                        impl.getName(), platform.getMinecraftVersion().getName(), impl.getVersion().orElse(""));
                    break;
                case UNKNOWN:
                    i18n.send(context, NEUTRAL, "Unknown platform running {name#server} {name#version:color=INDIGO} {name#version:color"
                                    + "=INDIGO}",
                                        impl.getName(), platform.getMinecraftVersion().getName(), impl.getVersion().orElse(""));
            }

            i18n.send(context, NEUTRAL, "Sponge API: {input#version:color=INDIGO}", platform.getContainer(API).getVersion().orElse("unknown"));
            context.sendMessage(Text.EMPTY);

            i18n.send(context, NEUTRAL, "Expanded and improved by {text:CubeEngine:color=BRIGHT_GREEN} version {input#version:color=INDIGO}", this.mm.getPlugin(LibCube.class).get().getVersion().orElse("unknown"));
            return;
        }
        if (context.hasPermission(COMMAND_VERSION_PLUGINS.getId()))
        {
            throw new PermissionDeniedException(COMMAND_VERSION_PLUGINS);
        }
        java.util.Optional<PluginContainer> instance = Sponge.getPluginManager().getPlugin(plugin);
        if (!instance.isPresent())
        {
            List<PluginContainer> plugins = Sponge.getPluginManager().getPlugins().stream()
                  .filter(container -> container.getName().toLowerCase().startsWith(plugin.toLowerCase()))
                  .collect(Collectors.toList());
            i18n.send(context, NEGATIVE, "The given plugin doesn't seem to be loaded, have you typed it correctly (casing does matter)?");
            if (!plugins.isEmpty())
            {
                i18n.send(context, NEGATIVE, "You might want to try one of these:");
                for (PluginContainer p : plugins)
                {
                    context.sendMessage(Text.of(" - " + p.getName()));
                }
            }
            return;
        }
        i18n.send(context, NEUTRAL, "{name#plugin} is currently running in version {input#version:color=INDIGO}.",
                               instance.get().getName(), instance.get().getVersion());
        context.sendMessage(Text.EMPTY);
        i18n.send(context, NEUTRAL, "Plugin information:");
        context.sendMessage(Text.EMPTY);
    }

}
