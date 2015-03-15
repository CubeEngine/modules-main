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

import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.parametric.Command;
import de.cubeisland.engine.command.parametric.Flag;
import de.cubeisland.engine.command.parametric.Default;
import de.cubeisland.engine.command.parametric.Label;
import de.cubeisland.engine.command.parametric.Named;
import de.cubeisland.engine.command.parametric.Optional;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.converter.ClassedConverter;
import de.cubeisland.engine.converter.node.StringNode;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.config.Priority;
import de.cubeisland.engine.module.roles.config.PriorityConverter;
import de.cubeisland.engine.module.roles.exception.CircularRoleDependencyException;
import de.cubeisland.engine.module.roles.role.DataStore.PermissionValue;
import de.cubeisland.engine.module.roles.role.Role;
import de.cubeisland.engine.module.roles.role.RoleProvider;
import de.cubeisland.engine.module.roles.role.WorldRoleProvider;
import org.bukkit.World;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Alias("manrole")
@Command(name = "role", desc = "Manage roles")
public class RoleManagementCommands extends RoleCommandHelper
{
    public RoleManagementCommands(Roles module)
    {
        super(module);
    }

    @Alias("setrperm")
    @Command(alias = "setperm",  desc = "Sets the permission for given role [in world]")
    public void setpermission(CommandContext context, @Label("[g:]role") String roleName,
                              String permission,
                              @Default PermissionValue type,
                              @Named("in") World world)
    {
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        world = global ? null : this.getWorld(context, world);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        switch (type)
        {
            case RESET:
                if (global)
                {
                    context.sendTranslated(NEUTRAL, "{name#permission} has been reset for the global role {name}!", permission, role.getName());
                }
                else
                {
                    context.sendTranslated(NEUTRAL, "{name#permission} has been reset for the role {name} in {world}!", permission, role.getName(), world);
                }
                break;
            case TRUE:
                if (global)
                {
                    context.sendTranslated(POSITIVE, "{name#permission} set to {text:true:color=DARK_GREEN} for the global role {name}!", permission, role.getName());
                }
                else
                {
                    context.sendTranslated(POSITIVE, "{name#permission} set to {text:true:color=DARK_GREEN} for the role {name} in {world}!", permission, role.getName(), world);
                }
                break;
            case FALSE:
                if (global)
                {
                    context.sendTranslated(NEGATIVE, "{name#permission} set to {text:false:color=DARK_RED} for the global role {name}!", permission, role.getName());
                }
                else
                {
                    context.sendTranslated(NEGATIVE, "{name#permission} set to {text:false:color=DARK_RED} for the role {name} in {world}!", permission, role.getName(), world);
                }
                break;
        }
        role.setPermission(permission, type);
        role.save();
    }

    @Alias(value = "setrdata")
    @Command(alias = {"setdata", "setmeta"}, desc = "Sets the metadata for given role [in world]")
    public void setmetadata(CommandContext context, @Label("[g:]role") String roleName,
                            String key,
                            @Optional String value,
                            @Named("in") World world)
    {
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        world = global ? null : this.getWorld(context, world);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
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

    @Alias(value = "resetrdata")
    @Command(alias = {"resetdata", "resetmeta"}, desc = "Resets the metadata for given role [in world]")
    public void resetmetadata(CommandContext context, @Label("[g:]role") String roleName,
                              String key,
                              @Named("in") World world)
    {
        this.setmetadata(context, roleName, key, null, world);
    }

    @Alias(value = "clearrdata")
    @Command(alias = {"cleardata", "clearmeta"}, desc = "Clears the metadata for given role [in world]")
    public void clearmetadata(CommandContext context, @Label("[g:]role") String roleName, @Named("in") World world)
    {
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        world = global ? null : this.getWorld(context, world);
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

    @Alias(value = {"addrparent","manradd"})
    @Command(desc = "Adds a parent role to given role [in world]")
    public void addParent(CommandContext context, @Label("[g:]role") String roleName,
                          @Label("[g:]parentrole") String parentRoleName,
                          @Named("in") World world)
    {
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        world = global ? null : this.getWorld(context, world);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        Role pRole = provider.getRole(parentRoleName);
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
            if (!role.assignRole(pRole))
            {
                if (global)
                {
                    context.sendTranslated(NEUTRAL, "{name#role} is already parent role of the global role {name}!", pRole.getName(), role.getName());
                    return;
                }
                context.sendTranslated(NEUTRAL, "{name#role} is already parent role of the role {name} in {world}!", pRole.getName(), role.getName(), world);
                return;
            }
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
        }
        catch (CircularRoleDependencyException ex)
        {
            context.sendTranslated(NEGATIVE, "Circular Dependency! {name#role} depends on the role {name}!", pRole.getName(), role.getName());
        }
    }

    @Alias(value = "remrparent")
    @Command(desc = "Removes a parent role from given role [in world]")
    public void removeParent(CommandContext context, @Label("[g:]role") String roleName,
                             @Label("[g:]parentrole") String parentRoleName,
                             @Named("in") World world)
    {
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        world = global ? null : this.getWorld(context, world);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        Role pRole = provider.getRole(parentRoleName);
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

    @Alias(value = "clearrparent")
    @Command(desc = "Removes all parent roles from given role [in world]")
    public void clearParent(CommandContext context, @Label("[g:]role") String roleName,
                            @Named("in") World world)
    {
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        world = global ? null : this.getWorld(context, world);
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

    @Alias(value = "setrolepriority")
    @Command(alias = "setprio", desc = "Sets the priority of given role [in world]")
    public void setPriority(CommandContext context, @Label("[g:]role") String roleName,
                            String priority,
                            @Named("in") World world)
    {
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        world = global ? null : this.getWorld(context, world);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
        ClassedConverter<Priority> converter = new PriorityConverter();
        Priority prio;
        try
        {
            prio = converter.fromNode(new StringNode(priority), Priority.class, null);
            role.setPriorityValue(prio.value);
            role.save();
            if (global)
            {
                context.sendTranslated(POSITIVE, "Priority of the global role {name} set to {input#priority}!", role.getName(), priority);
                return;
            }
            context.sendTranslated(POSITIVE, "Priority of the role {name} set to {input#priority} in {world}!", role.getName(), priority, world);
        }
        catch (ConversionException ex)
        {
            context.sendTranslated(NEGATIVE, "{input#priority} is not a valid priority!", context.get(1));
        }

    }

    @Alias(value = "renamerole")
    @Command(desc = "Renames given role [in world]")
    public void rename(CommandContext context, @Label("[g:]role") String roleName,
                       @Label("new name") String newName,
                       @Named("in") World world)
    {
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        world = global ? null : this.getWorld(context, world);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        Role role = this.getRole(context, provider, roleName, world);
        if (role == null) return;
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

    @Alias(value = "createrole")
    @Command(desc = "Creates a new role [in world]")
    public void create(CommandContext context, String rolename, @Named("in") World world, @Flag boolean global)
    {
        world = global ? null : this.getWorld(context, world);
        if (!global && world == null) return;
        RoleProvider provider = world == null ? this.manager.getGlobalProvider() : this.manager.getProvider(world);
        if (provider.createRole(rolename) != null)
        {
            if (world == null)
            {
                context.sendTranslated(POSITIVE, "Global role {name} created!", rolename);
                return;
            }
            context.sendTranslated(POSITIVE, "Role {name} created!", rolename);
            return;
        }
        if (world == null)
        {
            context.sendTranslated(NEUTRAL, "There is already a global role named {name}.", rolename);
            return;
        }
        context.sendTranslated(NEUTRAL, "There is already a role named {name} in {world}.", rolename, world);
    }

    @Alias(value = "deleteRole")
    @Command(desc = "Deletes a role [in world]")
    public void delete(CommandContext context, @Label("[g:]role") String roleName, @Named("in") World world)
    {
        boolean global = roleName.startsWith(GLOBAL_PREFIX);
        world = global ? null : this.getWorld(context, world);
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
    public void toggleDefaultRole(CommandContext context, @Label("[g:]role") String roleName, @Named("in") World world)
    {
        world = this.getWorld(context, world);
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
