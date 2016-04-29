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
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
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

public class PluginCommands extends PermissionContainer<VanillaPlus>
{
    private I18n i18n;

    public PluginCommands(I18n i18n, VanillaPlus module)
    {
        super(module);
        this.i18n = i18n;
    }

    private final PermissionDescription COMMAND_VERSION_PLUGINS = register("command.version.plugins", "", null);

    @Command(desc = "Lists all loaded plugins")
    public void plugins(CommandSource context)
    {
        Collection<PluginContainer> plugins = Sponge.getPluginManager().getPlugins();
        Set<LifeCycle> modules = this.module.getModularity().getModules();

        i18n.sendTranslated(context, NEUTRAL, "There are {amount} plugins and {amount} CubeEngine modules loaded:",
                            plugins.size(), modules.size());
        context.sendMessage(Text.EMPTY);
        context.sendMessage(Text.of(" - ", GREEN, "CubeEngine", RESET, " (" + module.getInformation().getVersion() + ")"));

        for (LifeCycle m : modules)
        {
            context.sendMessage(Text.of("   - ", m.getInformation().getIdentifier().name(), RESET, " (" + m.getInformation().getVersion() + ")"));
        }

        for (PluginContainer plugin : plugins)
        {
            context.sendMessage(Text.of(" - ", GREEN, plugin.getName(), ChatFormat.RESET, " (" + plugin.getVersion() + ")"));
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
                    i18n.sendTranslated(context, NEUTRAL, "This client is running {name#server}",
                                        platform.getMinecraftVersion().getName());
                    break;
                case SERVER:
                    i18n.sendTranslated(context, NEUTRAL, "This server is running {name#server}",
                                        platform.getMinecraftVersion().getName());
                    break;
                case UNKNOWN:
                    i18n.sendTranslated(context, NEUTRAL, "Unknown platform running {name#server}",
                                        platform.getMinecraftVersion().getName());
            }

            i18n.sendTranslated(context, NEUTRAL, "Sponge API: {input#version:color=INDIGO}", platform.getApi().getVersion());
            i18n.sendTranslated(context, NEUTRAL, "implemented by {input#name} {input#version:color=INDIGO}", platform.getImplementation().getName(), platform.getImplementation().getVersion());
            context.sendMessage(Text.EMPTY);
            i18n.sendTranslated(context, NEUTRAL, "Expanded and improved by {text:CubeEngine:color=BRIGHT_GREEN} version {input#version:color=INDIGO}", module.getInformation().getVersion());
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
