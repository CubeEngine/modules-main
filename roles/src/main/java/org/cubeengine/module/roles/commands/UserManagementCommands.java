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

import java.util.Set;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.emptySet;
import static org.cubeengine.module.roles.commands.RoleCommands.toSet;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

@Alias("manuser")
@Command(name = "user", desc = "Manage users")
public class UserManagementCommands extends ContainerCommand
{
    private RolesPermissionService service;
    private I18n i18n;

    public UserManagementCommands(Roles module, RolesPermissionService service, I18n i18n)
    {
        super(module);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias({"manuadd", "assignurole", "addurole", "giveurole"})
    @Command(alias = {"add", "give"}, desc = "Assign a role to the player [-temp]")
    public void assign(CommandSource ctx, @Default User player, RoleSubject role, @Flag boolean temp)
    {
        if (!role.canAssignAndRemove(ctx))
        {
            i18n.sendTranslated(ctx, NEGATIVE, "You are not allowed to assign the role {role}!", role);
            return;
        }
        if (temp)
        {
            if (!player.getPlayer().isPresent())
            {
                i18n.sendTranslated(ctx, NEGATIVE, "You cannot assign a temporary role to a offline player!");
                return;
            }
            if (player.getTransientSubjectData().addParent(emptySet(), role))
            {
                i18n.sendTranslated(ctx, POSITIVE, "Added the role {role} temporarily to {user}.", role, player);
                return;
            }
            i18n.sendTranslated(ctx, NEUTRAL, "{user} already had the temporary role {role}.", player, role);
            return;
        }
        if (player.getSubjectData().addParent(emptySet(), role))
        {
            i18n.sendTranslated(ctx, POSITIVE, "Added the role {role} to {user}.", role, player);
            return;
        }
        i18n.sendTranslated(ctx, NEUTRAL, "{user} already has the role {role}.", player, role);
    }

    @Alias(value = {"remurole", "manudel"})
    @Command(desc = "Removes a role from the player")
    public void remove(CommandSource ctx, @Default Player player, RoleSubject role)
    {
        if (!role.canAssignAndRemove(ctx))
        {
            i18n.sendTranslated(ctx, NEGATIVE, "You are not allowed to remove the role {role}!", role);
            return;
        }

        if (player.getTransientSubjectData().removeParent(emptySet(), role) | // do not short-circuit
            player.getSubjectData().removeParent(emptySet(), role))
        {
            i18n.sendTranslated(ctx, POSITIVE, "Removed the role {role} from {user}.", role, player);
            return;
        }
        i18n.sendTranslated(ctx, NEUTRAL, "{user} did not have the role {role}.", player, role);
    }

    @Alias(value = {"clearurole", "manuclear"})
    @Command(desc = "Clears all roles from the player and sets the defaultroles [in context]")
    public void clear(CommandSource ctx, @Default Player player)
    {
        player.getSubjectData().clearParents(emptySet());
        i18n.sendTranslated(ctx, NEUTRAL, "Cleared the roles of {user}.", player);
        SubjectData defaultData = service.getDefaultData();
        if (defaultData.getParents(emptySet()).isEmpty())
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Default roles assigned:");
            for (Subject subject : defaultData.getParents(emptySet()))
            {
                player.getTransientSubjectData().addParent(emptySet(), subject);
                ctx.sendMessage(Text.of("- ", TextColors.YELLOW, subject instanceof RoleSubject ? ((RoleSubject) subject).getName() : subject.getIdentifier()));
            }
        }
    }

    @Alias(value = "setuperm")
    @Command(alias = "setperm", desc = "Sets a permission for this user [in context]")
    public void setpermission(CommandSource ctx, @Default Player player, @Complete(PermissionCompleter.class) String permission, @Default Tristate value, @Named("in") @Default Context context)
    {
        if (value == Tristate.UNDEFINED)
        {
            resetpermission(ctx, player, permission, context);
        }
        Set<Context> contexts = toSet(context);
        if (!player.getSubjectData().setPermission(contexts, permission, value))
        {
            i18n.sendTranslated(ctx, NEGATIVE, "Permission {input} of {user} was already set to {bool} in {context}!", permission, player, value.asBoolean(), context);
            return;
        }
        switch (value)
        {
            case TRUE:
            case FALSE:
                i18n.sendTranslated(ctx, POSITIVE, "Permission {input} of {user} set to {bool} in {context}!", permission, player, value.asBoolean(), context);
        }
    }

    @Alias(value = "resetuperm")
    @Command(alias = "resetperm", desc = "Resets a permission for this user [in context]")
    public void resetpermission(CommandSource ctx, @Default Player player, String permission, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        if (player.getSubjectData().setPermission(contexts, permission, Tristate.UNDEFINED))
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Permission {input} of {user} reset in {context}!", permission, player, context);
            return;
        }
        i18n.sendTranslated(ctx, NEGATIVE, "Permission {input} of {user} was not set in {context}!", permission, player, context);

    }

    @Alias(value = {"setudata","setumeta","setumetadata"})
    @Command(alias = {"setdata", "setmeta"}, desc = "Sets metadata for this user [in context]")
    public void setmetadata(CommandSource ctx, @Default Player player, String metaKey, String metaValue, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        if (((OptionSubjectData)player.getSubjectData()).setOption(contexts, metaKey, metaValue))
        {
            i18n.sendTranslated(ctx, POSITIVE, "Metadata {input#key} of {user} set to {input#value} in {context}!", metaKey, player, metaValue, context);
            return;
        }
        i18n.sendTranslated(ctx, NEGATIVE, "Metadata {input#key} of {user} was already set to {input#value} in {context}!", metaKey, player, metaValue, context);
    }

    @Alias(value = {"resetudata","resetumeta","resetumetadata"})
    @Command(alias = {"resetdata", "resetmeta", "deletedata", "deletemetadata", "deletemeta"}, desc = "Resets metadata for this user [in context]")
    public void resetmetadata(CommandSource ctx, @Default Player player, String metaKey, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        if (((OptionSubjectData)player.getSubjectData()).setOption(contexts, metaKey, null))
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Metadata {input#key} of {user} removed in {context}!", metaKey, player, context);
            return;
        }
        i18n.sendTranslated(ctx, NEGATIVE, "Metadata {input#key} was not set for {user} in {context}!", metaKey, player, context);
    }

    @Alias(value = {"clearudata","clearumeta","clearumetadata"})
    @Command(alias = {"cleardata", "clearmeta"}, desc = "Resets metadata for this user [in context]")
    public void clearMetaData(CommandSource ctx, @Default Player player, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        if (((OptionSubjectData)player.getSubjectData()).clearOptions(contexts))
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Metadata of {user} cleared in {context}!", player, context);
            return;
        }
        i18n.sendTranslated(ctx, NEGATIVE, "Metadata of {user} was already cleared in {context}!", player, context);
    }
}
