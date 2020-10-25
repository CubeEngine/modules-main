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

import static java.util.Collections.emptySet;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.libcube.util.ContextUtil.toSet;

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Alias("manuser")
@Command(name = "user", desc = "Manage users")
public class UserManagementCommands extends ContainerCommand
{
    private RolesPermissionService service;
    private I18n i18n;

    public UserManagementCommands(CommandManager base, RolesPermissionService service, I18n i18n)
    {
        super(base, Roles.class);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias({"manUAdd", "assignURole", "addURole", "giveURole"})
    @Command(alias = {"add", "give"}, desc = "Assign a role to the player [-temp]")
    public void assign(CommandSource ctx, @Default User player, FileSubject role, @Flag boolean temp)
    {

        if (!role.canAssignAndRemove(ctx))
        {
            i18n.send(ctx, NEGATIVE, "You are not allowed to assign the role {role}!", role);
            return;
        }
        if (temp)
        {
            if (!player.getPlayer().isPresent())
            {
                i18n.send(ctx, NEGATIVE, "You cannot assign a temporary role to a offline player!");
                return;
            }
            player.getTransientSubjectData().addParent(emptySet(), role.asSubjectReference()).thenAccept(b -> {
                if (b)
                {
                    i18n.send(ctx, POSITIVE, "Added the role {role} temporarily to {user}.", role, player);
                    return;
                }
                i18n.send(ctx, NEUTRAL, "{user} already had the temporary role {role}.", player, role);
            });
            return;
        }
        player.getSubjectData().addParent(emptySet(), role.asSubjectReference()).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, POSITIVE, "Added the role {role} to {user}.", role, player);
                return;
            }
            i18n.send(ctx, NEUTRAL, "{user} already has the role {role}.", player, role);
        });
    }

    @Alias(value = {"remURole", "manUDel"})
    @Command(desc = "Removes a role from the player")
    public void remove(CommandSource ctx, @Default User player, FileSubject role)
    {
        if (!role.canAssignAndRemove(ctx))
        {
            i18n.send(ctx, NEGATIVE, "You are not allowed to remove the role {role}!", role);
            return;
        }

        CompletableFuture<Boolean> tData = player.getTransientSubjectData().removeParent(emptySet(), role.asSubjectReference());
        CompletableFuture<Boolean> pData = player.getSubjectData().removeParent(emptySet(), role.asSubjectReference());
        tData.thenCombine(pData, (b1, b2) -> b1 || b2).thenAccept(r -> {
            if (r) i18n.send(ctx, POSITIVE, "Removed the role {role} from {user}.", role, player);
            else   i18n.send(ctx, NEUTRAL, "{user} did not have the role {role}.", player, role);
        });
    }

    @Alias(value = {"clearURole", "manUClear"})
    @Command(desc = "Clears all roles from the player and sets the defaultroles [in context]")
    public void clear(CommandSource ctx, @Default User player)
    {
        player.getSubjectData().clearParents(emptySet());
        i18n.send(ctx, NEUTRAL, "Cleared the roles of {user}.", player);
        SubjectData defaultData = service.getDefaults().getSubjectData();
        if (defaultData.getParents(emptySet()).isEmpty())
        {
            i18n.send(ctx, NEUTRAL, "Default roles assigned:");
            for (SubjectReference subject : defaultData.getParents(emptySet()))
            {
                player.getTransientSubjectData().addParent(emptySet(), subject);
                ctx.sendMessage(Text.of("- ", TextColors.YELLOW, subject.getSubjectIdentifier()));
            }
        }
    }

    @Alias(value = "setUPerm")
    @Command(alias = "setPerm", desc = "Sets a permission for this user [in context]")
    public void setPermission(CommandSource ctx, @Default User player, @Complete(PermissionCompleter.class) String permission, @Default Tristate type, @Named("in") @Default Context context)
    {
        if (type == Tristate.UNDEFINED)
        {
            resetPermission(ctx, player, permission, context);
            return;
        }
        Set<Context> contexts = toSet(context);
        player.getSubjectData().setPermission(contexts, permission, type).thenAccept(b -> {
            if (!b)
            {
                i18n.send(ctx, NEGATIVE, "Permission {input} of {user} was already set to {bool} in {context}!", permission, player, type.asBoolean(), context);
                return;
            }
            switch (type)
            {
                case TRUE:
                case FALSE:
                    i18n.send(ctx, POSITIVE, "Permission {input} of {user} set to {bool} in {context}!", permission, player, type.asBoolean(), context);
            }
        });
    }

    @Alias(value = "resetUPerm")
    @Command(alias = "resetPerm", desc = "Resets a permission for this user [in context]")
    public void resetPermission(CommandSource ctx, @Default User player, String permission, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        player.getSubjectData().setPermission(contexts, permission, Tristate.UNDEFINED).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, NEUTRAL, "Permission {input} of {user} reset in {context}!", permission, player, context);
                return;
            }
            i18n.send(ctx, NEGATIVE, "Permission {input} of {user} was not set in {context}!", permission, player, context);
        });
    }

    @Alias(value = {"setUOption","setUData"})
    @Command(alias = "setData", desc = "Sets options for this user [in context]")
    public void setOption(CommandSource ctx, @Default User player, String key, String value, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        player.getSubjectData().setOption(contexts, key, value).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, POSITIVE, "Options {input#key} of {user} set to {input#value} in {context}!", key, player, value, context);
                return;
            }
            i18n.send(ctx, NEGATIVE, "Options {input#key} of {user} was already set to {input#value} in {context}!", key, player, value, context);
        });
    }

    @Alias(value = {"resetUOption","resetUData"})
    @Command(alias = {"resetData", "deleteOption", "deleteData"}, desc = "Resets options for this user [in context]")
    public void resetOption(CommandSource ctx, @Default User player, String key, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        player.getSubjectData().setOption(contexts, key, null).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, NEUTRAL, "Options {input#key} of {user} removed in {context}!", key, player, context);
                return;
            }
            i18n.send(ctx, NEGATIVE, "Options {input#key} was not set for {user} in {context}!", key, player, context);
        });
    }

    @Alias(value = {"clearUOption", "clearUData"})
    @Command(alias = "clearData", desc = "Resets options for this user [in context]")
    public void clearOption(CommandSource ctx, @Default User player, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        player.getSubjectData().clearOptions(contexts).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, NEUTRAL, "Options of {user} cleared in {context}!", player, context);
                return;
            }
            i18n.send(ctx, NEGATIVE, "Options of {user} was already cleared in {context}!", player, context);
        });
    }

    // TODO clone command

}
