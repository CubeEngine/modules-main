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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.sponge.subject.RoleSubject;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import static org.cubeengine.service.i18n.formatter.MessageType.*;

@Command(name = "user", desc = "Manage users")
public class UserInformationCommands extends ContainerCommand
{
    private I18n i18n;

    public UserInformationCommands(Roles module, I18n i18n)
    {
        super(module);
        this.i18n = i18n;
    }

    @Alias(value = "listuroles")
    @Command(desc = "Lists roles of a user [in context]")
    public void list(CommandSource ctx, @Default User player, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        List<Subject> parents = player.getSubjectData().getParents(contexts);

        i18n.sendTranslated(ctx, NEUTRAL, "Roles of {user} in {context}:", player, context);
        parents.stream().filter(parent -> parent instanceof RoleSubject)
               .forEach(parent -> ctx.sendMessage(Text.of("- ", TextColors.YELLOW, context.getName().isEmpty() ? context.getType() : context.getName(),
                               TextColors.WHITE, ": ", TextColors.GOLD, ((RoleSubject)parent).getName())));
    }


    @Alias(value = "checkuperm")
    @Command(alias = "checkperm", desc = "Checks for permissions of a user [in context]")
    public void checkpermission(CommandSource ctx, @Default User player, @Complete(PermissionCompleter.class) String permission, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        Tristate value = player.getPermissionValue(contexts, permission);
        // TODO search registered permission
        if (value == Tristate.TRUE)
        {
            i18n.sendTranslated(ctx, POSITIVE, "The player {user} does have access to {input#permission} in {context}",
                                   player, permission, context);
        }
        else if (value == Tristate.FALSE)
        {
            i18n.sendTranslated(ctx, NEGATIVE, "The player {user} does not have access to {input#permission} in {context}",
                                   player, permission, context);
        }
        else
        {
            // i18n.sendTranslated(ctx, NEGATIVE, "Permission {input} neither set nor registered!", permission);
            // i18n.sendTranslated(ctx, NEGATIVE, "Permission {input} not set but default is: {name#default}!", permission, defaultFor.name());
            return;
        }
        // TODO find origin
        //i18n.sendTranslated(ctx, NEUTRAL, "Permission inherited from:");
        //i18n.sendTranslated(ctx, NEUTRAL, "{input#permission} directly assigned to the user!", permission);
        //i18n.sendTranslated(ctx, NEUTRAL, "{input#permission} in the role {name}!", permission, store.getName());
    }

    @Alias(value = "listuperm")
    @Command(alias = "listperm", desc = "List permission assigned to a user [in context]")
    public void listpermission(CommandSource ctx, @Default User player, @Named("in") @Default Context context, @Flag boolean all)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        Map<String, Boolean> permissions = player.getSubjectData().getPermissions(contexts);
        if (all)
        {
            // TODO recursive
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
            ctx.sendMessage(Text.of("- ", TextColors.YELLOW, entry.getKey(),
                    TextColors.WHITE, ": ", TextColors.GOLD, entry.getValue()));
        }
    }

    @Alias(value = "checkumeta")
    @Command(alias = {"checkdata", "checkmeta"}, desc = "Checks for metadata of a user [in context]")
    public void checkmetadata(CommandSource ctx, @Default User player, String metadatakey, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        String value = ((OptionSubjectData)player.getSubjectData()).getOptions(contexts).get(metadatakey);
        if (value == null)
        {
            i18n.sendTranslated(ctx, NEUTRAL, "{input#key} is not set for {user} in {context}.", metadatakey, player, context);
            return;
        }
        i18n.sendTranslated(ctx, NEUTRAL, "{input#key}: {input#value} is set for {user} in {context}.", metadatakey, value, player, context);
        // TODO find origin
        // i18n.sendTranslated(ctx, NEUTRAL, "Origin: {name#role}", metadata.get(metadatakey).getOrigin().getName());
        // i18n.sendTranslated(ctx, NEUTRAL, "Origin: {text:directly assigned}");
    }

    @Alias(value = "listumeta")
    @Command(alias = {"listdata", "listmeta"}, desc = "Lists assigned metadata from a user [in context]")
    public void listmetadata(CommandSource ctx, @Default User player, @Named("in") @Default Context context, @Flag boolean all)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        Map<String, String> options = ((OptionSubjectData)player.getSubjectData()).getOptions(contexts);
        if (all)
        {
            // TODO recursive
        }
        i18n.sendTranslated(ctx, NEUTRAL, "Metadata of {user} in {context}:", player, context);
        for (Map.Entry<String, String> entry : options.entrySet())
        {
            ctx.sendMessage(Text.of("- ", TextColors.YELLOW, entry.getKey(),
                    TextColors.WHITE, ": ", TextColors.GOLD, entry.getValue()));
        }
    }
}
