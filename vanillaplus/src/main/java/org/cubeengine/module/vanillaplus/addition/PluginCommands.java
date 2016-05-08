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
package org.cubeengine.module.vanillaplus.addition;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import de.cubeisland.engine.modularity.core.LifeCycle;
import de.cubeisland.engine.modularity.core.Modularity;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.command.exception.PermissionDeniedException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.text.format.TextColors.GREEN;
import static org.spongepowered.api.text.format.TextColors.RESET;

public class PluginCommands extends PermissionContainer
{
    private I18n i18n;
    private Modularity modularity;
    private PluginContainer plugin;

    public PluginCommands(I18n i18n, PermissionManager pm, Modularity modularity, PluginContainer plugin)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
        this.modularity = modularity;
        this.plugin = plugin;
    }

    private final Permission COMMAND_VERSION_PLUGINS = register("command.version.plugins", "", null);

    @Command(desc = "Lists all loaded plugins")
    public void plugins(CommandSource context)
    {
        Collection<PluginContainer> plugins = Sponge.getPluginManager().getPlugins();
        Set<LifeCycle> modules = modularity.getModules();

        i18n.sendTranslated(context, NEUTRAL, "There are {amount} plugins and {amount} CubeEngine modules loaded:", plugins.size(), modules.size());
        context.sendMessage(Text.EMPTY);
        context.sendMessage(Text.of(" - ", GREEN, "CubeEngine", RESET, " (" + plugin.getVersion().orElse("unknown") + ")"));

        for (LifeCycle m : modules)
        {
            String name = m.getInformation() instanceof ModuleMetadata ? ((ModuleMetadata)m.getInformation()).getName() : m.getInformation().getClassName();
            context.sendMessage(Text.of("   - ", GREEN, name, RESET, " (" + m.getInformation().getVersion() + ")"));
        }

        for (PluginContainer plugin : plugins)
        {
            if (plugin == this.plugin)
            {
                continue;
            }
            context.sendMessage(Text.of(" - ", GREEN, plugin.getName(), RESET, " (" + plugin.getVersion().orElse("unknown") + ")"));
        }
    }

    @Command(desc = "Displays the version of the server or a given plugin")
    public void version(CommandSource context, @Optional String plugin)
    {
        if (plugin == null)
        {
            Platform platform = Sponge.getGame().getPlatform();
            switch (platform.getType())
            {
                case CLIENT:
                    i18n.sendTranslated(context, NEUTRAL, "This client is running {name#server} {name#version:color=INDIGO} {name#version:color=INDIGO}",
                                        platform.getImplementation().getName(), platform.getMinecraftVersion().getName(), platform.getImplementation().getVersion().orElse(""));
                    break;
                case SERVER:
                    i18n.sendTranslated(context, NEUTRAL, "This server is running {name#server} {name#version:color=INDIGO} {name#version:color=INDIGO}",
                                        platform.getImplementation().getName(), platform.getMinecraftVersion().getName(), platform.getImplementation().getVersion().orElse(""));
                    break;
                case UNKNOWN:
                    i18n.sendTranslated(context, NEUTRAL, "Unknown platform running {name#server} {name#version:color=INDIGO} {name#version:color=INDIGO}",
                                        platform.getImplementation().getName(), platform.getMinecraftVersion().getName(), platform.getImplementation().getVersion().orElse(""));
            }

            i18n.sendTranslated(context, NEUTRAL, "Sponge API: {input#version:color=INDIGO}", platform.getApi().getVersion().orElse("unknown"));
            context.sendMessage(Text.EMPTY);
            i18n.sendTranslated(context, NEUTRAL, "Expanded and improved by {text:CubeEngine:color=BRIGHT_GREEN} version {input#version:color=INDIGO}", this.plugin.getVersion().orElse("unknown"));
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
            i18n.sendTranslated(context, NEGATIVE, "The given plugin doesn't seem to be loaded, have you typed it correctly (casing does matter)?");
            if (!plugins.isEmpty())
            {
                i18n.sendTranslated(context, NEGATIVE, "You might want to try one of these:");
                for (PluginContainer p : plugins)
                {
                    context.sendMessage(Text.of(" - " + p.getName()));
                }
            }
            return;
        }
        i18n.sendTranslated(context, NEUTRAL, "{name#plugin} is currently running in version {input#version:color=INDIGO}.",
                               instance.get().getName(), instance.get().getVersion());
        context.sendMessage(Text.EMPTY);
        i18n.sendTranslated(context, NEUTRAL, "Plugin information:");
        context.sendMessage(Text.EMPTY);
    }

}
