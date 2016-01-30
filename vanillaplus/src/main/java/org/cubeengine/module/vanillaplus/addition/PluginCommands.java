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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.service.command.CommandContext;
import org.spongepowered.api.Platform;
import org.spongepowered.api.plugin.PluginContainer;

public class PluginCommands
{
    @Command(desc = "Lists all loaded plugins")
    public void plugins(CommandSender context)
    {
        Collection<PluginContainer> plugins = game.getPluginManager().getPlugins();
        Set<Module> modules = this.module.getModularity().getModules();

        context.sendTranslated(NEUTRAL, "There are {amount} plugins and {amount} CubeEngine modules loaded:",
                               plugins.size(), modules.size());
        context.sendMessage(" ");
        context.sendMessage(" - " + ChatFormat.BRIGHT_GREEN + "CubeEngine" + ChatFormat.RESET + " (" + module.getInformation().getVersion() + ")");

        for (Module m : modules)
        {
            context.sendMessage("   - " + m.getInformation().getName() + ChatFormat.RESET + " (" + m.getInformation().getVersion() + ")");
        }

        plugins.stream().filter(p -> p.getInstance() != this.module)
               .forEach(p -> context.sendMessage(
                   " - " + ChatFormat.BRIGHT_GREEN + p.getName() + ChatFormat.RESET + " (" + p.getVersion()
                       + ")"));
    }

    @Command(desc = "Displays the version of the server or a given plugin")
    public void version(CommandContext context, @Optional String plugin, @Flag boolean source)
    {
        if (plugin == null)
        {
            Platform platform = game.getPlatform();
            context.sendTranslated(NEUTRAL, "This server is running {name#server} in version {input#version:color=INDIGO}", platform.getMinecraftVersion().getName(), platform.getVersion());
            context.sendTranslated(NEUTRAL, "Sponge API {text:version\\::color=WHITE} {input#version:color=INDIGO}", platform.getApiVersion());
            context.sendMessage(" ");
            context.sendTranslated(NEUTRAL,
                                   "Expanded and improved by {text:CubeEngine:color=BRIGHT_GREEN} version {input#version:color=INDIGO}",
                                   module.getInformation().getVersion());
            if (source)
            {
                ModuleCommands.showSourceVersion(context.getSource(), module.getInformation().getSourceVersion());
            }
            return;
        }
        context.ensurePermission(COMMAND_VERSION_PLUGINS);
        com.google.common.base.Optional<PluginContainer> instance = game.getPluginManager().getPlugin(plugin);
        if (!instance.isPresent())
        {
            List<PluginContainer> plugins = new ArrayList<>();
            for (PluginContainer container : game.getPluginManager().getPlugins())
            {
                if (container.getName().toLowerCase().startsWith(plugin.toLowerCase()))
                {
                    plugins.add(container);
                }
            }
            context.sendTranslated(NEGATIVE,
                                   "The given plugin doesn't seem to be loaded, have you typed it correctly (casing does matter)?");
            if (!plugins.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "You might want to try one of these:");
                for (PluginContainer p : plugins)
                {
                    context.sendMessage(" - " + p.getName());
                }
            }
            return;
        }
        context.sendTranslated(NEUTRAL, "{name#plugin} is currently running in version {input#version:color=INDIGO}.",
                               instance.get().getName(), instance.get().getVersion());
        context.sendMessage(" ");
        context.sendTranslated(NEUTRAL, "Plugin information:");
        context.sendMessage(" ");
        if (instance.get().getInstance() instanceof CoreModule && source)
        {
            ModuleCommands.showSourceVersion(context.getSource(), module.getInformation().getSourceVersion());
        }
        /* TODO if possible later get detailed descriptions
        context.sendTranslated(NEUTRAL, "Description: {input}", instance.getDescription().getDescription() == null ? "NONE" : instance.getDescription().getDescription());
        context.sendTranslated(NEUTRAL, "Website: {input}", instance.getDescription().getWebsite() == null ? "NONE" : instance.getDescription().getWebsite());
        context.sendTranslated(NEUTRAL, "Authors:");
        for (String author : instance.getDescription().getAuthors())
        {
            context.sendMessage("   - " + ChatFormat.AQUA + author);
        }
        */
    }

}
