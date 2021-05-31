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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.cubeengine.converter.node.ListNode;
import org.cubeengine.converter.node.MapNode;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.StringNode;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Parser;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.command.parser.ContextParser;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.RolesUtil.FoundOption;
import org.cubeengine.module.roles.RolesUtil.FoundPermission;
import org.cubeengine.module.roles.commands.provider.FileSubjectParser;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.config.PermissionTreeConverter;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.Contextual;
import org.spongepowered.api.service.pagination.PaginationList.Builder;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.cubeengine.libcube.util.ContextUtil.toSet;
import static org.cubeengine.libcube.util.StringUtils.repeat;
import static org.cubeengine.module.roles.RolesUtil.permText;

@Singleton
@Using({FileSubjectParser.class, ContextParser.class})
public class RoleInformationCommands extends DispatcherCommand
{
    private RolesPermissionService service;
    private I18n i18n;

    @Inject
    public RoleInformationCommands(RolesPermissionService service, I18n i18n)
    {
        super(Roles.class);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias(value = "listRoles")
    @Command(desc = "Lists all roles")
    public void list(CommandCause cause)
    {
        final Audience cContext = cause.audience();
        List<Subject> roles = new ArrayList<>();
        roles.addAll(service.groupSubjects().loadedSubjects());
        if (roles.isEmpty())
        {
            i18n.send(cContext, NEGATIVE, "There are no roles!");
            return;
        }
        i18n.send(cContext, POSITIVE, "The following roles are available:");
        String permTrans = i18n.getTranslation("permissions");
        String optTrans = i18n.getTranslation("options");
        String parentTrans = i18n.getTranslation("parents");
        Component permClick = i18n.translate(cContext, NEUTRAL, "Click to show {input}", permTrans);
        Component optClick = i18n.translate(cContext, NEUTRAL, "Click to show {input}", optTrans);
        Component parentClick = i18n.translate(cContext, NEUTRAL, "Click to show {input}", parentTrans);
        Component defaultClick = i18n.translate(cContext, NEUTRAL, "Click to toggle default");
        String defaultRole = i18n.getTranslation(cContext, "default");
        String noDefaultRole = i18n.getTranslation(cContext, "not default");
        roles.sort(Comparator.comparing(Contextual::identifier));
        List<? extends SubjectReference> defaults = service.defaults().subjectData().parents(Collections.emptySet());
        for (Subject r : roles)
        {

            final Component perms = i18n.composeMessage(cause, Style.style(NamedTextColor.GRAY), "[{name:color=yellow}]", permTrans)
                .hoverEvent(HoverEvent.showText(permClick)).clickEvent(ClickEvent.runCommand("/roles role listpermission " + r.identifier()));
            final Component opts = i18n.composeMessage(cause, Style.style(NamedTextColor.GRAY), "[{name:color=yellow}]", optTrans)
                .hoverEvent(HoverEvent.showText(optClick)).clickEvent(ClickEvent.runCommand("/roles role listoption " + r.identifier()));
            final Component parents = i18n.composeMessage(cause, Style.style(NamedTextColor.GRAY), "[{name:color=yellow}]", parentTrans)
                .hoverEvent(HoverEvent.showText(parentClick)).clickEvent(ClickEvent.runCommand("/roles role listparent " + r.identifier()));
            // TODO downloads
            final Component def = i18n.composeMessage(cause, Style.style(NamedTextColor.GRAY), "[{name:color=yellow}]", defaults.contains(r.asSubjectReference()) ? defaultRole : noDefaultRole)
                .hoverEvent(HoverEvent.showText(defaultClick)).clickEvent(ClickEvent.runCommand("/roles role toggledefault " + r.identifier()));

            cContext.sendMessage(Identity.nil(), i18n.composeMessage(cause, Style.empty(),
                 "- {name} {txt#perm} {txt#opts} {txt#parents} {txt#def}", r.identifier(), perms, opts, parents, def));
        }
    }

    @Alias(value = "checkRPerm")
    @Command(alias = "checkPerm", desc = "Checks the permission in given role [in context]")
    public void checkPermission(CommandCause ctx, FileSubject role,
                                @Parser(completer = PermissionCompleter.class) String permission,
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
            permText(ctx, perm.permission, service, i18n), perm.subject.identifier());
    }

    @Alias(value = "listRPerm")
    @Command(alias = "listPerm", desc = "Lists all permissions of given role [in context]")
    public void listPermission(CommandCause ctx, FileSubject role, @Flag boolean all, @Named("in") @Default Context context)
    {
        final PaginationService paginator = Sponge.game().serviceProvider().paginationService();
        final Builder builder = paginator.builder();
        Component header = i18n.translate(ctx, NEUTRAL, "Permission list for {role}", role);
        Set<Context> contextSet = toSet(context);
        final List<Component> permList = new ArrayList<>();
        if (all)
        {
            header = header.append(Component.space()).append(i18n.translate(ctx, POSITIVE, "(Including inherited permissions)"));
            listPermission(ctx, permList, role, true, contextSet, RolesUtil.fillPermissions(role, contextSet, new TreeMap<>(), service));
        }
        else if (contextSet.isEmpty())
        {
            header = header.append(Component.space()).append(i18n.translate(ctx, NEUTRAL, "set directly"));
            role.subjectData().allPermissions().forEach((key, value) -> listPermission(ctx, permList, role, false, key, value));
        }
        else
        {
            String ctxText = getContextString(contextSet);
            header = header.append(Component.space()).append(i18n.translate(ctx, POSITIVE, "in {input#context}:", ctxText));
            listPermission(ctx, permList, role, false, contextSet, new TreeMap<>(role.subjectData().permissions(contextSet)));
        }
        builder.header(header).contents(permList).build().sendTo(ctx.audience());
    }

    private void listPermission(CommandCause ctx, List<Component> permList, FileSubject role, boolean all, Set<Context> context, Map<String, Boolean> permissions)
    {
        String ctxText = getContextString(context);
        if (permissions.isEmpty())
        {
            permList.add(i18n.translate(ctx, NEGATIVE, "No permissions set for {role} in {input#context}.", role, ctxText));
            return;
        }
        if (!context.isEmpty() || all)
        {
            permList.add(i18n.translate(ctx, POSITIVE, "in {input#context}:", ctxText));
        }

        Map<String, Object> easyMap = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : permissions.entrySet())
        {
            PermissionTreeConverter.easyMapValue(easyMap, entry.getKey(), entry.getValue());
        }

        ListNode list = PermissionTreeConverter.organizeTree(easyMap);

        permList.addAll(listPermissions0(ctx, i18n.getTranslation(ctx, "true"), i18n.getTranslation(ctx, "false"), list, 0, new Stack<>()));
    }

    private List<Component> listPermissions0(CommandCause ctx, String tT, String fT, ListNode list, int level, Stack<String> permStack)
    {
        List<Component> permList = new ArrayList<>();
        int i = 0;
        for (Node value : list.getValue())
        {
            i++;
            String prefix = repeat("│", level);
            if (value instanceof StringNode)
            {
                String perm = ((StringNode) value).getValue();
                boolean neg = false;
                NamedTextColor color = NamedTextColor.DARK_GREEN;
                if (perm.startsWith("-"))
                {
                    neg = true;
                    perm = perm.substring(1);
                    color = NamedTextColor.DARK_RED;
                }
                permStack.push(perm);
                Component permText = Component.text(perm).hoverEvent(HoverEvent.showText(Component.text(StringUtils.implode(".", permStack), NamedTextColor.YELLOW)));
                String tree = prefix + (i == 1 && level != 0 ? (list.getValue().size() == 1 ? "-" : "┬") : "├") + " ";
                permList.add(Component.text().append(Component.text(tree, NamedTextColor.WHITE).append(permText.color(NamedTextColor.YELLOW))
                                                              .append(Component.text(": ", NamedTextColor.WHITE)).append(Component.text(neg ? fT : tT, color))).build());
            }
            if (value instanceof MapNode)
            {
                for (Entry<String, Node> entry : ((MapNode) value).getMappedNodes().entrySet())
                {
                    String tree = prefix + (i == 1 ? "┬" : "├") + " ";
                    permList.add(Component.text().append(Component.text(tree, NamedTextColor.WHITE))
                                 .append(Component.text(entry.getKey(), NamedTextColor.YELLOW))
                                 .append(Component.text(":", NamedTextColor.WHITE)).build());
                    permStack.push(entry.getKey());
                    permList.addAll(listPermissions0(ctx, tT, fT, ((ListNode) entry.getValue()), level + 1, permStack));
                }
            }
            permStack.pop();
        }
        return permList;
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

    @Alias(value = "listROption", alias = "listRData")
    @Command(alias = "listData", desc = "Lists all options of given role [in context]")
    public void listOption(CommandCause ctx, FileSubject role, @Flag boolean all, @Named("in") @Default Context context)
    {
        i18n.send(ctx, NEUTRAL, "Options list for {role}", role);
        Set<Context> contextSet = toSet(context);
        if (all)
        {
            listOption(ctx, true, contextSet, RolesUtil.fillOptions(role, contextSet, new HashMap<>(), service));
        }
        else if (contextSet.isEmpty())
        {
            role.subjectData().allOptions().forEach((key, value) -> listOption(ctx, false, key, value));
        }
        else
        {
            listOption(ctx, false, contextSet, role.subjectData().options(contextSet));
        }
    }

    private void listOption(CommandCause ctx, boolean all, Set<Context> context, Map<String, ?> options)
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
        for (Entry<String, ?> entry : options.entrySet())
        {
            if (entry.getValue() instanceof RolesUtil.FoundOption)
            {
                Subject owner = ((RolesUtil.FoundOption) entry.getValue()).subject;

                Component hoverText = Component.text().append(Component.text(owner.containingCollection().identifier(), NamedTextColor.YELLOW))
                         .append(Component.text(":", NamedTextColor.GRAY))
                         .append(Component.text(owner.friendlyIdentifier().orElse(owner.identifier()), NamedTextColor.YELLOW))
                         .build();

                Component key = Component.text(entry.getKey(), NamedTextColor.YELLOW)
                    .hoverEvent(HoverEvent.showText(hoverText));

                final TextComponent finalText = Component.text().append(Component.text("- ")).append(key)
                                                         .append(Component.text(": ", NamedTextColor.WHITE))
                                                         .append(Component.text(((FoundOption)entry.getValue()).value, NamedTextColor.GOLD)).build();

                ctx.sendMessage(Identity.nil(), finalText);
            }
            else
            {
                final TextComponent finalText = Component.text().append(Component.text("- ")).append(Component.text(entry.getKey(), NamedTextColor.YELLOW))
                                                         .append(Component.text(": ", NamedTextColor.WHITE)).append(
                    Component.text(entry.getValue().toString(), NamedTextColor.GOLD)).build();

                ctx.sendMessage(Identity.nil(), finalText);
            }

        }
    }

    @Alias(value = "listRParent")
    @Command(desc = "Lists all parents of given role [in context]")
    public void listParent(CommandCause ctx, FileSubject role, @Named("in") @Default Context context)
    {
        List<SubjectReference> parents = role.subjectData().parents(toSet(context));
        i18n.send(ctx, NEUTRAL, "Parent list for {role}", role);
        if (parents.isEmpty())
        {
            i18n.send(ctx, NEGATIVE, "No parent roles in {context}.", context);
            return;
        }
        i18n.send(ctx, POSITIVE, "in {context}:", context);
        for (SubjectReference parent : parents)
        {
            ctx.sendMessage(Identity.nil(), i18n.composeMessage(ctx, Style.empty(), "- {name:color=YELLOW}", parent.subjectIdentifier()));
        }
    }

    @Command(alias = "prio", desc = "Show the priority of given role")
    public void priority(CommandCause ctx, FileSubject role)
    {
        Priority priority = role.prio();
        i18n.send(ctx, NEUTRAL, "The priority of the role {role} is: {integer#priority}", role, priority.value);
    }

    @Command(alias = {"default","defaultRoles","listDefRoles"}, desc = "Lists all default roles")
    public void listDefaultRoles(CommandCause cContext)
    {
        List<? extends SubjectReference> parents = service.userSubjects().defaults().subjectData().parents(Collections.emptySet());
        if (parents.isEmpty())
        {
            i18n.send(cContext, NEGATIVE, "There are no default roles set!");
            return;
        }
        i18n.send(cContext, POSITIVE, "The following roles are default roles:");
        for (SubjectReference role : parents)
        {
            cContext.sendMessage(Identity.nil(), i18n.composeMessage(cContext, Style.empty(), "- {name:color=YELLOW}", role.subjectIdentifier()));
        }
    }
}
