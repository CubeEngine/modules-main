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

import org.bukkit.World;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command_old.reflected.Alias;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.config.Priority;
import de.cubeisland.engine.module.roles.config.PriorityConverter;
import de.cubeisland.engine.module.roles.exception.CircularRoleDependencyException;
import de.cubeisland.engine.module.roles.role.DataStore.PermissionValue;
import de.cubeisland.engine.module.roles.role.Role;
import de.cubeisland.engine.module.roles.role.RoleProvider;
import de.cubeisland.engine.module.roles.role.WorldRoleProvider;
import de.cubeisland.engine.reflect.codec.converter.Converter;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.node.StringNode;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Command(name = "role", desc = "Manage roles", alias = "manrole")
public class RoleManagementCommands extends RoleCommandHelper
{
    public RoleManagementCommands(Roles module)
    {
        super(module);
    }

    @Alias(names = "setrperm")
    @Command(alias = "setperm",  desc = "Sets the permission for given role [in world]")
    @Params(positional = {@Param(label = "[g:]role"),
                          @Param(label = "permission"),
                          @Param(req = false, names = {"true","false","reset"})},
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void setpermission(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        String permission = context.get(1);
        String setTo = "true";
        if (context.getPositionalCount() > 2)
        {
            setTo = context.get(2);
        }
        try
        {
            PermissionValue type = PermissionValue.valueOf(setTo.toUpperCase());
            if (type == PermissionValue.RESET)
            {
                if (global)
                {
                    context.sendTranslated(NEUTRAL, "{name#permission} has been reset for the global role {name}!", permission, role.getName());
                }
                else
                {
                    context.sendTranslated(NEUTRAL, "{name#permission} has been reset for the role {name} in {world}!", permission, role.getName(), world);
                }
            }
            else if (type == PermissionValue.TRUE)
            {
                if (global)
                {
                    context.sendTranslated(POSITIVE, "{name#permission} set to {text:true:color=DARK_GREEN} for the global role {name}!", permission, role.getName());
                }
                else
                {
                    context.sendTranslated(POSITIVE, "{name#permission} set to {text:true:color=DARK_GREEN} for the role {name} in {world}!", permission, role.getName(), world);
                }
            }
            else if (type == PermissionValue.FALSE)
            {
                if (global)
                {
                    context.sendTranslated(NEGATIVE, "{name#permission} set to {text:false:color=DARK_RED} for the global role {name}!", permission, role.getName());
                }
                else
                {
                    context.sendTranslated(NEGATIVE, "{name#permission} set to {text:false:color=DARK_RED} for the role {name} in {world}!", permission, role.getName(), world);
                }
            }
            role.setPermission(permission, type);
            role.save();
        }
        catch (IllegalArgumentException e)
        {
            context.sendTranslated(NEGATIVE, "Unknown setting: {input} Use {text:true},{text:false} or {text:reset}!", setTo);
        }
    }

    @Alias(names = "setrdata")
    @Command(alias = {"setdata", "setmeta"}, desc = "Sets the metadata for given role [in world]")
    @Params(positional = {@Param(label = "[g:]role"),
                          @Param(label = "key"),
                          @Param(req = false, label = "value")},
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void setmetadata(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        String key = context.get(1);
        String value = context.get(2);
        role.setMetadata(key, value);
        role.save();
        if (value == null)
        {
            if (global)
            {
                context.sendTranslated(NEUTRAL, "Metadata {input#key} reset for the global role {name}!", key, role.getName());
                return;
            }
            context.sendTranslated(NEUTRAL, "Metadata {input#key} reset for the role {name} in {world}!", key, role.getName(), world);
            return;
        }
        if (global)
        {
            context.sendTranslated(POSITIVE, "Metadata {input#key} set to {input#value} for the global role {name}!", key, value, role.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "Metadata {input#key} set to {input#value} for the role {name} in {world}!", key, value, role.getName(), world);
    }

    @Alias(names = "resetrdata")
    @Command(alias = {"resetdata", "resetmeta"}, desc = "Resets the metadata for given role [in world]")
    @Params(positional = {@Param(label = "[g:]role"),
                          @Param(label = "key")},
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void resetmetadata(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        String key = context.get(1);
        role.setMetadata(key,null);
        role.save();
        if (global)
        {
            context.sendTranslated(NEUTRAL, "Metadata {input#key} reset for the global role {name}!", key, role.getName());
        }
        else
        {
            context.sendTranslated(NEUTRAL, "Metadata {input#key} reset for the role {name} in {world}!", key, role.getName(), world);
        }
    }

    @Alias(names = "clearrdata")
    @Command(alias = {"cleardata", "clearmeta"}, desc = "Clears the metadata for given role [in world]")
    @Params(positional = @Param(label = "[g:]role"),
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void clearmetadata(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        role.clearMetadata();
        role.save();
        if (global)
        {
            context.sendTranslated(NEUTRAL, "Metadata cleared for the global role {name}!", role.getName());
            return;
        }
        context.sendTranslated(NEUTRAL, "Metadata cleared for the role {name} in {world}!", role.getName(), world);
    }

    @Alias(names = {"addrparent","manradd"})
    @Command(desc = "Adds a parent role to given role [in world]")
    @Params(positional = {@Param(label = "[g:]role"),
                          @Param(label = "[g:]parentrole")},
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void addParent(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        Role pRole = provider.getRole(context.getString(1));
        if (pRole == null)
        {
            if (global)
            {
                context.sendTranslated(NEUTRAL, "Could not find the global parent role {name}.", context.get(1));
                return;
            }
            context.sendTranslated(NEUTRAL, "Could not find the parent role {name} in {world}.", context.get(1), world);
            return;
        }
        try
        {
            if (role.assignRole(pRole))
            {
                role.save();
                if (global)
                {
                    if (pRole.isGlobal())
                    {
                        context.sendTranslated(NEGATIVE, "{name#role} is a global role and cannot inherit from a non-global role!", role.getName());
                        return;
                    }
                    context.sendTranslated(POSITIVE, "Added {name#role} as parent role for the global role {name}!", pRole.getName(), role.getName());
                    return;
                }
                context.sendTranslated(POSITIVE, "Added {name#role} as parent role for the role {name} in {world}", pRole.getName(), role.getName(), world);
                return;
            }
            if (global)
            {
                context.sendTranslated(NEUTRAL, "{name#role} is already parent role of the global role {name}!", pRole.getName(), role.getName());
                return;
            }
            context.sendTranslated(NEUTRAL, "{name#role} is already parent role of the role {name} in {world}!", pRole.getName(), role.getName(), world);
        }
        catch (CircularRoleDependencyException ex)
        {
            context.sendTranslated(NEGATIVE, "Circular Dependency! {name#role} depends on the role {name}!", pRole.getName(), role.getName());
        }
    }

    @Alias(names = "remrparent")
    @Command(desc = "Removes a parent role from given role [in world]")
    @Params(positional = {@Param(label = "[g:]role"),
                          @Param(label = "[g:]parentrole")},
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void removeParent(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        Role pRole = provider.getRole(context.getString(1));
        if (pRole == null)
        {
            if (global)
            {
                context.sendTranslated(NEUTRAL, "Could not find the global parent role {name}.", context.get(1));
                return;
            }
            context.sendTranslated(NEUTRAL, "Could not find the parent role {name} in {world}.", context.get(1), world);
            return;
        }
        if (role.removeRole(pRole))
        {
            role.save();
            if (global)
            {
                context.sendTranslated(POSITIVE, "Removed the parent role {name} from the global role {name}!", pRole.getName(), role.getName());
                return;
            }
            context.sendTranslated(POSITIVE, "Removed the parent role {name} from the role {name} in {world}!", pRole.getName(), role.getName(), world);
            return;
        }
        if (global)
        {
            context.sendTranslated(NEUTRAL, "{name#role} is not a parent role of the global role {name}!", pRole.getName(), role.getName());
            return;
        }
        context.sendTranslated(NEUTRAL, "{name#role} is not a parent role of the role {name} in {world}!", pRole.getName(), role.getName(), world);
    }

    @Alias(names = "clearrparent")
    @Command(desc = "Removes all parent roles from given role [in world]")
    @Params(positional = @Param(label = "[g:]role"),
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void clearParent(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        role.clearRoles();
        role.save();
        if (global)
        {
            context.sendTranslated(NEUTRAL, "All parent roles of the global role {name} cleared!", role.getName());
            return;
        }
        context.sendTranslated(NEUTRAL, "All parent roles of the role {name} in {world} cleared!", role.getName(), world);
    }

    @Alias(names = "setrolepriority")
    @Command(alias = "setprio", desc = "Sets the priority of given role [in world]")
    @Params(positional = {@Param(label = "[g:]role"),
                          @Param(label = "priority")},
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void setPriority(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        Converter<Priority> converter = new PriorityConverter();
        Priority priority;
        try
        {
            priority = converter.fromNode(new StringNode(context.getString(1)), null);
            role.setPriorityValue(priority.value);
            role.save();
            if (global)
            {
                context.sendTranslated(POSITIVE, "Priority of the global role {name} set to {input#priority}!", role.getName(), context.get(
                    1));
                return;
            }
            context.sendTranslated(POSITIVE, "Priority of the role {name} set to {input#priority} in {world}!", role.getName(), context.get(
                1), world);
        }
        catch (ConversionException ex)
        {
            context.sendTranslated(NEGATIVE, "{input#priority} is not a valid priority!", context.get(1));
        }

    }

    @Alias(names = "renamerole")
    @Command(desc = "Renames given role [in world]")
    @Params(positional = {@Param(label = "[g:]role"),
                          @Param(label = "new name")},
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void rename(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        String newName = context.get(1);
        String oldName = role.getName();
        if (role.getName().equalsIgnoreCase(newName))
        {
            context.sendTranslated(NEGATIVE, "These are the same names!");
            return;
        }
        if (role.rename(newName))
        {
            if (global)
            {
                context.sendTranslated(POSITIVE, "Global role {name} renamed to {name#new}!", oldName, newName);
                return;
            }
            context.sendTranslated(POSITIVE, "{name#role} renamed to {name#new} in {world}", oldName, newName, world);
            return;
        }
        if (global)
        {
            context.sendTranslated(NEGATIVE, "Renaming failed! The role global {name} already exists!", newName);
            return;
        }
        context.sendTranslated(NEGATIVE, "Renaming failed! The role {name} already exists in {world}!", newName, world);
    }

    @Alias(names = "createrole")
    @Command(desc = "Creates a new role [in world]")
    @Params(positional = @Param(label = "rolename"),
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    @Flags(@Flag(longName = "global", name = "g"))
    public void create(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = context.hasFlag("g");
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        if (provider.createRole(roleName) != null)
        {
            if (world == null)
            {
                context.sendTranslated(POSITIVE, "Global role {name} created!", roleName);
                return;
            }
            context.sendTranslated(POSITIVE, "Role {name} created!", roleName);
            return;
        }
        if (world == null)
        {
            context.sendTranslated(NEUTRAL, "There is already a global role named {name}.", roleName);
            return;
        }
        context.sendTranslated(NEUTRAL, "There is already a role named {name} in {world}.", roleName, world);
    }

    @Alias(names = "deleteRole")
    @Command(desc = "Deletes a role [in world]")
    @Params(positional = @Param(label = "[g:]rolename"),
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void delete(CommandContext context)
    {
        String roleName = context.get(0);
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        World world = global ? null : this.getWorld(context);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        role.delete();
        this.manager.recalculateAllRoles();
        if (global)
        {
            context.sendTranslated(POSITIVE, "Global role {name} deleted!", role.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "Deleted the role {name} in {world}!", role.getName(), world);
    }


    @Command(alias = {"toggledefault", "toggledef"}, desc = "Toggles whether given role is a default role [in world]")
    @Params(positional = @Param(label = "rolename"),
            nonpositional = @Param(names = "in", label = "world", type = World.class))
    public void toggleDefaultRole(CommandContext context)
    {
        String roleName = context.get(0);
        World world = this.getWorld(context);
        if (world == null) return;
        WorldRoleProvider provider = this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        role.setDefaultRole(!role.isDefaultRole());
        this.manager.recalculateAllRoles();
        if (role.isDefaultRole())
        {
            context.sendTranslated(POSITIVE, "{name#role} is now a default role in {world}!", role.getName(), world);
            return;
        }
        context.sendTranslated(POSITIVE, "{name#role} is no longer a default role in {world}!", role.getName(), world);
    }
}
