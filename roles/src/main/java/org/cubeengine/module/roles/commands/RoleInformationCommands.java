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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.subject.RoleSubject;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import static org.cubeengine.module.roles.commands.RoleCommands.toSet;
import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.TRUE;

@Command(name = "role", desc = "Manage roles")
public class RoleInformationCommands extends ContainerCommand
{
    private RolesPermissionService service;
    private I18n i18n;

    public RoleInformationCommands(Roles module, RolesPermissionService service, I18n i18n)
    {
        super(module);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias(value = "listroles")
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
            cContext.sendMessage(Text.of("- ", TextColors.YELLOW, r instanceof RoleSubject ? ((RoleSubject) r).getName() : r.getIdentifier()));
        }
    }

    @Alias(value = "checkrperm")
    @Command(alias = "checkpermission", desc = "Checks the permission in given role")
    public void checkperm(CommandSource cContext, RoleSubject role,
                          @Complete(PermissionCompleter.class) String permission,
                          @Named("in") @Default Context context)
    {
        Tristate value = role.getPermissionValue(toSet(context), permission);
        if (value == TRUE)
        {
            i18n.sendTranslated(cContext, POSITIVE, "{name#permission} is set to {text:true:color=DARK_GREEN} for the role {role} in {context}.",
                                    permission, role, context);
        }
        else if (value == FALSE)
        {
            i18n.sendTranslated(cContext, NEGATIVE, "{name#permission} is set to {text:false:color=DARK_RED} for the role {role} in {context}.",
                                    permission, role, context);
        }
        else
        {
            i18n.sendTranslated(cContext, NEUTRAL, "The permission {name} is not assigned to the role {role} in {context}.",
                                    permission, role, context);
            return;
        }
        // TODO origin:
        //context.sendTranslated(NEUTRAL, "Permission inherited from:");
        //context.sendTranslated(NEUTRAL, "{name#permission} in the role {name}!", myPerm.getKey(), myPerm.getOrigin().getName());
        // context.sendTranslated(NEUTRAL, "{name#permission} in the role {name}!", myPerm.getOriginPermission(), myPerm.getOrigin().getName());
    }

    @Alias(value = "listrperm")
    @Command(alias = "listpermission", desc = "Lists all permissions of given role")
    public void listperm(CommandSource cContext, RoleSubject role,
                         @Flag boolean all,
                         @Named("in") @Default Context context)
    {
        Map<String, Boolean> permissions = role.getSubjectData().getPermissions(toSet(context));
        if (all)
        {
            // TODO recursive
        }
        if (permissions.isEmpty())
        {
            i18n.sendTranslated(cContext, NEUTRAL, "No permissions set for the role {role} in {context}.", role, context);
            return;
        }
        i18n.sendTranslated(cContext, POSITIVE, "Permissions of the role {role} in {context}:", role, context);
        if (all)
        {
            i18n.sendTranslated(cContext, POSITIVE, "(Including inherited permissions)");
        }
        String trueString = ChatFormat.DARK_GREEN + "true";
        String falseString = ChatFormat.DARK_RED + "false";
        for (Entry<String, Boolean> perm : permissions.entrySet())
        {
            if (perm.getValue())
            {
                cContext.sendMessage(Text.of("- ", TextColors.YELLOW, perm.getKey(),
                        TextColors.WHITE, ": ", TextColors.GOLD, trueString));
                continue;
            }
            cContext.sendMessage(Text.of("- ", TextColors.YELLOW, perm.getKey(),
                    TextColors.WHITE, ": ", TextColors.GOLD, falseString));
        }
    }

    @Alias(value = "listrdata")
    @Command(alias = {"listdata", "listmeta"}, desc = "Lists all metadata of given role")
    public void listmetadata(CommandSource cContext, RoleSubject role,
                             @Flag boolean all,
                             @Named("in") @Default Context context)
    {
        Map<String, String> options = role.getSubjectData().getOptions(toSet(context));
        if (all)
        {
            // TODO recursive
        }
        if (options.isEmpty())
        {
            i18n.sendTranslated(cContext, NEUTRAL, "No metadata set for the role {role} in {context}.", role, context);
            return;
        }
        i18n.sendTranslated(cContext, POSITIVE, "Metadata of the role {role} in {context}:", role, context);
        if (all)
        {
            i18n.sendTranslated(cContext, POSITIVE, "(Including inherited metadata)");
        }
        for (Entry<String, String> entry : options.entrySet())
        {
            cContext.sendMessage(Text.of("- ", TextColors.YELLOW, entry.getKey(),
                    TextColors.WHITE, ": ", TextColors.GOLD, entry.getValue()));
        }
    }

    @Alias(value = "listrparent")
    @Command(desc = "Lists all parents of given role")
    public void listParent(CommandSource ctx, RoleSubject role,
                           @Named("in") @Default Context context)
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
            ctx.sendMessage(Text.of("- ", TextColors.YELLOW, parent instanceof RoleSubject ? ((RoleSubject) parent).getName() : parent.getIdentifier()));
        }
    }

    @Command(alias = "prio", desc = "Show the priority of given role")
    public void priority(CommandSource ctx, RoleSubject role)
    {
        Priority priority = role.prio();
        i18n.sendTranslated(ctx, NEUTRAL, "The priority of the role {role} is: {integer#priority}", role, priority.value);
    }

    @Command(alias = {"default","defaultroles","listdefroles"}, desc = "Lists all default roles [in context]")
    public void listDefaultRoles(CommandSource cContext, @Named("in") @Default Context context)
    {
        List<Subject> parents = service.getDefaultData().getParents(toSet(context));
        if (parents.isEmpty())
        {
            i18n.sendTranslated(cContext, NEGATIVE, "There are no default roles set for {context}!", context);
            return;
        }
        i18n.sendTranslated(cContext, POSITIVE, "The following roles are default roles in {context}!", context);
        for (Subject role : parents)
        {
            cContext.sendMessage(Text.of("- ", TextColors.YELLOW, role instanceof RoleSubject ? ((RoleSubject) role).getName() : role.getIdentifier()));
        }
    }
}
