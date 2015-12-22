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

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.*;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.converter.ClassedConverter;
import de.cubeisland.engine.converter.node.StringNode;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.config.PriorityConverter;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.subject.RoleSubject;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.ContainerCommand;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

import static org.cubeengine.module.roles.commands.RoleCommands.toSet;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

@Alias("manrole")
@Command(name = "role", desc = "Manage roles")
public class RoleManagementCommands extends ContainerCommand
{
    private RolesPermissionService service;

    public RoleManagementCommands(Roles module, RolesPermissionService service)
    {
        super(module);
        this.service = service;
    }

    @Alias("setrperm")
    @Command(alias = "setperm", desc = "Sets the permission for given role")
    public void setpermission(CommandContext cContext, RoleSubject role,
                              @Complete(PermissionCompleter.class) String permission,
                              @Default Tristate type,
                              @Named("in") @Default Context context)
    {
        role.getSubjectData().setPermission(toSet(context), permission, type);
        switch (type)
        {
            case UNDEFINED:
                cContext.sendTranslated(NEUTRAL, "{name#permission} has been reset for the role {role} in {context}!",
                                        permission, role, context);
                break;
            case TRUE:
                cContext.sendTranslated(POSITIVE,
                                        "{name#permission} set to {text:true:color=DARK_GREEN} for the role {role} in {context}!",
                                        permission, role, context);
                break;
            case FALSE:
                cContext.sendTranslated(NEGATIVE,
                                        "{name#permission} set to {text:false:color=DARK_RED} for the role {naroleme} in {context}!",
                                        permission, role, context);
                break;
        }
    }

    @Alias(value = "setrdata")
    @Command(alias = {"setdata", "setmeta"}, desc = "Sets the metadata for given role")
    public void setmetadata(CommandContext cContext, RoleSubject role,
                            String key,
                            @Optional String value,
                            @Named("in") @Default Context context)
    {
        role.getSubjectData().setOption(toSet(context), key, value);
        if (value == null)
        {
            cContext.sendTranslated(NEUTRAL, "Metadata {input#key} reset for the role {role} in {context}!",
                                    key, role, context);
            return;
        }
        cContext.sendTranslated(POSITIVE, "Metadata {input#key} set to {input#value} for the role {role} in {context}!",
                                key, value, role, context);
    }

    @Alias(value = "resetrdata")
    @Command(alias = {"resetdata", "resetmeta"}, desc = "Resets the metadata for given role")
    public void resetmetadata(CommandContext cContext, RoleSubject role, String key, @Named("in") @Default Context context)
    {
        this.setmetadata(cContext, role, key, null, context);
    }

    @Alias(value = "clearrdata")
    @Command(alias = {"cleardata", "clearmeta"}, desc = "Clears the metadata for given role")
    public void clearmetadata(CommandContext cContext, RoleSubject role, @Named("in") @Default Context context)
    {
        role.getSubjectData().clearOptions(toSet(context));
        cContext.sendTranslated(NEUTRAL, "Metadata cleared for the role {role} in {context}!", role, context);
    }

    @Alias(value = {"addrparent", "manradd"})
    @Command(desc = "Adds a parent role to given role")
    public void addParent(CommandContext cContext, RoleSubject role, RoleSubject parentRole, @Named("in") @Default Context context)
    {
        if (role.getSubjectData().addParent(toSet(context), parentRole))
        {
            cContext.sendTranslated(POSITIVE, "Added {name#role} as parent role for the role {role} in {context}",
                    parentRole, role, context);
            return;
        }
        cContext.sendTranslated(NEUTRAL, "{name#role} is already parent role of the role {role} in {context}!",
                parentRole, role, context);
        // TODO context.sendTranslated(NEGATIVE, "Circular Dependency! {name#role} depends on the role {name}!", pr.getName(), r.getName());
    }

    @Alias(value = "remrparent")
    @Command(desc = "Removes a parent role from given role")
    public void removeParent(CommandContext cContext, RoleSubject role, RoleSubject parentRole, @Named("in") @Default Context context)
    {
        if (role.getSubjectData().removeParent(toSet(context), parentRole))
        {
            cContext.sendTranslated(NEUTRAL, "{role} is not a parent role of the role {role} in {context}!", parentRole, role, context);
            return;
        }
        cContext.sendTranslated(POSITIVE, "Removed the parent role {role} from the role {role} in {context}!", parentRole, role, context);
    }

    @Alias(value = "clearrparent")
    @Command(desc = "Removes all parent roles from given role")
    public void clearParent(CommandContext cContext, RoleSubject role, @Named("in") @Default Context context)
    {
        if (role.getSubjectData().clearParents(toSet(context)))
        {
            cContext.sendTranslated(NEUTRAL, "All parent roles of the role {role} in {context} cleared!", role, context);
            return;
        }
        // TODO msg
    }

    @Alias(value = "setrolepriority")
    @Command(alias = "setprio", desc = "Sets the priority of given role")
    public void setPriority(CommandContext cContext, RoleSubject role, String priority)
    {
        try
        {
            ClassedConverter<Priority> converter = new PriorityConverter();
            Priority prio = converter.fromNode(new StringNode(priority), Priority.class, null);
            role.setPriorityValue(prio.value); // TODO set priority
            cContext.sendTranslated(POSITIVE, "Priority of the role {role} set to {input#priority}!", role, priority);
        }
        catch (ConversionException ex)
        {
            cContext.sendTranslated(NEGATIVE, "{input#priority} is not a valid priority!", priority);
        }
    }

    @Alias(value = "renamerole")
    @Command(desc = "Renames given role")
    public void rename(CommandContext context, RoleSubject role, @Label("new name") String newName)
    {
        String oldName = role.getName();
        if (oldName.equalsIgnoreCase(newName))
        {
            context.sendTranslated(NEGATIVE, "These are the same names!");
            return;
        }
        if (service.getGroupSubjects().rename(role, newName))
        {
            context.sendTranslated(POSITIVE, "{role} renamed to {name#new}", role, newName);
            return;
        }
        context.sendTranslated(NEGATIVE, "Renaming failed! The role {name} already exists!", newName);
    }

    @Alias(value = "createrole")
    @Command(desc = "Creates a new role [in context]")
    public void create(CommandContext cContext, String name)
    {
        if (service.getGroupSubjects().hasRegistered("role:" + name))
        {
            cContext.sendTranslated(NEUTRAL, "There is already a role named {name}.", name);
            return;
        }
        RoleSubject r = service.getGroupSubjects().get("role:" + name);
        r.getSubjectData().save(true); // TODO force save
        cContext.sendTranslated(POSITIVE, "Role {name} created!", name);
    }

    @Alias(value = "deleteRole")
    @Command(desc = "Deletes a role")
    public void delete(CommandContext context, RoleSubject role, @Flag boolean force)
    {
        if (service.getGroupSubjects().delete(role, force))
        {
            context.sendTranslated(POSITIVE, "Deleted the role {role}!", role);
            return;
        }
        context.sendTranslated(NEGATIVE, "Role is still in use! Use the -force flag to delete the role and all occurrences");
    }

    @Command(alias = {"toggledefault", "toggledef"}, desc = "Toggles whether given role is a default role")
    public void toggleDefaultRole(CommandContext context, RoleSubject role)
    {
        SubjectData defaultData = service.getDefaultData();
        if (defaultData.getParents(GLOBAL_CONTEXT).contains(role))
        {
            defaultData.removeParent(GLOBAL_CONTEXT, role);
            context.sendTranslated(POSITIVE, "{role} is no longer a default role!", role);
            return;
        }
        defaultData.addParent(GLOBAL_CONTEXT, role);
        context.sendTranslated(POSITIVE, "{role} is now a default role!", role);
    }
}
