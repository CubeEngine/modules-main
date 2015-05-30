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

import de.cubeisland.engine.butler.alias.Alias;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.converter.ClassedConverter;
import de.cubeisland.engine.converter.node.StringNode;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.config.Priority;
import de.cubeisland.engine.module.roles.config.PriorityConverter;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.data.RoleSubjectData;
import de.cubeisland.engine.module.roles.sponge.subject.RoleSubject;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;

@Alias("manrole")
@Command(name = "role", desc = "Manage roles")
public class RoleManagementCommands extends RoleCommandHelper
{
    private RolesPermissionService service;

    public RoleManagementCommands(Roles module, WorldManager wm, RolesPermissionService service)
    {
        super(module, wm);
        this.service = service;
    }

    @Alias("setrperm")
    @Command(alias = "setperm", desc = "Sets the permission for given role")
    public void setpermission(CommandContext cContext, ContextualRole role, String permission, @Default Tristate type)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        r.getSubjectData().setPermission(toSet(role.getContext()), permission, type);
        switch (type)
        {
            case UNDEFINED:
                cContext.sendTranslated(NEUTRAL, "{name#permission} has been reset for the role {name} in {context}!",
                                        permission, r.getName(), role.getContext());
                break;
            case TRUE:
                cContext.sendTranslated(POSITIVE,
                                        "{name#permission} set to {text:true:color=DARK_GREEN} for the role {name} in {context}!",
                                        permission, r.getName(), role.getContext());
                break;
            case FALSE:
                cContext.sendTranslated(NEGATIVE,
                                        "{name#permission} set to {text:false:color=DARK_RED} for the role {name} in {context}!",
                                        permission, r.getName(), role.getContext());
                break;
        }
    }

    @Alias(value = "setrdata")
    @Command(alias = {"setdata", "setmeta"}, desc = "Sets the metadata for given role")
    public void setmetadata(CommandContext cContext, ContextualRole role, String key, @Optional String value)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        r.getSubjectData().setOption(toSet(role.getContext()), key, value);
        if (value == null)
        {
            cContext.sendTranslated(NEUTRAL, "Metadata {input#key} reset for the role {name} in {context}!", key,
                                   r.getName(), role.getContext());
            return;
        }
        cContext.sendTranslated(POSITIVE, "Metadata {input#key} set to {input#value} for the role {name} in {context}!",
                               key, value, r.getName(), role.getContext());
    }

    @Alias(value = "resetrdata")
    @Command(alias = {"resetdata", "resetmeta"}, desc = "Resets the metadata for given role")
    public void resetmetadata(CommandContext context, ContextualRole role, String key)
    {
        this.setmetadata(context, role, key, null);
    }

    @Alias(value = "clearrdata")
    @Command(alias = {"cleardata", "clearmeta"}, desc = "Clears the metadata for given role")
    public void clearmetadata(CommandContext context, ContextualRole role)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        r.getSubjectData().clearOptions(toSet(role.getContext()));
        context.sendTranslated(NEUTRAL, "Metadata cleared for the role {name} in {context}!", r.getName(),
                               role.getContext());
    }

    @Alias(value = {"addrparent", "manradd"})
    @Command(desc = "Adds a parent role to given role")
    public void addParent(CommandContext context, ContextualRole role, ContextualRole parentRole)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        RoleSubject pr = service.getGroupSubjects().get(parentRole.getIdentifier());
        if (r.getSubjectData().addParent(toSet(role.getContext()), pr))
        {
            context.sendTranslated(POSITIVE, "Added {name#role} as parent role for the role {name} in {context}",
                                   pr.getName(), r.getName(), role.getContext());
            return;
        }
        context.sendTranslated(NEUTRAL, "{name#role} is already parent role of the role {name} in {context}!",
                               pr.getName(), r.getName(), role.getContext());
        // TODO context.sendTranslated(NEGATIVE, "Circular Dependency! {name#role} depends on the role {name}!", pr.getName(), r.getName());
    }

    @Alias(value = "remrparent")
    @Command(desc = "Removes a parent role from given role")
    public void removeParent(CommandContext context, ContextualRole role, ContextualRole parentRole)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        RoleSubject pr = service.getGroupSubjects().get(parentRole.getIdentifier());
        if (r.getSubjectData().removeParent(toSet(role.getContext()), pr))
        {
            context.sendTranslated(NEUTRAL, "{name#role} is not a parent role of the role {name} in {context}!",
                                   pr.getName(), r.getName(), role.getContext());
            return;
        }
        context.sendTranslated(POSITIVE, "Removed the parent role {name} from the role {name} in {context}!",
                               pr.getName(), r.getName(), role.getContext());
    }

    @Alias(value = "clearrparent")
    @Command(desc = "Removes all parent roles from given role")
    public void clearParent(CommandContext context, ContextualRole role)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        if (r.getSubjectData().clearParents(toSet(role.getContext())))
        {
            context.sendTranslated(NEUTRAL, "All parent roles of the role {name} in {context} cleared!", r.getName(), role.getContext());
        }
        // TODO msg
    }

    @Alias(value = "setrolepriority")
    @Command(alias = "setprio", desc = "Sets the priority of given role")
    public void setPriority(CommandContext context, ContextualRole role, String priority)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        try
        {
            ClassedConverter<Priority> converter = new PriorityConverter();
            Priority prio = converter.fromNode(new StringNode(priority), Priority.class, null);
            r.setPriorityValue(prio.value); // TODO set priority
            context.sendTranslated(POSITIVE, "Priority of the role {name} set to {input#priority} in {context}!",
                                   r.getName(), priority, role.getContext());
        }
        catch (ConversionException ex)
        {
            context.sendTranslated(NEGATIVE, "{input#priority} is not a valid priority!", priority);
        }
    }

    @Alias(value = "renamerole")
    @Command(desc = "Renames given role")
    public void rename(CommandContext context, ContextualRole role, @Label("new name") String newName)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        String oldName = r.getName();
        if (oldName.equalsIgnoreCase(newName))
        {
            context.sendTranslated(NEGATIVE, "These are the same names!");
            return;
        }
        if (role.rename(newName)) // TODO rename
        {
            context.sendTranslated(POSITIVE, "{name#role} renamed to {name#new} in {context}", oldName, newName, role.getContext());
            return;
        }
        context.sendTranslated(NEGATIVE, "Renaming failed! The role {name} already exists in {context}!", newName, role.getContext());
    }

    @Alias(value = "createrole")
    @Command(desc = "Creates a new role [in context]")
    public void create(CommandContext cContext, String name, @Named("in") Context context, @Flag boolean global)
    {
        ContextualRole role = new ContextualRole();
        role.roleName = name;
        role.contextType = context.getType();
        role.contextName = context.getName();
        if (service.getGroupSubjects().hasRegistered(role.getIdentifier()))
        {
            cContext.sendTranslated(NEUTRAL, "There is already a role named {name} in {context}.", name, context);
            return;
        }
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        ((RoleSubjectData)r.getSubjectData()).save(true); // TODO force save
        cContext.sendTranslated(POSITIVE, "Role {name} created!", name);
    }

    @Alias(value = "deleteRole")
    @Command(desc = "Deletes a role")
    public void delete(CommandContext context, ContextualRole role)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        role.delete(); // TODO delete
        context.sendTranslated(POSITIVE, "Deleted the role {name} in {context}!", r.getName(), role.getContext());
    }


    @Command(alias = {"toggledefault", "toggledef"}, desc = "Toggles whether given role is a default role")
    public void toggleDefaultRole(CommandContext context, ContextualRole role)
    {
        RoleSubject r = service.getGroupSubjects().get(role.getIdentifier());
        role.setDefaultRole(!role.isDefaultRole());  // TODO defaultRole
        if (role.isDefaultRole())
        {
            context.sendTranslated(POSITIVE, "{name#role} is now a default role in {context}!", r.getName(), role.getContext());
            return;
        }
        context.sendTranslated(POSITIVE, "{name#role} is no longer a default role in {context}!", r.getName(), role.getContext());
    }
}
