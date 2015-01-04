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
                       @Default @Label("player") User user,
                       @Label("role") @Complete(RoleCompleter.class) String roleName,
                       @Named("in") @Label("world") World world,
                       @Flag(name = "t",longName = "temp") boolean temp)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        Role role = this.manager.getProvider(world).getRole(roleName);
        if (role == null)
        {
            context.sendTranslated(NEUTRAL, "Could not find the role {name} in {world}.", roleName, world);
            return;
        }
        if (!role.canAssignAndRemove(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to assign the role {name} in {world}!", role.getName(), world);
            return;
        }
        RolesAttachment attachment = this.manager.getRolesAttachment(user);
        if (temp)
        {
            if (!user.isOnline())
            {
                context.sendTranslated(NEGATIVE, "You cannot assign a temporary role to a offline player!");
                return;
            }
            if (attachment.getDataHolder(world).assignTempRole(role))
            {
                attachment.getCurrentDataHolder().apply();
                context.sendTranslated(POSITIVE, "Added the role {name} temporarily to {user} in {world}.", roleName, user, world);
                return;
            }
            context.sendTranslated(NEUTRAL, "{user} already had the role {name} in {world}.", user, roleName, world);
            return;
        }
        if (attachment.getDataHolder(world).assignRole(role))
        {
            attachment.getCurrentDataHolder().apply();
            context.sendTranslated(POSITIVE, "Added the role {name} to {user} in {world}.", roleName, user, world);
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} already has the role {name} in {world}.", user, roleName, world);
    }

    @Alias(value = {"remurole", "manudel"})
    @Command(desc = "Removes a role from the player [in world]")
    public void remove(CommandContext context,
                       @Default @Label("player") User user,
                       @Label("role") @Complete(RoleCompleter.class) String roleName,
                       @Named("in") @Label("world") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        Role role = this.manager.getProvider(world).getRole(roleName);
        if (role == null)
        {
            context.sendTranslated(NEUTRAL, "Could not find the role {name} in {world}.", roleName, world);
            return;
        }
        if (!role.canAssignAndRemove(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to remove the role {name} in {world}!", role.getName(), world);
            return;
        }
        RolesAttachment attachment = this.manager.getRolesAttachment(user);
        if (attachment.getDataHolder(world).removeRole(role))
        {
            attachment.reload();
            attachment.getCurrentDataHolder().apply();
            context.sendTranslated(POSITIVE, "Removed the role {name} from {user} in {world}.", role.getName(), user, world);
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} did not have the role {name} in {world}.", user, role.getName(), world);
    }

    @Alias(value = {"clearurole", "manuclear"})
    @Command(desc = "Clears all roles from the player and sets the defaultroles [in world]")
    public void clear(CommandContext context,
                      @Default @Label("player") User user,
                      @Named("in") @Label("world") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(user);
        UserDatabaseStore dataHolder = attachment.getDataHolder(world);
        dataHolder.clearRoles();
        Set<Role> defaultRoles = this.manager.getProvider(world).getDefaultRoles();
        for (Role role : defaultRoles)
        {
            dataHolder.assignTempRole(role);
        }
        dataHolder.apply();
        context.sendTranslated(NEUTRAL, "Cleared the roles of {user} in {world}.", user, world);
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
    public void setpermission(CommandContext context,
                              @Default @Label("player") User user,
                              @Label("permission") String perm,
                              @Default PermissionValue value,
                              @Named("in") @Label("world") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(user);
        attachment.getDataHolder(world).setPermission(perm, value);
        attachment.getCurrentDataHolder().apply();
        switch (value)
        {
            case RESET:
                context.sendTranslated(NEUTRAL, "Permission {input} of {user} reset!", perm, user);
                return;
            case TRUE:
                context.sendTranslated(POSITIVE, "Permission {input} of {user} set to true!", perm, user);
                return;
            case FALSE:
                context.sendTranslated(NEGATIVE, "Permission {input} of {user} set to false!", perm, user);
        }
    }

    @Alias(value = "resetuperm")
    @Command(alias = "resetperm", desc = "Resets a permission for this user [in world]")
    public void resetpermission(CommandContext context,
                                @Default @Label("player") User user,
                                @Label("permission") String perm,
                                @Named("in") @Label("world") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(user);
        attachment.getDataHolder(world).setPermission(perm, PermissionValue.RESET);
        attachment.getCurrentDataHolder().apply();
        context.sendTranslated(NEUTRAL, "Permission {input} of {user} resetted!", perm, user);
    }

    @Alias(value = {"setudata","setumeta","setumetadata"})
    @Command(alias = {"setdata", "setmeta"}, desc = "Sets metadata for this user [in world]")
    public void setmetadata(CommandContext context,
                            @Default @Label("player") User user,
                            @Label("metaKey") String metaKey,
                            @Label("metaValue") String metaVal,
                            @Named("in") @Label("world") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(user);
        attachment.getDataHolder(world).setMetadata(metaKey, metaVal);
        attachment.getCurrentDataHolder().apply();
        context.sendTranslated(POSITIVE, "Metadata {input#key} of {user} set to {input#value} in {world}!", metaKey, user, metaVal, world);
    }

    @Alias(value = {"resetudata","resetumeta","resetumetadata"})
    @Command(alias = {"resetdata", "resetmeta", "deletedata", "deletemetadata", "deletemeta"}, desc = "Resets metadata for this user [in world]")
    public void resetmetadata(CommandContext context,
                              @Default @Label("player") User user,
                              @Label("metaKey") String metaKey,
                              @Named("in") @Label("world") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(user);
        attachment.getDataHolder(world).removeMetadata(metaKey);
        attachment.getCurrentDataHolder().apply();
        context.sendTranslated(NEUTRAL, "Metadata {input#key} of {user} removed in {world}!", metaKey, user, world);
    }

    @Alias(value = {"clearudata","clearumeta","clearumetadata"})
    @Command(alias = {"cleardata", "clearmeta"}, desc = "Resets metadata for this user [in world]")
    public void clearMetaData(CommandContext context,
                              @Default @Label("player") User user,
                              @Named("in") @Label("world") World world)
    {
        world = this.getWorld(context, world);
        if (world == null) return;
        RolesAttachment attachment = this.manager.getRolesAttachment(user);
        attachment.getDataHolder(world).clearMetadata();
        attachment.getCurrentDataHolder().apply();
        context.sendTranslated(NEUTRAL, "Metadata of {user} cleared in {world}!", user, world);
    }
}
