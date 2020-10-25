/*
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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.libcube.util.ContextUtil.toSet;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.converter.ClassedConverter;
import org.cubeengine.converter.node.StringNode;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.config.PriorityConverter;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.data.FileSubjectData;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.concurrent.CompletableFuture;

@Alias("manrole")
@Command(name = "role", desc = "Manage roles")
public class RoleManagementCommands extends ContainerCommand
{
    private RolesPermissionService service;
    private I18n i18n;

    public RoleManagementCommands(CommandManager base, RolesPermissionService service, I18n i18n)
    {
        super(base, Roles.class);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias("setRPerm")
    @Command(alias = "setPerm", desc = "Sets the permission for given role [in context]")
    public void setPermission(CommandSource ctx, FileSubject role,
                              @Complete(PermissionCompleter.class) String permission,
                              @Default Tristate type,
                              @Named("in") @Default Context context)
    {
        role.getSubjectData().setPermission(toSet(context), permission, type).thenAccept(b -> {
            switch (type)
            {
                case UNDEFINED:
                    i18n.send(ctx, NEUTRAL, "{name#permission} has been reset for the role {role} in {context}!",
                            permission, role, context);
                    break;
                case TRUE:
                    i18n.send(ctx, POSITIVE,
                            "{name#permission} set to {text:true:color=DARK_GREEN} for the role {role} in {context}!",
                            permission, role, context);
                    break;
                case FALSE:
                    i18n.send(ctx, NEGATIVE,
                            "{name#permission} set to {text:false:color=DARK_RED} for the role {role} in {context}!",
                            permission, role, context);
                    break;
            }
        });

    }

    @Alias(value = {"setROption", "setRData"})
    @Command(alias = "setData", desc = "Sets an option for given role [in context]")
    public void setOption(CommandSource ctx, FileSubject role, String key, @Optional String value, @Named("in") @Default Context context)
    {
        role.getSubjectData().setOption(toSet(context), key, value);
        if (value == null)
        {
            i18n.send(ctx, NEUTRAL, "Options {input#key} reset for the role {role} in {context}!", key, role, context);
            return;
        }
        i18n.send(ctx, POSITIVE, "Options {input#key} set to {input#value} for the role {role} in {context}!", key, value, role, context);
    }

    @Alias(value = {"resetROption", "resetRData"})
    @Command(alias = "resetData", desc = "Resets the options for given role [in context]")
    public void resetOption(CommandSource ctx, FileSubject role, String key, @Named("in") @Default Context context)
    {
        this.setOption(ctx, role, key, null, context);
    }

    @Alias(value = {"clearROption", "clearRData"})
    @Command(alias = "clearData", desc = "Clears the options for given role [in context]")
    public void clearOption(CommandSource ctx, FileSubject role, @Named("in") @Default Context context)
    {
        role.getSubjectData().clearOptions(toSet(context));
        i18n.send(ctx, NEUTRAL, "Options cleared for the role {role} in {context}!", role, context);
    }

    @Alias(value = {"addRParent", "manRAdd"})
    @Command(desc = "Adds a parent role to given role [in context]")
    public void addParent(CommandSource ctx, FileSubject role, FileSubject parentRole, @Named("in") @Default Context context)
    {
        role.getSubjectData().addParent(toSet(context), parentRole.asSubjectReference()).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, POSITIVE, "Added {role} as parent role for the role {role} in {context}", parentRole, role, context);
                return;
            }
            i18n.send(ctx, NEUTRAL, "{name#role} is already parent role of the role {role} in {context}!", parentRole, role, context);
        });


        // TODO i18n.sendTranslated(ctx, NEGATIVE, "Circular Dependency! {name#role} depends on the role {name}!", pr.getName(), r.getName());
    }

    @Alias(value = "remRParent")
    @Command(desc = "Removes a parent role from given role [in context]")
    public void removeParent(CommandSource ctx, FileSubject role, FileSubject parentRole, @Named("in") @Default Context context)
    {
        role.getSubjectData().removeParent(toSet(context), parentRole.asSubjectReference()).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, POSITIVE, "Removed the parent role {role} from the role {role} in {context}!", parentRole, role, context);
                return;
            }
            i18n.send(ctx, NEUTRAL, "{role} is not a parent role of the role {role} in {context}!", parentRole, role, context);
        });
    }

    @Alias(value = "clearRParent")
    @Command(desc = "Removes all parent roles from given role [in context]")
    public void clearParent(CommandSource ctx, FileSubject role, @Named("in") @Default Context context)
    {
        role.getSubjectData().clearParents(toSet(context)).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, NEUTRAL, "All parent roles of the role {role} in {context} cleared!", role, context);
                return;
            }
            i18n.send(ctx, NEUTRAL, "{role} had no parent roles in {context}!", role, context);
        });
    }

    @Alias(value = "setRolePriority")
    @Command(alias = "setPrio", desc = "Sets the priority of given role")
    public void setPriority(CommandSource ctx, FileSubject role, String priority)
    {
        try
        {
            ClassedConverter<Priority> converter = new PriorityConverter();
            Priority prio = converter.fromNode(new StringNode(priority), Priority.class, null);
            role.setPriorityValue(prio.value);
            i18n.send(ctx, POSITIVE, "Priority of the role {role} set to {input#priority}!", role, priority);
        }
        catch (ConversionException ex)
        {
            i18n.send(ctx, NEGATIVE, "{input#priority} is not a valid priority!", priority);
        }
    }

    @Alias(value = "renameRole")
    @Command(desc = "Renames given role")
    public void rename(CommandSource ctx, FileSubject role, @Label("new name") String newName)
    {
        String oldName = role.getIdentifier();
        if (oldName.equalsIgnoreCase(newName))
        {
            i18n.send(ctx, NEGATIVE, "These are the same names!");
            return;
        }
        if (service.getGroupSubjects().rename(role, newName))
        {
            i18n.send(ctx, POSITIVE, "The role {name#old} was renamed to {role}", oldName, role);
            return;
        }
        i18n.send(ctx, NEGATIVE, "Renaming failed! The role {name} already exists!", newName);
    }

    @Alias(value = "createRole")
    @Command(desc = "Creates a new role")
    public void create(CommandSource ctx, String name)
    {
        service.getGroupSubjects().hasSubject(name).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, NEUTRAL, "There is already a role named {name}.", name);
                return;
            }
            service.getGroupSubjects().loadSubject(name).thenAccept(s -> {
                ((FileSubjectData) s.getSubjectData()).save(CompletableFuture.completedFuture(true));
                i18n.send(ctx, POSITIVE, "Role {name} created!", name);
            });
        });
    }

    @Alias(value = "deleteRole")
    @Command(desc = "Deletes a role")
    public void delete(CommandSource ctx, FileSubject role, @Flag boolean force)
    {
        if (service.getGroupSubjects().delete(role, force))
        {
            i18n.send(ctx, POSITIVE, "Deleted the role {role}!", role);
            return;
        }
        i18n.send(ctx, NEGATIVE, "Role is still in use! Use the -force flag to delete the role and all occurrences");
    }

    @Command(alias = {"toggleDefault", "toggleDef"}, desc = "Toggles whether given role is a default role")
    public void toggleDefaultRole(CommandSource ctx, FileSubject role)
    {
        SubjectData defaultData = service.getUserSubjects().getDefaults().getSubjectData();
        if (defaultData.getParents(GLOBAL_CONTEXT).contains(role.asSubjectReference()))
        {
            defaultData.removeParent(GLOBAL_CONTEXT, role.asSubjectReference());
            i18n.send(ctx, POSITIVE, "{role} is no longer a default role!", role);
            return;
        }
        defaultData.addParent(GLOBAL_CONTEXT, role.asSubjectReference());
        i18n.send(ctx, POSITIVE, "{role} is now a default role!", role);
    }
}
