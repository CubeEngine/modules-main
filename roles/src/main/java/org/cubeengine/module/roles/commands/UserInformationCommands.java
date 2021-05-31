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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Parser;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.RolesUtil.FoundOption;
import org.cubeengine.module.roles.RolesUtil.FoundPermission;
import org.cubeengine.libcube.service.command.parser.ContextParser;
import org.cubeengine.module.roles.commands.provider.FileSubjectParser;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.cubeengine.libcube.util.ContextUtil.toSet;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

@Singleton
@Using({FileSubjectParser.class, ContextParser.class})
public class UserInformationCommands extends DispatcherCommand
{
    private I18n i18n;
    private RolesPermissionService service;

    @Inject
    public UserInformationCommands(I18n i18n, RolesPermissionService service)
    {
        super(Roles.class);
        this.i18n = i18n;
        this.service = service;
    }

    @Alias(value = "listurole")
    @Command(desc = "Lists roles of a user")
    public void list(CommandCause ctx, @Default User player)
    {
        List<? extends SubjectReference> parents = player.subjectData().parents(GLOBAL_CONTEXT);
        List<? extends SubjectReference> transientParents = player.transientSubjectData().parents(GLOBAL_CONTEXT);

        Component translation = i18n.translate(ctx, NEUTRAL, "Roles of {user}:", player);
        if (ctx.hasPermission("cubeengine.roles.command.roles.user.assign"))
        {
            translation = translation.append(Component.space()).append(
                i18n.translate(ctx, POSITIVE, "(+)").clickEvent(SpongeComponents.executeCallback(
                    sender -> {
                        i18n.send(sender, POSITIVE, "Click on the role you want to add to {user}.", player);

                        for (Subject subject : service.groupSubjects().loadedSubjects())
                        {
                            // TODO perm check for each role
                            if (!parents.contains(subject.asSubjectReference()) && subject instanceof FileSubject)
                            {
                                sender.sendMessage(Identity.nil(), Component.text(" - " + subject.identifier(), NamedTextColor.YELLOW).clickEvent(
                                    ClickEvent.runCommand("/roles user assign " + player.name() + " " + subject.identifier())));
                            }
                        }
                    })).hoverEvent(HoverEvent.showText(i18n.translate(ctx, POSITIVE, "Click to add role"))));
        }
        ctx.sendMessage(Identity.nil(), translation);

        if (ctx.hasPermission("cubeengine.roles.command.roles.user.remove"))
        {
            Component removeText1 = i18n.translate(ctx, NEGATIVE, "Click to remove role.");
            parents.stream().map(p -> {
                try{
                    return p.resolve().get();
                }
                catch (ExecutionException | InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }).forEach(parent -> {
                // TODO perm check for each role
                final Component removeText = Component.text("(-)", NamedTextColor.RED).clickEvent(SpongeComponents.executeCallback(sender -> {
                    i18n.send(sender, NEGATIVE, "Do you really want to remove {role} from {user}?", parent, player);
                    ctx.sendMessage(Identity.nil(), i18n.translate(sender, Style.style(NamedTextColor.DARK_GREEN), "Confirm").clickEvent(ClickEvent.runCommand("/roles user remove " + player.name() + " " + parent.identifier())));
                })).hoverEvent(HoverEvent.showText(removeText1));

                ctx.sendMessage(Identity.nil(), Component.text().append(Component.text("- ")).append(Component.text(parent.identifier(), NamedTextColor.GOLD)).append(Component.space().append(removeText)).build());
            });
        }
        else
        {

            parents.forEach(parent -> ctx.sendMessage(Identity.nil(), Component.text().append(Component.text("- ")).append(Component.text(parent.subjectIdentifier(), NamedTextColor.GOLD)).build()));
        }
        String transientText = i18n.getTranslation(ctx, "transient");
        final Component transientComponent = i18n.composeMessage(ctx, Style.style(NamedTextColor.GRAY), " ({name:color=YELLOW})", transientText);
        transientParents.forEach(parent -> ctx.sendMessage(Identity.nil(),
                   Component.text().append(Component.text("- ")).append(Component.text(parent.subjectIdentifier(), NamedTextColor.GOLD)).append(transientComponent).build()));
    }


    @Alias(value = "checkuperm")
    @Command(alias = "checkperm", desc = "Checks for permissions of a user [in context]")
    public void checkPermission(CommandCause ctx, @Default User player, @Parser(completer = PermissionCompleter.class) String permission, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);

        Component permText = RolesUtil.permText(ctx, permission, service, i18n);
        FoundPermission found = RolesUtil.findPermission(service, player, permission, contexts);
        FoundPermission foundNow = RolesUtil.findPermission(service, player, permission, service.contextsFor(player.contextCause()));

        i18n.send(ctx, NEUTRAL, "Player {user} permission check {txt#permission}", player, permText);
        if (found != null)
        {
            Component from = getFromText(ctx, player, found);
            if (found.value)
            {
                i18n.send(ctx, POSITIVE, "Set to {text:true:color=DARK_GREEN} in {context} {txt#info}", context, from);
            }
            else
            {
                i18n.send(ctx, NEGATIVE, "Set to {text:false:color=DARK_RED} in {context} {txt#info}", context, from);
            }
        }
        else
        {
            i18n.send(ctx, NEGATIVE, "Not set in {context}", context);
        }
        if (foundNow != null)
        {
            Component from = getFromText(ctx, player, foundNow);
            if (foundNow.value)
            {
                i18n.send(ctx, POSITIVE, "Set to {text:true:color=DARK_GREEN} in their active contexts {txt#info}", from);
            }
            else
            {
                i18n.send(ctx, NEGATIVE, "Set to {text:false:color=DARK_RED} in their active contexts {txt#info}", from);
            }
        }
        else
        {
            i18n.send(ctx, NEGATIVE, "Not set in {context}", context);
        }
    }

    private Component getFromText(CommandCause ctx, @Default User player, FoundPermission found)
    {
        Component from;
        if (found.subject == player)
        {
            from = i18n.translate(ctx, NEUTRAL, "Permission is directly assigned to the user!");
        }
        else
        {
            from = Component.text().append(i18n.translate(ctx, NEUTRAL, "Permission inherited from:")).append(Component.newline()).append(
                    RolesUtil.permText(ctx, found.permission, service, i18n).color(NamedTextColor.GOLD)).append(Component.newline()).append(
                           i18n.translate(ctx, NEUTRAL, "in the role {name}!", found.subject.identifier())).build();
        }
        from = Component.text("(?)").hoverEvent(HoverEvent.showText(from));
        return from;
    }

    @Alias(value = "listuperm")
    @Command(alias = "listperm", desc = "List permission assigned to a user [in context]")
    public void listPermission(CommandCause ctx, @Default User player, @Named("in") @Default Context context, @Flag boolean all)
    {
        Set<Context> contexts = toSet(context);
        Map<String, Boolean> permissions = new HashMap<>();
        if (all)
        {
            RolesUtil.fillPermissions(player, contexts, permissions, service);
        }
        else
        {
            permissions.putAll(player.subjectData().permissions(contexts));
        }
        if (permissions.isEmpty())
        {
            if (all)
            {
                i18n.send(ctx, NEUTRAL, "{user} has no permissions set in {context}.", player, context);
                return;
            }
            i18n.send(ctx, NEUTRAL, "{user} has no permissions set directly in {context}.", player, context);
            return;
        }
        i18n.send(ctx, NEUTRAL, "Permissions of {user} in {context}:", player, context);
        for (Map.Entry<String, Boolean> entry : permissions.entrySet())
        {
            ctx.sendMessage(Identity.nil(), Component.text().append(Component.text("- ")).append(RolesUtil.permText(ctx, entry.getKey(), service, i18n).color(NamedTextColor.YELLOW))
                                                     .append(Component.text(": ", NamedTextColor.WHITE)).append(Component.text(entry.getValue(), NamedTextColor.GOLD)).build());
            ;
        }
    }

    @Alias(value = "checkUOption", alias = "checkUData")
    @Command(alias = {"checkData"}, desc = "Checks for options of a user [in context]")
    public void checkOption(CommandCause ctx, @Default User player, String key, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);

        Optional<FoundOption> option = RolesUtil.getOption(service, player, key, contexts, true);
        if (!option.isPresent())
        {
            i18n.send(ctx, NEUTRAL, "{input#key} is not set for {user} in {context}.", key, player, context);
            return;
        }

        i18n.send(ctx, NEUTRAL, "{input#key}: {input#value} is set for {user} in {context}.", key, option.get().value, player, context);
        if (option.get().subject.identifier().equals(player.identifier()))
        {
            i18n.send(ctx, NEUTRAL, "Options is directly assigned to the user!");
        }
        else
        {
            i18n.send(ctx, NEUTRAL, "Options inherited from the role {name}!", option.get().subject.identifier());
        }
    }

    @Alias(value = "listUOption", alias = "listUData")
    @Command(alias = "listData", desc = "Lists assigned options from a user [in context]")
    public void listOption(CommandCause ctx, @Default User player, @Named("in") @Default Context context, @Flag boolean all)
    {
        Set<Context> contexts = toSet(context);
        if (!all)
        {
            Map<String, String> options = (player.subjectData()).options(contexts);
            i18n.send(ctx, NEUTRAL, "Options of {user} directly set in {context}:", player, context);
            for (Map.Entry<String, String> entry : options.entrySet())
            {
                ctx.sendMessage(Identity.nil(), i18n.composeMessage(ctx, Style.empty(),"- {name:color=YELLOW} {text:\\::color=WHITE} {name:color=GOLD}", entry.getKey(), entry.getValue()));
            }
            return;
        }
        try
        {
            Subject subject = service.userSubjects().loadSubject(player.identifier()).get();
            Map<String, FoundOption> options = new HashMap<>();
            RolesUtil.fillOptions(subject, contexts, options, service);
            i18n.send(ctx, NEUTRAL, "Options of {user} in {context}:", player, context);
            for (Map.Entry<String, FoundOption> entry : options.entrySet())
            {
                Subject owner = entry.getValue().subject;
                final Component hoverText = Component.text().append(Component.text(owner.containingCollection().identifier(), NamedTextColor.YELLOW))
                         .append(Component.text(":", NamedTextColor.GRAY))
                         .append(Component.text(owner.friendlyIdentifier().orElse(owner.identifier()), NamedTextColor.YELLOW))
                         .build();
                final TextComponent key = Component.text(entry.getKey(), NamedTextColor.YELLOW).hoverEvent(HoverEvent.showText(hoverText));
                ctx.sendMessage(Identity.nil(), i18n.composeMessage(ctx, Style.empty(), "- {txt}{text:\\::color=WHITE} {name:color=GOLD}", key, entry.getValue().value));
            }
        }
        catch (InterruptedException | ExecutionException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
