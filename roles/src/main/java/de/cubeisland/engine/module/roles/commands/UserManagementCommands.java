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

import de.cubeisland.engine.command.methodic.parametric.Complete;
import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.command.methodic.parametric.Named;
import org.bukkit.World;

import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.roles.RoleCompleter;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.role.DataStore.PermissionValue;
import de.cubeisland.engine.module.roles.role.Role;
import de.cubeisland.engine.module.roles.role.RolesAttachment;
import de.cubeisland.engine.module.roles.role.UserDatabaseStore;

import static de.cubeisland.engine.command.parameter.property.Requirement.OPTIONAL;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Alias("manuser")
@Command(name = "user", desc = "Manage users")
public class UserManagementCommands extends UserCommandHelper
{
    public UserManagementCommands(Roles module)
    {
        super(module);
    }

    @Alias({"manuadd", "assignurole", "addurole", "giveurole"})
    @Command(alias = {"add", "give"}, desc = "Assign a role to the player [in world] [-temp]")
    public void assign(CommandContext context,
                       @Default User player,
                       @Complete(RoleCompleter.class) String role,
                       @Named("in") World world,
                       @Flag boolean temp)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        Role r = this.manager.getProvider(world).getRole(role);
        if (r == null)
        {
            context.sendTranslated(NEUTRAL, "Could not find the role {name} in {world}.", role, world);
            return;
        }
        if (!r.canAssignAndRemove(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to assign the role {name} in {world}!", r.getName(), world);
            return;
        }
        RolesAttachment attachment = this.manager.getRolesAttachment(player);
        if (temp)
        {
            if (!player.isOnline())
            {
                context.sendTranslated(NEGATIVE, "You cannot assign a temporary role to a offline player!");
                return;
            }
            if (attachment.getDataHolder(world).assignTempRole(r))
            {
                attachment.getCurrentDataHolder().apply();
                context.sendTranslated(POSITIVE, "Added the role {name} temporarily to {user} in {world}.", role, player, world);
                return;
            }
            context.sendTranslated(NEUTRAL, "{user} already had the role {name} in {world}.", player, role, world);
            return;
        }
        if (attachment.getDataHolder(world).assignRole(r))
        {
            attachment.getCurrentDataHolder().apply();
            context.sendTranslated(POSITIVE, "Added the role {name} to {user} in {world}.", role, player, world);
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} already has the role {name} in {world}.", player, role, world);
    }

    @Alias(value = {"remurole", "manudel"})
    @Command(desc = "Removes a role from the player [in world]")
    public void remove(CommandContext context, @Default User player, @Complete(RoleCompleter.class) String role, @Named("in") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        Role r = this.manager.getProvider(world).getRole(role);
        if (r == null)
        {
            context.sendTranslated(NEUTRAL, "Could not find the role {name} in {world}.", role, world);
            return;
        }
        if (!r.canAssignAndRemove(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to remove the role {name} in {world}!", r.getName(), world);
            return;
        }
        RolesAttachment attachment = this.manager.getRolesAttachment(player);
        if (attachment.getDataHolder(world).removeRole(r))
        {
            attachment.reload();
            attachment.getCurrentDataHolder().apply();
            context.sendTranslated(POSITIVE, "Removed the role {name} from {user} in {world}.", r.getName(), player, world);
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} did not have the role {name} in {world}.", player, r.getName(), world);
    }

    @Alias(value = {"clearurole", "manuclear"})
    @Command(desc = "Clears all roles from the player and sets the defaultroles [in world]")
    public void clear(CommandContext context, @Default User player, @Named("in") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(player);
        UserDatabaseStore dataHolder = attachment.getDataHolder(world);
        dataHolder.clearRoles();
        Set<Role> defaultRoles = this.manager.getProvider(world).getDefaultRoles();
        for (Role role : defaultRoles)
        {
            dataHolder.assignTempRole(role);
        }
        dataHolder.apply();
        context.sendTranslated(NEUTRAL, "Cleared the roles of {user} in {world}.", player, world);
        if (!defaultRoles.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "Default roles assigned:");
            for (Role role : defaultRoles)
            {
                context.sendMessage(String.format(this.LISTELEM, role.getName()));
            }
        }
    }

    @Alias(value = "setuperm")
    @Command(alias = "setperm", desc = "Sets a permission for this user [in world]")
    public void setpermission(CommandContext context, @Default User player, String permission, @Default PermissionValue value, @Named("in") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(player);
        attachment.getDataHolder(world).setPermission(permission, value);
        attachment.getCurrentDataHolder().apply();
        switch (value)
        {
            case RESET:
                context.sendTranslated(NEUTRAL, "Permission {input} of {user} reset!", permission, player);
                return;
            case TRUE:
                context.sendTranslated(POSITIVE, "Permission {input} of {user} set to true!", permission, player);
                return;
            case FALSE:
                context.sendTranslated(NEGATIVE, "Permission {input} of {user} set to false!", permission, player);
        }
    }

    @Alias(value = "resetuperm")
    @Command(alias = "resetperm", desc = "Resets a permission for this user [in world]")
    public void resetpermission(CommandContext context, @Default User player, String perm, @Named("in") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(player);
        attachment.getDataHolder(world).setPermission(perm, PermissionValue.RESET);
        attachment.getCurrentDataHolder().apply();
        context.sendTranslated(NEUTRAL, "Permission {input} of {user} resetted!", perm, player);
    }

    @Alias(value = {"setudata","setumeta","setumetadata"})
    @Command(alias = {"setdata", "setmeta"}, desc = "Sets metadata for this user [in world]")
    public void setmetadata(CommandContext context, @Default User player, String metaKey, String metaValue, @Named("in") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(player);
        attachment.getDataHolder(world).setMetadata(metaKey, metaValue);
        attachment.getCurrentDataHolder().apply();
        context.sendTranslated(POSITIVE, "Metadata {input#key} of {user} set to {input#value} in {world}!", metaKey, player, metaValue, world);
    }

    @Alias(value = {"resetudata","resetumeta","resetumetadata"})
    @Command(alias = {"resetdata", "resetmeta", "deletedata", "deletemetadata", "deletemeta"}, desc = "Resets metadata for this user [in world]")
    public void resetmetadata(CommandContext context, @Default User player, String metaKey, @Named("in") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(player);
        attachment.getDataHolder(world).removeMetadata(metaKey);
        attachment.getCurrentDataHolder().apply();
        context.sendTranslated(NEUTRAL, "Metadata {input#key} of {user} removed in {world}!", metaKey, player, world);
    }

    @Alias(value = {"clearudata","clearumeta","clearumetadata"})
    @Command(alias = {"cleardata", "clearmeta"}, desc = "Resets metadata for this user [in world]")
    public void clearMetaData(CommandContext context, @Default User player, @Named("in") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(player);
        attachment.getDataHolder(world).clearMetadata();
        attachment.getCurrentDataHolder().apply();
        context.sendTranslated(NEUTRAL, "Metadata of {user} cleared in {world}!", player, world);
    }
}
