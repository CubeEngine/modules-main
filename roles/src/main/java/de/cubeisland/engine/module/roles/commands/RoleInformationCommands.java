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
package de.cubeisland.engine.module.roles.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import de.cubeisland.engine.butler.alias.Alias;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Complete;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Named;
import org.cubeengine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.commands.provider.ContextualRole;
import de.cubeisland.engine.module.roles.commands.provider.PermissionCompleter;
import de.cubeisland.engine.module.roles.config.Priority;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.subject.RoleSubject;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.ContainerCommand;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;

import static de.cubeisland.engine.module.roles.commands.RoleCommands.toSet;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.TRUE;

@Command(name = "role", desc = "Manage roles")
public class RoleInformationCommands extends ContainerCommand
{
    private RolesPermissionService service;

    public RoleInformationCommands(Roles module, RolesPermissionService service)
    {
        super(module);
        this.service = service;
    }

    @Alias(value = "listroles")
    @Command(desc = "Lists all roles in a world or globally")
    public void list(CommandContext cContext, @Default Context context)
    {
        ContextualRole role = new ContextualRole();
        role.contextName = context.getName();
        role.contextType = context.getType();
        role.roleName = "";
        List<Subject> roles = new ArrayList<>();
        for (Subject subject : service.getGroupSubjects().getAllSubjects())
        {
            if (subject.getIdentifier().startsWith(role.getIdentifier()))
            {
                roles.add(subject);
            }
        }
        if (roles.isEmpty())
        {
            cContext.sendTranslated(NEGATIVE, "There are no roles in {context}!", context);
            return;
        }
        cContext.sendTranslated(POSITIVE, "The following roles are available in {context}:", context);
        for (Subject r : roles)
        {
            cContext.sendMessage(String.format(RoleCommands.LISTELEM, r instanceof RoleSubject ? ((RoleSubject)r).getName() : r.getIdentifier()));
        }
    }

    @Alias(value = "checkrperm")
    @Command(alias = "checkpermission", desc = "Checks the permission in given role")
    public void checkperm(CommandContext context, ContextualRole role, @Complete(PermissionCompleter.class) String permission)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        Tristate value = r.getPermissionValue(toSet(role.getContext()), permission);
        if (value == TRUE)
        {
            context.sendTranslated(POSITIVE, "{name#permission} is set to {text:true:color=DARK_GREEN} for the role {role} in {context}.", permission, r, role.getContext());
        }
        else if (value == FALSE)
        {
            context.sendTranslated(NEGATIVE, "{name#permission} is set to {text:false:color=DARK_RED} for the role {role} in {context}.", permission, r, role.getContext());
        }
        else
        {
            context.sendTranslated(NEUTRAL, "The permission {name} is not assigned to the role {role} in {context}.", permission, r, role.getContext());
            return;
        }
        // TODO origin:
        //context.sendTranslated(NEUTRAL, "Permission inherited from:");
        //context.sendTranslated(NEUTRAL, "{name#permission} in the role {name}!", myPerm.getKey(), myPerm.getOrigin().getName());
        // context.sendTranslated(NEUTRAL, "{name#permission} in the role {name}!", myPerm.getOriginPermission(), myPerm.getOrigin().getName());
    }

    @Alias(value = "listrperm")
    @Command(alias = "listpermission", desc = "Lists all permissions of given role")
    public void listperm(CommandContext context, ContextualRole role, @Flag boolean all)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        Map<String, Boolean> permissions = r.getSubjectData().getPermissions(toSet(role.getContext()));
        if (all)
        {
            // TODO recursive
        }
        if (permissions.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "No permissions set for the role {role} in {context}.", r, role.getContext());
            return;
        }
        context.sendTranslated(POSITIVE, "Permissions of the role {role} in {context}:", r, role.getContext());
        if (all)
        {
            context.sendTranslated(POSITIVE, "(Including inherited permissions)");
        }
        String trueString = ChatFormat.DARK_GREEN + "true";
        String falseString = ChatFormat.DARK_RED + "false";
        for (Entry<String, Boolean> perm : permissions.entrySet())
        {
            if (perm.getValue())
            {
                context.sendMessage(String.format(RoleCommands.LISTELEM_VALUE,perm,trueString));
                continue;
            }
            context.sendMessage(String.format(RoleCommands.LISTELEM_VALUE,perm,falseString));
        }
    }

    @Alias(value = "listrdata")
    @Command(alias = {"listdata", "listmeta"}, desc = "Lists all metadata of given role")
    public void listmetadata(CommandContext context, ContextualRole role, @Flag boolean all)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        Map<String, String> options = r.getSubjectData().getOptions(toSet(role.getContext()));
        if (all)
        {
            // TODO recursive
        }
        if (options.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "No metadata set for the role {role} in {context}.", r, role.getContext());
            return;
        }
        context.sendTranslated(POSITIVE, "Metadata of the role {role} in {context}:", r, role.getContext());
        if (all)
        {
            context.sendTranslated(POSITIVE, "(Including inherited metadata)");
        }
        for (Entry<String, String> data : options.entrySet())
        {
            context.sendMessage(String.format(RoleCommands.LISTELEM_VALUE,data.getKey(), data.getValue()));
        }
    }

    @Alias(value = "listrparent")
    @Command(desc = "Lists all parents of given role")
    public void listParent(CommandContext context, ContextualRole role)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        List<Subject> parents = r.getSubjectData().getParents(toSet(role.getContext()));
        if (parents.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "The role {role} in {context} has no parent roles.", r, role.getContext());
            return;
        }
        context.sendTranslated(NEUTRAL, "The role {role} in {context} has following parent roles:", r, role.getContext());
        for (Subject parent : parents)
        {
            context.sendMessage(String.format(RoleCommands.LISTELEM, parent instanceof RoleSubject ? ((RoleSubject)parent).getName() : parent.getIdentifier()));
        }
    }

    @Command(alias = "prio", desc = "Show the priority of given role")
    public void priority(CommandContext context, ContextualRole role)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        Priority priority = null; // TODO get prio
        context.sendTranslated(NEUTRAL, "The priority of the role {role} in {context} is: {integer#priority}", r, role.getContext(), priority.value);
    }

    @Command(alias = {"default","defaultroles","listdefroles"}, desc = "Lists all default roles [in context]")
    public void listDefaultRoles(CommandContext cContext, @Named("in") @Default Context context)
    {
        List<Subject> parents = service.getDefaultData().getParents(toSet(context));
        if (parents.isEmpty())
        {
            cContext.sendTranslated(NEGATIVE, "There are no default roles set for {context}!", context);
            return;
        }
        cContext.sendTranslated(POSITIVE, "The following roles are default roles in {context}!", context);
        for (Subject role : parents)
        {
            cContext.sendMessage(String.format(RoleCommands.LISTELEM, role instanceof RoleSubject ? ((RoleSubject)role).getName() : role.getIdentifier()));
        }
    }
}
