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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.RolesUtil.FoundOption;
import org.cubeengine.module.roles.RolesUtil.FoundPermission;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextFormat;

import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;
import static org.spongepowered.api.text.action.TextActions.showText;
import static org.spongepowered.api.text.format.TextColors.*;

@Command(name = "user", desc = "Manage users")
public class UserInformationCommands extends ContainerCommand
{
    private I18n i18n;
    private RolesPermissionService service;

    public UserInformationCommands(Roles module, I18n i18n, RolesPermissionService service)
    {
        super(module);
        this.i18n = i18n;
        this.service = service;
    }

    @Alias(value = "listuroles")
    @Command(desc = "Lists roles of a user [in context]")
    public void list(CommandSource ctx, @Default User player)
    {
        List<Subject> parents = player.getSubjectData().getParents(GLOBAL_CONTEXT);

        Text translation = i18n.getTranslation(ctx, NEUTRAL, "Roles of {user}:", player);
        if (ctx.hasPermission("cubeengine.roles.command.roles.user.assign"))
        {
            translation = translation.toBuilder().append(Text.of(" ")).append(
                i18n.getTranslation(ctx, POSITIVE, "(+)").toBuilder().onClick(TextActions.executeCallback(
                    sender -> {
                        i18n.sendTranslated(sender, POSITIVE, "Click on the role you want to add to {user}.", player);

                        for (Subject subject : service.getGroupSubjects().getAllSubjects())
                        {
                            // TODO perm check for each role
                            if (!parents.contains(subject) && subject instanceof RoleSubject)
                            {
                                sender.sendMessage(Text.of(YELLOW, " - ", ((RoleSubject)subject).getName()).toBuilder().onClick(
                                    TextActions.runCommand("/roles user assign " + player.getName() + " " + ((RoleSubject)subject).getName())).build());
                            }
                        }
                    })).onHover(showText(i18n.getTranslation(ctx, POSITIVE, "Click to add role"))).build()).build();
        }
        ctx.sendMessage(translation);

        if (ctx.hasPermission("cubeengine.roles.command.roles.user.remove"))
        {
            Text removeText1 = i18n.getTranslation(ctx, NEGATIVE, "Click to remove role.");
            parents.stream().filter(parent -> parent instanceof RoleSubject).map(RoleSubject.class::cast)
               .forEach(parent -> {
                    // TODO perm check for each role
                    Text removeText = Text.of(RED, "(-)").toBuilder().onClick(TextActions.executeCallback(sender -> {
                        i18n.sendTranslated(sender, NEGATIVE, "Do you really want to remove {role} from {user}?", parent, player);
                        ctx.sendMessage(i18n.getTranslation(sender, TextFormat.NONE, "Confirm").toBuilder().color(DARK_GREEN).onClick(
                            TextActions.runCommand("/roles user remove " + player.getName() + " " + parent.getName())).build());
                    })).onHover(showText(removeText1)).build();
                    ctx.sendMessage(Text.of("- ", GOLD, parent.getName(), " ", removeText));
                });
        }
        else
        {
            parents.stream().filter(parent -> parent instanceof RoleSubject).map(RoleSubject.class::cast)
                   .forEach(parent -> ctx.sendMessage(Text.of("- ", GOLD, parent.getName())));
        }
    }


    @Alias(value = "checkuperm")
    @Command(alias = "checkperm", desc = "Checks for permissions of a user [in context]")
    public void checkpermission(CommandSource ctx, @Default User player, @Complete(PermissionCompleter.class) String permission, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);

        Text permText = permText(ctx, permission);
        FoundPermission found = RolesUtil.findPermission(service, player, permission, contexts);

        if (found != null && found.value)
        {
            i18n.sendTranslated(ctx, POSITIVE, "The player {user} does have access to {txt#permission} in {context}", player, permText, context);
        }
        else
        {
            i18n.sendTranslated(ctx, NEGATIVE, "The player {user} does not have access to {txt#permission} in {context}", player, permText, context);
        }

        if (found != null)
        {
            if (found.subject == player)
            {
                i18n.sendTranslated(ctx, NEUTRAL, "Permission is directly assigned to the user!");
            }
            else
            {
                i18n.sendTranslated(ctx, NEUTRAL, "Permission inherited from:");
                i18n.sendTranslated(ctx, NEUTRAL, "{txt#permission} in the role {name}!", permText(ctx, found.permission), found.subject.getIdentifier().substring(5));
            }
        }
    }

    private Text permText(CommandSource cmdSource, String permission)
    {
        Text permText = Text.of(permission);
        Optional<PermissionDescription> permDesc = service.getDescription(permission);
        if (permDesc.isPresent())
        {
            permText = permText.toBuilder().onHover(showText(permDesc.get().getDescription().toBuilder().color(YELLOW).build())).build();
        }
        else
        {
            permText = permText.toBuilder().onHover(showText(i18n.getTranslation(cmdSource, NEGATIVE, "Permission not registered"))).build();
        }
        return permText;
    }

    @Alias(value = "listuperm")
    @Command(alias = "listperm", desc = "List permission assigned to a user [in context]")
    public void listpermission(CommandSource ctx, @Default User player, @Named("in") @Default Context context, @Flag boolean all)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        Map<String, Boolean> permissions = new HashMap<>();
        if (all)
        {
            RolesUtil.fillPermissions(player, contexts, permissions);
        }
        else
        {
            permissions.putAll(player.getSubjectData().getPermissions(contexts));
        }
        if (permissions.isEmpty())
        {
            if (all)
            {
                i18n.sendTranslated(ctx, NEUTRAL, "{user} has no permissions set in {context}.", player, context);
                return;
            }
            i18n.sendTranslated(ctx, NEUTRAL, "{user} has no permissions set directly in {context}.", player, context);
            return;
        }
        i18n.sendTranslated(ctx, NEUTRAL, "Permissions of {user} in {context}:", player, context);
        for (Map.Entry<String, Boolean> entry : permissions.entrySet())
        {
            ctx.sendMessage(Text.of("- ", YELLOW, permText(ctx, entry.getKey()), TextColors.WHITE, ": ", GOLD, entry.getValue()));
        }
    }

    @Alias(value = "checkumeta")
    @Command(alias = {"checkdata", "checkmeta"}, desc = "Checks for metadata of a user [in context]")
    public void checkmetadata(CommandSource ctx, @Default User player, String metadatakey, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);

        Optional<FoundOption> option = RolesUtil.getOption(service.getUserSubjects().get(player.getIdentifier()), metadatakey, contexts);
        if (!option.isPresent())
        {
            i18n.sendTranslated(ctx, NEUTRAL, "{input#key} is not set for {user} in {context}.", metadatakey, player, context);
            return;
        }

        i18n.sendTranslated(ctx, NEUTRAL, "{input#key}: {input#value} is set for {user} in {context}.", metadatakey, option.get().value, player, context);
        if (option.get().subject.getIdentifier().equals(player.getIdentifier()))
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Metadata is directly assigned to the user!");
        }
        else
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Metadata inherited from the role {name}!", option.get().subject.getIdentifier().substring(5));
        }
    }

    @Alias(value = "listumeta")
    @Command(alias = {"listdata", "listmeta"}, desc = "Lists assigned metadata from a user [in context]")
    public void listmetadata(CommandSource ctx, @Default User player, @Named("in") @Default Context context, @Flag boolean all)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        Map<String, String> options = new HashMap<>();
        if (all)
        {
            RolesUtil.fillOptions(service.getUserSubjects().get(player.getIdentifier()), contexts, options);
        }
        else
        {
            options.putAll(((OptionSubjectData)player.getSubjectData()).getOptions(contexts));
        }
        i18n.sendTranslated(ctx, NEUTRAL, "Metadata of {user} in {context}:", player, context);
        for (Map.Entry<String, String> entry : options.entrySet())
        {
            ctx.sendMessage(Text.of("- ", YELLOW, entry.getKey(), TextColors.WHITE, ": ", GOLD, entry.getValue()));
        }
    }
}
