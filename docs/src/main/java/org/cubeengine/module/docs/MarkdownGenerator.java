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
package org.cubeengine.module.docs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.AnnotationCommandBuilder;
import org.cubeengine.libcube.service.command.AnnotationCommandBuilder.Requirements;
import org.cubeengine.libcube.service.command.HelpExecutor;
import org.cubeengine.libcube.service.permission.Permission;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.Command.Parameterized;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.Parameter.Subcommand;
import org.spongepowered.api.command.parameter.Parameter.Value;
import org.spongepowered.api.command.registrar.CommandRegistrar;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.metadata.PluginDependency;

import static java.util.stream.Collectors.toMap;

public class MarkdownGenerator implements Generator
{

    private static final String WHITESPACE = "&nbsp;";

    @Override
    public String generateList(Map<String, ModuleDocs> modules, Path modulePath, ModuleManager mm)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("# CubeEngine Documentation\n\n");
        sb.append("## Modules\n\n");

        ModuleDocs doc = modules.get("cubeengine-core");
        sb.append(" - [").append("Core").append("](modules/").append(doc.getModuleId()).append("/").append(doc.getModuleId()).append(".md)\n");

        List<ModuleDocs> list = new ArrayList<>(modules.values());
        list.sort((a, b) -> Boolean.compare(b.isOnOre(), a.isOnOre()));
        for (ModuleDocs module : list)
        {
            if (module == doc)
            {
                continue;
            }
            sb.append(" - [").append(module.getModuleName()).append("](modules/").append(module.getModuleId()).append("/").append(module.getModuleId()).append(".md)");
            if (module.isWIP())
            {
                sb.append(" - [WIP]");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    public String generate(Logger log, String id, String name, PluginContainer pc, Info info, Set<PermissionDescription> permissions,
                           Map<CommandMapping, Command.Parameterized> commands, Permission basePermission)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(name);
        if (info.workInProgress)
        {
            sb.append(" [WIP]");
        }

        sb.append("\n");
        sb.append(pc.getMetadata().getDescription().orElse(""));
        sb.append("\n");
        if (info.features.isEmpty())
        {
            log.warn("Missing Features for " + name + "(" + pc.getMetadata().getId() + ")!");
        }
        else
        {
            sb.append("\n## Features:\n");
            for (String feature : info.features)
            {
                sb.append(" - ").append(feature).append("\n");
            }
        }

        List<PluginDependency> plugDep = pc.getMetadata().getDependencies();
        if (plugDep.size() > 2) // ignore cubeengine-core and spongeapi
        {
            sb.append("\n## Dependencies:\n");
            for (PluginDependency dep : plugDep)
            {
                if (dep.getId().equals("cubeengine-core") || dep.getId().equals("spongeapi"))
                {
                    continue;
                }
                // TODO link to module or plugin on ore if possible?
                sb.append(" `").append(dep.getId()).append("`");
            }
            sb.append("\n");
        }

        if (!info.pages.isEmpty())
        {
            sb.append("\n## Pages:\n");
            for (Map.Entry<String, String> entry : info.pages.entrySet())
            {
                sb.append(" - [").append(entry.getKey()).append("]").append("(pages/").append(entry.getValue()).append(".md)\n");
            }
        }

        if (!info.config.isEmpty()) {
            sb.append("\n## Config:\n");
            for (Class clazz : info.config) {
                String simpleName = clazz.getSimpleName();
                sb.append(" - [").append(simpleName).append("]").append("(pages/").append("config-").append(simpleName.toLowerCase()).append(".md)\n");
            }
        }

        TreeMap<String, PermissionDescription> addPerms = new TreeMap<>(
            permissions.stream().collect(toMap(PermissionDescription::id, p -> p)));
        if (!commands.isEmpty())
        {

            sb.append("\n## Commands:").append("\n\n");

            sb.append("| Command | Description | Permission<br>`").append(basePermission.getId()).append(
                ".command.<perm>`").append(" |\n");
            sb.append("| --- | --- | --- |\n");

            commands.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().primaryAlias())).forEach(entry -> {
                generateCommandDocs(sb, addPerms, entry.getKey(), entry.getValue(), new Stack<>(), basePermission, true);
            });

            commands.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().primaryAlias())).forEach(entry -> {
                generateCommandDocs(sb, addPerms, entry.getKey(), entry.getValue(), new Stack<>(), basePermission, false);
            });
        }

        if (!addPerms.values().isEmpty())
        {
            sb.append("\n## Permissions:\n\n");
            sb.append("| Permission | Description |\n");
            sb.append("| --- | --- |\n");
            for (PermissionDescription perm : addPerms.values())
            {
                final String plainDesc = PlainComponentSerializer.plain().serialize(perm.description().orElse(Component.empty()));
                sb.append("| `").append(perm.id()).append("` | ").append(plainDesc).append(" |\n");
            }
        }

        if (info.onOre != null)
        {
            sb.append("\n## [Download on Ore](https://ore.spongepowered.org/CubeEngine/").append(info.onOre).append(")\n\n");
        }

        return sb.toString();
    }

    private void generateCommandDocs(StringBuilder sb, Map<String, PermissionDescription> addPerms, CommandMapping mapping,
                                     Command.Parameterized command, Stack<String> commandStack, Permission basePermission, boolean overview)
    {
        final PlainComponentSerializer plainSerializer = PlainComponentSerializer.plain();
        // TODO alias commands?
//        if (command.getExecutor().map(e -> e instanceof HelpExecutor).orElse(false))
//        {
//            return;
//        }
        String id = basePermission.getId() + ".command.";

        final List<Subcommand> subCommands = command.subcommands();
        subCommands.sort(Comparator.comparing(s -> s.aliases().iterator().next()));

        String primaryAlias = mapping.primaryAlias();
        if (primaryAlias.contains(":"))
        {
            primaryAlias = primaryAlias.substring(primaryAlias.indexOf(":") + 1);

        }
        if (overview)
        {
            commandStack.push("*" + primaryAlias + "*");
            String fullCmd = String.join(WHITESPACE, commandStack);
            sb.append("| [").append(fullCmd).append("]").append("(#").append(
                fullCmd.replace("*", "")
                       .replace(" ","-")
                       .replace(WHITESPACE, "").toLowerCase()).append(") | ");

            sb.append(plainSerializer.serialize(command.shortDescription(CommandCause.create()).orElse(Component.empty())).replace("\n", "<br>")).append(" | ");
//            CubeEngineCommand executor = command.getExecutor().filter(CubeEngineCommand.class::isInstance).map(CubeEngineCommand.class::cast).orElse(null);
            if (command.executionRequirements() instanceof AnnotationCommandBuilder.Requirements) {
                final String perm = ((Requirements)command.executionRequirements()).getPermission();
                sb.append("`").append(perm.replace(id, "")).append("` |\n");
            }


            commandStack.pop();
            commandStack.push("**" + primaryAlias + "**");
        }
        else
        {
            commandStack.push(primaryAlias);
            String fullCmd = String.join(WHITESPACE, commandStack);
            sb.append("\n#### ").append(fullCmd).append("  \n");

            sb.append(plainSerializer.serialize(command.shortDescription(CommandCause.create()).orElse(Component.empty()))).append("  \n");
            sb.append("**Usage:** `").append(getUsage(commandStack, command, plainSerializer)).append("`  \n");

            final Set<String> allAliases = new HashSet<>(mapping.allAliases());
            allAliases.remove(primaryAlias);
            if (commandStack.size() == 1)
            {
                allAliases.remove(mapping.plugin().getMetadata().getId() + ":" + primaryAlias);
            }
            if (!allAliases.isEmpty())
            {
                sb.append("**Alias:**");
                for (String alias : allAliases)
                {
                    // TODO alias registered on different dispatchers?
//                    String[] dispatcher = alias.getDispatcher();
//                    List<String> labels = new ArrayList<>();
//                    if (dispatcher == null)
//                    {
//                        labels.add(alias); // local alias
//                    }
//                    else
//                    {
//                        labels.addAll(Arrays.asList(alias.getDispatcher()));
//                        labels.add(alias.getName());
//                        labels.set(0, "/" + labels.get(0));
//                    }
//                    sb.append(" `").append(String.join(" ", labels)).append("`");
                    sb.append(" `").append(alias).append("`");
                }
                sb.append("  \n");
            }

            if (command.executionRequirements() instanceof AnnotationCommandBuilder.Requirements) {
                final String perm = ((Requirements)command.executionRequirements()).getPermission();
                sb.append("**Permission:** `").append(perm).append("`  \n");
                addPerms.remove(perm);
            }

            // TODO parameter permission?
            // TODO parameter description?
            // TODO Parser with default parameter descriptions
        }

        if (!overview)
        {
            StringBuilder subBuilder = new StringBuilder();
            for (Subcommand sub : subCommands)
            {
                // TODO is alias?
                if (!sub.command().executor().map(e -> e instanceof HelpExecutor).orElse(false))
                {
                    subBuilder.append(" `").append(sub.aliases().iterator().next()).append("`");
                }
            }

            if (subBuilder.length() != 0)
            {
                sb.append("**SubCommands:**").append(subBuilder.toString());
            }
            sb.append("  \n");
        }

        for (Subcommand sub : subCommands)
        {
            if (!sub.command().executor().map(e -> e instanceof HelpExecutor).orElse(false)
                && !sub.command().subcommands().isEmpty())
            {
                this.generateCommandDocs(sb, addPerms, new TmpCommandMapping(sub.aliases()), sub.command(), commandStack, basePermission, overview);
            }
        }

        commandStack.pop();
    }

    private String getUsage(Stack<String> commandStack, Parameterized command, PlainComponentSerializer plainSerializer)
    {
        StringBuilder usage = new StringBuilder(String.join(" ", commandStack));
        for (Parameter parameter : command.parameters())
        {
            if (parameter instanceof Parameter.Value)
            {
                String paramUsage = ((Value<?>)parameter).usage(CommandCause.create());
                if (!parameter.isOptional())
                {
                    paramUsage = "<" + paramUsage + ">";
                }
                usage.append(" ").append(paramUsage);
            }
            else
            {
                if (parameter.isOptional())
                {
                    usage.append("[?]");
                }
                else
                {
                    usage.append("<?>");
                }
            }
        }
        return usage.toString();
        // TODO usage from sponge is rather bad :/
//        return plainSerializer.serialize(command.getUsage(CommandCause.create()));
    }

    private static class TmpCommandMapping implements CommandMapping {

        private final Set<String> aliases;

        public TmpCommandMapping(Set<String> aliases)
        {
            this.aliases = aliases;
        }

        @Override
        public String primaryAlias()
        {
            return this.aliases.iterator().next();
        }

        @Override
        public Set<String> allAliases()
        {
            return this.aliases;
        }

        @Override
        public PluginContainer plugin()
        {
            return null;
        }

        @Override
        public CommandRegistrar<?> registrar()
        {
            return null;
        }
    }
}
