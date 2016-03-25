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

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.converter.ClassedConverter;
import de.cubeisland.engine.converter.node.StringNode;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.config.PriorityConverter;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import static org.cubeengine.module.roles.commands.RoleCommands.toSet;
import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

@Alias("manrole")
@Command(name = "role", desc = "Manage roles")
public class RoleManagementCommands extends ContainerCommand
{
    private RolesPermissionService service;
    private I18n i18n;

    public RoleManagementCommands(Roles module, RolesPermissionService service, I18n i18n)
    {
        super(module);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias("setrperm")
    @Command(alias = "setperm", desc = "Sets the permission for given role")
    public void setpermission(CommandSource ctx, RoleSubject role,
                              @Complete(PermissionCompleter.class) String permission,
                              @Default Tristate type,
                              @Named("in") @Default Context context)
    {
        role.getSubjectData().setPermission(toSet(context), permission, type);
        switch (type)
        {
            case UNDEFINED:
                i18n.sendTranslated(ctx, NEUTRAL, "{name#permission} has been reset for the role {role} in {context}!",
                                        permission, role, context);
                break;
            case TRUE:
                i18n.sendTranslated(ctx, POSITIVE,
                                        "{name#permission} set to {text:true:color=DARK_GREEN} for the role {role} in {context}!",
                                        permission, role, context);
                break;
            case FALSE:
                i18n.sendTranslated(ctx, NEGATIVE,
                                        "{name#permission} set to {text:false:color=DARK_RED} for the role {naroleme} in {context}!",
                                        permission, role, context);
                break;
        }
    }

    @Alias(value = "setrdata")
    @Command(alias = {"setdata", "setmeta"}, desc = "Sets the metadata for given role")
    public void setmetadata(CommandSource ctx, RoleSubject role,
                            String key,
                            @Optional String value,
                            @Named("in") @Default Context context)
    {
        role.getSubjectData().setOption(toSet(context), key, value);
        if (value == null)
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Metadata {input#key} reset for the role {role} in {context}!",
                                    key, role, context);
            return;
        }
        i18n.sendTranslated(ctx, POSITIVE, "Metadata {input#key} set to {input#value} for the role {role} in {context}!",
                                key, value, role, context);
    }

    @Alias(value = "resetrdata")
    @Command(alias = {"resetdata", "resetmeta"}, desc = "Resets the metadata for given role")
    public void resetmetadata(CommandSource ctx, RoleSubject role, String key, @Named("in") @Default Context context)
    {
        this.setmetadata(ctx, role, key, null, context);
    }

    @Alias(value = "clearrdata")
    @Command(alias = {"cleardata", "clearmeta"}, desc = "Clears the metadata for given role")
    public void clearmetadata(CommandSource ctx, RoleSubject role, @Named("in") @Default Context context)
    {
        role.getSubjectData().clearOptions(toSet(context));
        i18n.sendTranslated(ctx, NEUTRAL, "Metadata cleared for the role {role} in {context}!", role, context);
    }

    @Alias(value = {"addrparent", "manradd"})
    @Command(desc = "Adds a parent role to given role")
    public void addParent(CommandSource ctx, RoleSubject role, RoleSubject parentRole, @Named("in") @Default Context context)
    {
        if (role.getSubjectData().addParent(toSet(context), parentRole))
        {
            i18n.sendTranslated(ctx, POSITIVE, "Added {name#role} as parent role for the role {role} in {context}",
                    parentRole, role, context);
            return;
        }
        i18n.sendTranslated(ctx, NEUTRAL, "{name#role} is already parent role of the role {role} in {context}!",
                parentRole, role, context);
        // TODO i18n.sendTranslated(ctx, NEGATIVE, "Circular Dependency! {name#role} depends on the role {name}!", pr.getName(), r.getName());
    }

    @Alias(value = "remrparent")
    @Command(desc = "Removes a parent role from given role")
    public void removeParent(CommandSource ctx, RoleSubject role, RoleSubject parentRole, @Named("in") @Default Context context)
    {
        if (role.getSubjectData().removeParent(toSet(context), parentRole))
        {
            i18n.sendTranslated(ctx, NEUTRAL, "{role} is not a parent role of the role {role} in {context}!", parentRole, role, context);
            return;
        }
        i18n.sendTranslated(ctx, POSITIVE, "Removed the parent role {role} from the role {role} in {context}!", parentRole, role, context);
    }

    @Alias(value = "clearrparent")
    @Command(desc = "Removes all parent roles from given role")
    public void clearParent(CommandSource ctx, RoleSubject role, @Named("in") @Default Context context)
    {
        if (role.getSubjectData().clearParents(toSet(context)))
        {
            i18n.sendTranslated(ctx, NEUTRAL, "All parent roles of the role {role} in {context} cleared!", role, context);
            return;
        }
        // TODO msg
    }

    @Alias(value = "setrolepriority")
    @Command(alias = "setprio", desc = "Sets the priority of given role")
    public void setPriority(CommandSource ctx, RoleSubject role, String priority)
    {
        try
        {
            ClassedConverter<Priority> converter = new PriorityConverter();
            Priority prio = converter.fromNode(new StringNode(priority), Priority.class, null);
            role.setPriorityValue(prio.value); // TODO set priority
            i18n.sendTranslated(ctx, POSITIVE, "Priority of the role {role} set to {input#priority}!", role, priority);
        }
        catch (ConversionException ex)
        {
            i18n.sendTranslated(ctx, NEGATIVE, "{input#priority} is not a valid priority!", priority);
        }
    }

    @Alias(value = "renamerole")
    @Command(desc = "Renames given role")
    public void rename(CommandSource ctx, RoleSubject role, @Label("new name") String newName)
    {
        String oldName = role.getName();
        if (oldName.equalsIgnoreCase(newName))
        {
            i18n.sendTranslated(ctx, NEGATIVE, "These are the same names!");
            return;
        }
        if (service.getGroupSubjects().rename(role, newName))
        {
            i18n.sendTranslated(ctx, POSITIVE, "{role} renamed to {name#new}", role, newName);
            return;
        }
        i18n.sendTranslated(ctx, NEGATIVE, "Renaming failed! The role {name} already exists!", newName);
    }

    @Alias(value = "createrole")
    @Command(desc = "Creates a new role [in context]")
    public void create(CommandSource ctx, String name)
    {
        if (service.getGroupSubjects().hasRegistered("role:" + name))
        {
            i18n.sendTranslated(ctx, NEUTRAL, "There is already a role named {name}.", name);
            return;
        }
        RoleSubject r = service.getGroupSubjects().get("role:" + name);
        r.getSubjectData().save(true); // TODO force save
        i18n.sendTranslated(ctx, POSITIVE, "Role {name} created!", name);
    }

    @Alias(value = "deleteRole")
    @Command(desc = "Deletes a role")
    public void delete(CommandSource ctx, RoleSubject role, @Flag boolean force)
    {
        if (service.getGroupSubjects().delete(role, force))
        {
            i18n.sendTranslated(ctx, POSITIVE, "Deleted the role {role}!", role);
            return;
        }
        i18n.sendTranslated(ctx, NEGATIVE, "Role is still in use! Use the -force flag to delete the role and all occurrences");
    }

    @Command(alias = {"toggledefault", "toggledef"}, desc = "Toggles whether given role is a default role")
    public void toggleDefaultRole(CommandSource ctx, RoleSubject role)
    {
        SubjectData defaultData = service.getDefaultData();
        if (defaultData.getParents(GLOBAL_CONTEXT).contains(role))
        {
            defaultData.removeParent(GLOBAL_CONTEXT, role);
            i18n.sendTranslated(ctx, POSITIVE, "{role} is no longer a default role!", role);
            return;
        }
        defaultData.addParent(GLOBAL_CONTEXT, role);
        i18n.sendTranslated(ctx, POSITIVE, "{role} is now a default role!", role);
    }
}
