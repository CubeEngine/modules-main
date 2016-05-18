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
package org.cubeengine.module.roles.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.google.common.collect.ImmutableMap;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.RolesUtil.FoundPermission;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextElement;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.format.TextColors;

import static org.cubeengine.module.roles.RolesUtil.permText;
import static org.cubeengine.libcube.util.ContextUtil.toSet;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.text.TextTemplate.arg;
import static org.spongepowered.api.text.format.TextColors.*;

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
            i18n.sendTranslated(cContext, NEGATIVE, "There are no roles!");
            return;
        }
        i18n.sendTranslated(cContext, POSITIVE, "The following roles are available:");
        for (Subject r : roles)
        {
            cContext.sendMessage(Text.of("- ", YELLOW, r.getIdentifier()));
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
            i18n.sendTranslated(ctx, NEUTRAL, "The permission {txt} is not assigned to the role {role} in {context}.", permText(ctx, permission, service, i18n), role, context);
            return;
        }
        if (perm.value)
        {
            i18n.sendTranslated(ctx, POSITIVE, "{txt#permission} is set to {text:true:color=DARK_GREEN} for the role {role} in {context}.", permText(ctx,permission, service, i18n), role, context);
        }
        else
        {
            i18n.sendTranslated(ctx, NEGATIVE, "{txt#permission} is set to {text:false:color=DARK_RED} for the role {role} in {context}.", permText(ctx, permission, service, i18n), role, context);
        }
        i18n.sendTranslated(ctx, NEUTRAL, "Permission inherited from:");
        i18n.sendTranslated(ctx, NEUTRAL, "{txt#permission} in the role {name}!",
            permText(ctx, perm.permission, service, i18n), perm.subject.getIdentifier());
    }

    @Alias(value = "listRPerm")
    @Command(alias = "listPerm", desc = "Lists all permissions of given role [in context]")
    public void listPermission(CommandSource ctx, RoleSubject role, @Flag boolean all, @Named("in") @Default Context context)
    {
        Map<String, Boolean> permissions = new HashMap<>();
        if (all)
        {
            RolesUtil.fillPermissions(role, toSet(context), permissions);
        }
        else
        {
            permissions.putAll(role.getSubjectData().getPermissions(toSet(context)));
        }
        if (permissions.isEmpty())
        {
            i18n.sendTranslated(ctx, NEUTRAL, "No permissions set for the role {role} in {context}.", role, context);
            return;
        }
        i18n.sendTranslated(ctx, POSITIVE, "Permissions of the role {role} in {context}:", role, context);
        if (all)
        {
            i18n.sendTranslated(ctx, POSITIVE, "(Including inherited permissions)");
        }
        TextTemplate trueTemplate = TextTemplate.of("- ", arg("perm").color(YELLOW), WHITE, ": ", DARK_GREEN, i18n.getTranslation(ctx, MessageType.NONE, "true"));
        TextTemplate falseTemplate = TextTemplate.of("- ", arg("perm").color(YELLOW), WHITE, ": ", DARK_RED, i18n.getTranslation(ctx, MessageType.NONE, "false"));
        for (Entry<String, Boolean> perm : permissions.entrySet())
        {
            Map<String, TextElement> map = ImmutableMap.of("perm", permText(ctx, perm.getKey(), service, i18n));
            if (perm.getValue())
            {
                ctx.sendMessage(trueTemplate, map);
                continue;
            }
            ctx.sendMessage(falseTemplate, map);
        }
    }

    @Alias(value = {"listROption", "listRData"})
    @Command(alias = "listData", desc = "Lists all options of given role [in context]")
    public void listOption(CommandSource ctx, RoleSubject role, @Flag boolean all, @Named("in") @Default Context context)
    {
        Map<String, String> options = new HashMap<>();
        if (all)
        {
            RolesUtil.fillOptions(role, toSet(context), options);
        }
        else
        {
            options.putAll(role.getSubjectData().getOptions(toSet(context)));
        }
        if (options.isEmpty())
        {
            i18n.sendTranslated(ctx, NEUTRAL, "No options set for the role {role} in {context}.", role, context);
            return;
        }
        i18n.sendTranslated(ctx, POSITIVE, "Options of the role {role} in {context}:", role, context);
        if (all)
        {
            i18n.sendTranslated(ctx, POSITIVE, "(Including inherited options)");
        }
        for (Entry<String, String> entry : options.entrySet())
        {
            ctx.sendMessage(Text.of("- ", YELLOW, entry.getKey(), WHITE, ": ", TextColors.GOLD, entry.getValue()));
        }
    }

    @Alias(value = "listRParent")
    @Command(desc = "Lists all parents of given role [in context]")
    public void listParent(CommandSource ctx, RoleSubject role, @Named("in") @Default Context context)
    {
        List<Subject> parents = role.getSubjectData().getParents(toSet(context));
        if (parents.isEmpty())
        {
            i18n.sendTranslated(ctx, NEUTRAL, "The role {role} in {context} has no parent roles.", role, context);
            return;
        }
        i18n.sendTranslated(ctx, NEUTRAL, "The role {role} in {context} has following parent roles:", role, context);
        for (Subject parent : parents)
        {
            ctx.sendMessage(Text.of("- ", YELLOW, parent.getIdentifier()));
        }
    }

    @Command(alias = "prio", desc = "Show the priority of given role")
    public void priority(CommandSource ctx, RoleSubject role)
    {
        Priority priority = role.prio();
        i18n.sendTranslated(ctx, NEUTRAL, "The priority of the role {role} is: {integer#priority}", role, priority.value);
    }

    @Command(alias = {"default","defaultRoles","listDefRoles"}, desc = "Lists all default roles")
    public void listDefaultRoles(CommandSource cContext)
    {
        List<Subject> parents = service.getDefaultData().getParents(Collections.emptySet());
        if (parents.isEmpty())
        {
            i18n.sendTranslated(cContext, NEGATIVE, "There are no default roles set!");
            return;
        }
        i18n.sendTranslated(cContext, POSITIVE, "The following roles are default roles:");
        for (Subject role : parents)
        {
            cContext.sendMessage(Text.of("- ", YELLOW, role.getIdentifier()));
        }
    }
}
