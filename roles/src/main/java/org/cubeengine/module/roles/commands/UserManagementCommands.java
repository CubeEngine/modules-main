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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.format.Style;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.ExceptionHandler;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Parser;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.command.parser.ContextParser;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.commands.provider.FileSubjectParser;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.commands.provider.TristateParser;
import org.cubeengine.module.roles.exception.CircularRoleDependencyExceptionHandler;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.emptySet;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.cubeengine.libcube.util.ContextUtil.toSet;

@Singleton
@Alias("manuser")
@Command(name = "user", desc = "Manage users")
@Using({FileSubjectParser.class, TristateParser.class, ContextParser.class})
public class UserManagementCommands extends DispatcherCommand
{
    private RolesPermissionService service;
    private I18n i18n;

    @Inject
    public UserManagementCommands(RolesPermissionService service, I18n i18n, UserInformationCommands userInformationCommands)
    {
        super(Roles.class, userInformationCommands);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias(value = "manUAdd", alias = {"assignURole", "addURole", "giveURole"})
    @Command(alias = {"add", "give"}, desc = "Assign a role to the player [-temp]")
    @ExceptionHandler(CircularRoleDependencyExceptionHandler.class)
    public void assign(CommandCause ctx, User player, FileSubject role, @Flag boolean temp)
    {

        if (!role.canAssignAndRemove(ctx))
        {
            i18n.send(ctx, NEGATIVE, "You are not allowed to assign the role {role}!", role);
            return;
        }
        if (temp)
        {
            if (!player.player().isPresent())
            {
                i18n.send(ctx, NEGATIVE, "You cannot assign a temporary role to a offline player!");
                return;
            }
            player.transientSubjectData().addParent(emptySet(), role.asSubjectReference()).thenAccept(b -> {
                if (b)
                {
                    i18n.send(ctx, POSITIVE, "Added the role {role} temporarily to {user}.", role, player);
                    return;
                }
                i18n.send(ctx, NEUTRAL, "{user} already had the temporary role {role}.", player, role);
            });
            return;
        }
        player.subjectData().addParent(emptySet(), role.asSubjectReference()).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, POSITIVE, "Added the role {role} to {user}.", role, player);
                return;
            }
            i18n.send(ctx, NEUTRAL, "{user} already has the role {role}.", player, role);
        });
    }

    @Alias(value = "remURole", alias = "manUDel")
    @Command(desc = "Removes a role from the player")
    public void remove(CommandCause ctx, User player, FileSubject role)
    {
        if (!role.canAssignAndRemove(ctx))
        {
            i18n.send(ctx, NEGATIVE, "You are not allowed to remove the role {role}!", role);
            return;
        }

        CompletableFuture<Boolean> tData = player.transientSubjectData().removeParent(emptySet(), role.asSubjectReference());
        CompletableFuture<Boolean> pData = player.subjectData().removeParent(emptySet(), role.asSubjectReference());
        tData.thenCombine(pData, (b1, b2) -> b1 || b2).thenAccept(r -> {
            if (r) i18n.send(ctx, POSITIVE, "Removed the role {role} from {user}.", role, player);
            else   i18n.send(ctx, NEUTRAL, "{user} did not have the role {role}.", player, role);
        });
    }

    @Alias(value = "clearURole", alias = "manUClear")
    @Command(desc = "Clears all roles from the player and sets the defaultroles [in context]")
    @ExceptionHandler(CircularRoleDependencyExceptionHandler.class)
    public void clear(CommandCause ctx, User player)
    {
        player.subjectData().clearParents(emptySet());
        i18n.send(ctx, NEUTRAL, "Cleared the roles of {user}.", player);
        SubjectData defaultData = service.defaults().subjectData();
        if (defaultData.parents(emptySet()).isEmpty())
        {
            i18n.send(ctx, NEUTRAL, "Default roles assigned:");
            for (SubjectReference subject : defaultData.parents(emptySet()))
            {
                player.transientSubjectData().addParent(emptySet(), subject);
                ctx.sendMessage(Identity.nil(), i18n.composeMessage(ctx, Style.empty(), "- {name:color=YELLOW}", subject.subjectIdentifier()));
            }
        }
    }

    @Alias(value = "setUPerm")
    @Command(alias = "setPerm", desc = "Sets a permission for this user [in context]")
    public void setPermission(CommandCause ctx, User player, @Parser(completer = PermissionCompleter.class) String permission, @Default Tristate type, @Named("in") @Default Context context)
    {
        if (type == Tristate.UNDEFINED)
        {
            resetPermission(ctx, player, permission, context);
            return;
        }
        Set<Context> contexts = toSet(context);
        player.subjectData().setPermission(contexts, permission, type).thenAccept(b -> {
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
    public void resetPermission(CommandCause ctx, @Default User player, String permission, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        player.subjectData().setPermission(contexts, permission, Tristate.UNDEFINED).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, NEUTRAL, "Permission {input} of {user} reset in {context}!", permission, player, context);
                return;
            }
            i18n.send(ctx, NEGATIVE, "Permission {input} of {user} was not set in {context}!", permission, player, context);
        });
    }

    @Alias(value = "setUOption", alias = "setUData")
    @Command(alias = "setData", desc = "Sets options for this user [in context]")
    public void setOption(CommandCause ctx, User player, String key, String value, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        player.subjectData().setOption(contexts, key, value).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, POSITIVE, "Options {input#key} of {user} set to {input#value} in {context}!", key, player, value, context);
                return;
            }
            i18n.send(ctx, NEGATIVE, "Options {input#key} of {user} was already set to {input#value} in {context}!", key, player, value, context);
        });
    }

    @Alias(value = "resetUOption", alias = "resetUData")
    @Command(alias = {"resetData", "deleteOption", "deleteData"}, desc = "Resets options for this user [in context]")
    public void resetOption(CommandCause ctx, User player, String key, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        player.subjectData().setOption(contexts, key, null).thenAccept(b -> {
            if (b)
            {
                i18n.send(ctx, NEUTRAL, "Options {input#key} of {user} removed in {context}!", key, player, context);
                return;
            }
            i18n.send(ctx, NEGATIVE, "Options {input#key} was not set for {user} in {context}!", key, player, context);
        });
    }

    @Alias(value = "clearUOption", alias = "clearUData")
    @Command(alias = "clearData", desc = "Resets options for this user [in context]")
    public void clearOption(CommandCause ctx, User player, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        player.subjectData().clearOptions(contexts).thenAccept(b -> {
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
