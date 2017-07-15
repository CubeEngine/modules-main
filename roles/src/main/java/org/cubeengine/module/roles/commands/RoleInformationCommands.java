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
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.libcube.util.ContextUtil.toSet;
import static org.cubeengine.libcube.util.StringUtils.repeat;
import static org.cubeengine.module.roles.RolesUtil.permText;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.DARK_RED;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.GRAY;
import static org.spongepowered.api.text.format.TextColors.WHITE;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.converter.node.ListNode;
import org.cubeengine.converter.node.MapNode;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.StringNode;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.RolesUtil.FoundPermission;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.config.PermissionTreeConverter;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.Contextual;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Command(name = "role", desc = "Manage roles")
public class RoleInformationCommands extends ContainerCommand
{
    private RolesPermissionService service;
    private I18n i18n;

    public RoleInformationCommands(CommandManager base, RolesPermissionService service, I18n i18n)
    {
        super(base, Roles.class);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias(value = "listRoles")
    @Command(desc = "Lists all roles")
    public void list(CommandSource cContext)
    {
        List<Subject> roles = new ArrayList<>();
        for (Subject subject : service.getGroupSubjects().getAllSubjects())
        {
            roles.add(subject);
        }
        if (roles.isEmpty())
        {
            i18n.send(cContext, NEGATIVE, "There are no roles!");
            return;
        }
        i18n.send(cContext, POSITIVE, "The following roles are available:");
        String permTrans = i18n.getTranslation("permissions");
        String optTrans = i18n.getTranslation("options");
        String parentTrans = i18n.getTranslation("parents");
        Text permClick = i18n.translate(cContext, NEUTRAL, "Click to show {input}", permTrans);
        Text optClick = i18n.translate(cContext, NEUTRAL, "Click to show {input}", optTrans);
        Text parentClick = i18n.translate(cContext, NEUTRAL, "Click to show {input}", parentTrans);
        Text defaultClick = i18n.translate(cContext, NEUTRAL, "Click to toggle default");
        String defaultRole = i18n.getTranslation(cContext, "default");
        String noDefaultRole = i18n.getTranslation(cContext, "not default");
        roles.sort(Comparator.comparing(Contextual::getIdentifier));
        List<Subject> defaults = service.getDefaults().getSubjectData().getParents(Collections.emptySet());
        for (Subject r : roles)
        {
            cContext.sendMessage(Text.of("- ", GOLD, r.getIdentifier(), " ",
                    Text.of(GRAY, "[", YELLOW, permTrans, GRAY, "]").toBuilder()
                            .onHover(TextActions.showText(permClick))
                            .onClick(TextActions.runCommand("/roles role listpermission " + r.getIdentifier()))
                            .build(), " ",
                    Text.of(GRAY, "[", YELLOW, optTrans, GRAY, "]").toBuilder()
                            .onHover(TextActions.showText(optClick))
                            .onClick(TextActions.runCommand("/roles role listoption " + r.getIdentifier()))
                            .build(), " ",
                    Text.of(GRAY, "[", YELLOW, parentTrans, GRAY, "]").toBuilder()
                            .onHover(TextActions.showText(parentClick))
                            .onClick(TextActions.runCommand("/roles role listparent " + r.getIdentifier()))
                            .build(),
                    Text.of(GRAY, " (", YELLOW, defaults.contains(r) ? defaultRole : noDefaultRole, GRAY, ")").toBuilder()
                            .onHover(TextActions.showText(defaultClick))
                            .onClick(TextActions.runCommand("/roles role toggledefault " + r.getIdentifier()))
                            .build()
                    ));
        }
    }

    @Alias(value = "checkRPerm")
    @Command(alias = "checkPerm", desc = "Checks the permission in given role [in context]")
    public void checkPermission(CommandSource ctx, RoleSubject role,
                                @Complete(PermissionCompleter.class) String permission,
                                @Named("in") @Default Context context)
    {
        FoundPermission perm = RolesUtil.findPermission(service, role, permission, toSet(context));
        if (perm == null)
        {
            i18n.send(ctx, NEUTRAL, "The permission {txt} is not assigned to the role {role} in {context}.", permText(ctx, permission, service, i18n), role, context);
            return;
        }
        if (perm.value)
        {
            i18n.send(ctx, POSITIVE, "{txt#permission} is set to {text:true:color=DARK_GREEN} for the role {role} in {context}.", permText(ctx,permission, service, i18n), role, context);
        }
        else
        {
            i18n.send(ctx, NEGATIVE, "{txt#permission} is set to {text:false:color=DARK_RED} for the role {role} in {context}.", permText(ctx, permission, service, i18n), role, context);
        }
        i18n.send(ctx, NEUTRAL, "Permission inherited from:");
        i18n.send(ctx, NEUTRAL, "{txt#permission} in the role {name}!",
            permText(ctx, perm.permission, service, i18n), perm.subject.getIdentifier());
    }

    @Alias(value = "listRPerm")
    @Command(alias = "listPerm", desc = "Lists all permissions of given role [in context]")
    public void listPermission(CommandSource ctx, RoleSubject role, @Flag boolean all, @Named("in") @Default Context context)
    {
        i18n.send(ctx, NEUTRAL, "Permission list for {role}", role);
        Set<Context> contextSet = toSet(context);
        if (all)
        {
            listPermission(ctx, true, contextSet, RolesUtil.fillPermissions(role, contextSet, new TreeMap<>()));
        }
        else if (contextSet.isEmpty())
        {
            role.getSubjectData().getAllPermissions().entrySet()
                    .forEach(e -> listPermission(ctx, false, e.getKey(), e.getValue()));
        }
        else
        {
            listPermission(ctx, false, contextSet, new TreeMap<>(role.getSubjectData().getPermissions(contextSet)));
        }
    }

    private void listPermission(CommandSource ctx, boolean all, Set<Context> context, Map<String, Boolean> permissions)
    {
        String ctxText = getContextString(context);
        if (permissions.isEmpty())
        {
            //i18n.sendTranslated(ctx, NEGATIVE, "No permissions set in {input#context}.", ctxText);
            return;
        }
        i18n.send(ctx, POSITIVE, "in {input#context}:", ctxText);
        if (all)
        {
            i18n.send(ctx, POSITIVE, "(Including inherited permissions)");
        }

        Map<String, Object> easyMap = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : permissions.entrySet())
        {
            PermissionTreeConverter.easyMapValue(easyMap, entry.getKey(), entry.getValue());
        }

        ListNode list = PermissionTreeConverter.organizeTree(easyMap);

        listPermissions0(ctx, i18n.getTranslation(ctx, "true"), i18n.getTranslation(ctx, "false"), list, 0, new Stack<>());
    }

    private void listPermissions0(CommandSource ctx, String tT, String fT, ListNode list, int level, Stack<String> permStack)
    {
        int i = 0;
        for (Node value : list.getValue())
        {
            i++;
            String prefix = repeat("│", level);
            if (value instanceof StringNode)
            {
                String perm = ((StringNode) value).getValue();
                boolean neg = false;
                TextColor color = DARK_GREEN;
                if (perm.startsWith("-"))
                {
                    neg = true;
                    perm = perm.substring(1);
                    color = DARK_RED;
                }
                permStack.push(perm);
                Text permText = Text.of(perm).toBuilder().onHover(TextActions.showText(Text.of(YELLOW, StringUtils.implode(".", permStack)))).build();
                String tree = prefix + (i == 1 && level != 0 ? (list.getValue().size() == 1 ? "-" : "┬") : "├") + " ";
                ctx.sendMessage(Text.of(WHITE, tree, YELLOW, permText, WHITE, ": ", color, neg ? fT : tT));
            }
            if (value instanceof MapNode)
            {
                for (Entry<String, Node> entry : ((MapNode) value).getMappedNodes().entrySet())
                {
                    String tree = prefix + (i == 1 ? "┬" : "├") + " ";
                    ctx.sendMessage(Text.of(WHITE, tree, YELLOW, entry.getKey(), WHITE, ":"));
                    permStack.push(entry.getKey());
                    listPermissions0(ctx, tT, fT, ((ListNode) entry.getValue()), level + 1, permStack);
                }
            }
            permStack.pop();
        }
    }

    private String getContextString(Set<Context> context)
    {
        String ctxText = context.stream().map(c -> c.getValue().isEmpty() ? c.getKey() : c.getKey() + "|" + c.getValue()).collect(Collectors.joining(" ; "));
        if (context.isEmpty())
        {
            ctxText = "global";
        }
        return ctxText;
    }

    @Alias(value = {"listROption", "listRData"})
    @Command(alias = "listData", desc = "Lists all options of given role [in context]")
    public void listOption(CommandSource ctx, RoleSubject role, @Flag boolean all, @Named("in") @Default Context context)
    {
        i18n.send(ctx, NEUTRAL, "Options list for {role}", role);
        Set<Context> contextSet = toSet(context);
        if (all)
        {
            listOption(ctx, true, contextSet, RolesUtil.fillOptions(role, contextSet, new HashMap<>()));
        }
        else if (contextSet.isEmpty())
        {
            role.getSubjectData().getAllOptions().entrySet().forEach(
                    e -> listOption(ctx, false, e.getKey(), e.getValue()));
        }
        else
        {
            listOption(ctx, false, contextSet, role.getSubjectData().getOptions(contextSet));
        }
    }

    private void listOption(CommandSource ctx, boolean all, Set<Context> context, Map<String, String> options)
    {
        String ctxText = getContextString(context);
        if (options.isEmpty())
        {
            //i18n.sendTranslated(ctx, NEGATIVE, "No options set in {input#context}.", ctxText);
            return;
        }
        i18n.send(ctx, POSITIVE, "in {input#context}:", ctxText);
        if (all)
        {
            i18n.send(ctx, POSITIVE, "(Including inherited options)");
        }
        for (Entry<String, String> entry : options.entrySet())
        {
            ctx.sendMessage(Text.of("- ", YELLOW, entry.getKey(), WHITE, ": ", GOLD, entry.getValue()));
        }
    }

    @Alias(value = "listRParent")
    @Command(desc = "Lists all parents of given role [in context]")
    public void listParent(CommandSource ctx, RoleSubject role, @Named("in") @Default Context context)
    {
        List<Subject> parents = role.getSubjectData().getParents(toSet(context));
        i18n.send(ctx, NEUTRAL, "Parent list for {role}", role);
        if (parents.isEmpty())
        {
            i18n.send(ctx, NEGATIVE, "No parent roles in {context}.", context);
            return;
        }
        i18n.send(ctx, POSITIVE, "in {context}:", context);
        for (Subject parent : parents)
        {
            ctx.sendMessage(Text.of("- ", YELLOW, parent.getIdentifier()));
        }
    }

    @Command(alias = "prio", desc = "Show the priority of given role")
    public void priority(CommandSource ctx, RoleSubject role)
    {
        Priority priority = role.prio();
        i18n.send(ctx, NEUTRAL, "The priority of the role {role} is: {integer#priority}", role, priority.value);
    }

    @Command(alias = {"default","defaultRoles","listDefRoles"}, desc = "Lists all default roles")
    public void listDefaultRoles(CommandSource cContext)
    {
        List<Subject> parents = service.getDefaults().getSubjectData().getParents(Collections.emptySet());
        if (parents.isEmpty())
        {
            i18n.send(cContext, NEGATIVE, "There are no default roles set!");
            return;
        }
        i18n.send(cContext, POSITIVE, "The following roles are default roles:");
        for (Subject role : parents)
        {
            cContext.sendMessage(Text.of("- ", YELLOW, role.getIdentifier()));
        }
    }
}
