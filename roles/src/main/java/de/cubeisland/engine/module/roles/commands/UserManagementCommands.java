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

import java.util.Set;
import de.cubeisland.engine.butler.alias.Alias;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Complete;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.commands.provider.ContextualRole;
import de.cubeisland.engine.module.roles.commands.provider.PermissionCompleter;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.subject.RoleSubject;
import de.cubeisland.engine.service.command.CommandContext;
import de.cubeisland.engine.service.command.ContainerCommand;
import de.cubeisland.engine.service.user.User;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;

import static de.cubeisland.engine.service.i18n.formatter.MessageType.*;
import static de.cubeisland.engine.module.roles.commands.RoleCommands.LISTELEM;

@Alias("manuser")
@Command(name = "user", desc = "Manage users")
public class UserManagementCommands extends ContainerCommand
{
    private RolesPermissionService service;

    public UserManagementCommands(Roles module, RolesPermissionService service)
    {
        super(module);
        this.service = service;
    }

    @Alias({"manuadd", "assignurole", "addurole", "giveurole"})
    @Command(alias = {"add", "give"}, desc = "Assign a role to the player [-temp]")
    public void assign(CommandContext context,
                       @Default User player,
                       ContextualRole role, // TODO RoleCompleter & Reader
                       @Flag boolean temp)
    {
        // TODO RoleReader String identifier = role.contains("|") ? "role:" + role : "role:world|" + world.getName() + "|" + role;
        String ctx = role.contextType + "|" + role.contextName;
        if (!service.getGroupSubjects().hasRegistered(role.getIdentifier()))
        {
            context.sendTranslated(NEUTRAL, "Could not find the role {name} in {input#context}.", role.roleName, ctx);
            return;
        }
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        if (!r.canAssignAndRemove(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to assign the role {role} in {input#context}!", r, ctx);
            return;
        }
        Set<Context> contexts = RoleCommands.toSet(role.getContext());
        if (temp)
        {
            if (!player.getPlayer().isPresent())
            {
                context.sendTranslated(NEGATIVE, "You cannot assign a temporary role to a offline player!");
                return;
            }
            if (player.asPlayer().getTransientSubjectData().addParent(contexts, r))
            {
                context.sendTranslated(POSITIVE, "Added the role {role} temporarily to {user} in {input#context}.", r, player, ctx);
                return;
            }
            context.sendTranslated(NEUTRAL, "{user} already had the role {role} in {input#context}.", player, r, ctx);
            return;
        }
        if (player.asPlayer().getSubjectData().addParent(contexts, r))
        {
            context.sendTranslated(POSITIVE, "Added the role {role} to {user} in {input#context}.", r, player, ctx);
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} already has the role {role} in {input#context}.", player, r, ctx);
    }

    @Alias(value = {"remurole", "manudel"})
    @Command(desc = "Removes a role from the player")
    public void remove(CommandContext context, @Default User player, ContextualRole role)
    {
        String ctx = role.contextType + "|" + role.contextName;
        if (!service.getGroupSubjects().hasRegistered(role.getIdentifier()))
        {
            context.sendTranslated(NEUTRAL, "Could not find the role {name} in {input#context}.", role, ctx);
            return;
        }
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        if (!r.canAssignAndRemove(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to remove the role {role} in {input#context}!", r, ctx);
            return;
        }
        Set<Context> contexts = RoleCommands.toSet(role.getContext());
        if (player.asPlayer().getSubjectData().removeParent(contexts, r))
        {
            context.sendTranslated(POSITIVE, "Removed the role {role} from {user} in {input#context}.", r, player, ctx);
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} did not have the role {role} in {input#context}.", player, r, ctx);
    }

    @Alias(value = {"clearurole", "manuclear"})
    @Command(desc = "Clears all roles from the player and sets the defaultroles [in context]")
    public void clear(CommandContext cContext, @Default User player, @Named("in") @Default Context context) // TODO reader for context
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        player.asPlayer().getSubjectData().clearParents(contexts);
        cContext.sendTranslated(NEUTRAL, "Cleared the roles of {user} in {ctx}.", player, context);
        SubjectData defaultData = service.getDefaultData();
        if (!defaultData.getParents(contexts).isEmpty())
        {
            cContext.sendTranslated(NEUTRAL, "Default roles assigned:");
            for (Subject subject : defaultData.getParents(contexts))
            {
                player.asPlayer().getTransientSubjectData().addParent(contexts, subject);
                cContext.sendMessage(String.format(LISTELEM, subject instanceof RoleSubject ? ((RoleSubject)subject).getName() : subject.getIdentifier()));
            }
        }
    }

    @Alias(value = "setuperm")
    @Command(alias = "setperm", desc = "Sets a permission for this user [in context]")
    public void setpermission(CommandContext cContext, @Default User player, @Complete(PermissionCompleter.class) String permission, @Default Tristate value, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        if (value == Tristate.UNDEFINED)
        {
            resetpermission(cContext, player, permission, context);
        }
        if (player.asPlayer().getSubjectData().setPermission(contexts, permission, value))
        {
            switch (value)
            {
                case TRUE:
                    cContext.sendTranslated(POSITIVE, "Permission {input} of {user} set to true!", permission, player);
                    return;
                case FALSE:
                    cContext.sendTranslated(NEGATIVE, "Permission {input} of {user} set to false!", permission, player);
            }
            return;
        }
        cContext.sendTranslated(NEGATIVE, "Permission {input} of {user} was already set to {bool}!", permission, player,
                                value.asBoolean());
    }

    @Alias(value = "resetuperm")
    @Command(alias = "resetperm", desc = "Resets a permission for this user [in context]")
    public void resetpermission(CommandContext cContext, @Default User player, String permission, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        if (player.asPlayer().getSubjectData().setPermission(contexts, permission, Tristate.UNDEFINED))
        {
            cContext.sendTranslated(NEUTRAL, "Permission {input} of {user} resetted!", permission, player);
            return;
        }
        cContext.sendTranslated(NEGATIVE, "Permission {input} of {user} was not set!", permission, player);

    }

    @Alias(value = {"setudata","setumeta","setumetadata"})
    @Command(alias = {"setdata", "setmeta"}, desc = "Sets metadata for this user [in context]")
    public void setmetadata(CommandContext cContext, @Default User player, String metaKey, String metaValue, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        if (((OptionSubjectData)player.asPlayer().getSubjectData()).setOption(contexts, metaKey, metaValue))
        {
            cContext.sendTranslated(POSITIVE, "Metadata {input#key} of {user} set to {input#value}!", metaKey,
                                    player, metaValue);
        }
        // TODO msg
    }

    @Alias(value = {"resetudata","resetumeta","resetumetadata"})
    @Command(alias = {"resetdata", "resetmeta", "deletedata", "deletemetadata", "deletemeta"}, desc = "Resets metadata for this user [in context]")
    public void resetmetadata(CommandContext cContext, @Default User player, String metaKey, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        if (((OptionSubjectData)player.asPlayer().getSubjectData()).setOption(contexts, metaKey, null))
        {
            cContext.sendTranslated(NEUTRAL, "Metadata {input#key} of {user} removed!", metaKey, player);
        }
        // TODO msg
    }

    @Alias(value = {"clearudata","clearumeta","clearumetadata"})
    @Command(alias = {"cleardata", "clearmeta"}, desc = "Resets metadata for this user [in context]")
    public void clearMetaData(CommandContext cContext, @Default User player, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        if (((OptionSubjectData)player.asPlayer().getSubjectData()).clearOptions(contexts))
        {
            cContext.sendTranslated(NEUTRAL, "Metadata of {user} cleared!", player);
        }
    }
}
