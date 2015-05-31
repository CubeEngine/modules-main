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

import java.util.List;
import java.util.Map;
import java.util.Set;
import de.cubeisland.engine.butler.alias.Alias;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.sponge.subject.RoleSubject;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.ContainerCommand;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.roles.commands.RoleCommands.LISTELEM_VALUE;

@Command(name = "user", desc = "Manage users")
public class UserInformationCommands extends ContainerCommand
{
    public UserInformationCommands(Roles module)
    {
        super(module);
    }

    @Alias(value = "listuroles")
    @Command(desc = "Lists roles of a user [in context]")
    public void list(CommandContext cContext, @Default User player, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        List<Subject> parents = player.getPlayer().get().getSubjectData().getParents(contexts);

        cContext.sendTranslated(NEUTRAL, "Roles of {user} in {context}:", player, context);
        for (Subject parent : parents)
        {
            if (parent instanceof RoleSubject)
            {
                cContext.sendMessage(String.format(LISTELEM_VALUE, context.getName().isEmpty() ? context.getType() : context.getName(), ((RoleSubject)parent).getName()));
            }
        }
    }

    @Alias(value = "checkuperm")
    @Command(alias = "checkperm", desc = "Checks for permissions of a user [in context]")
    public void checkpermission(CommandContext cContext, @Default User player, String permission, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        Tristate value = player.getPlayer().get().getPermissionValue(contexts, permission);
        // TODO search registered permission
        if (value == Tristate.TRUE)
        {
            cContext.sendTranslated(POSITIVE, "The player {user} does have access to {input#permission} in {world}",
                                   player, permission, context);
        }
        else if (value == Tristate.FALSE)
        {
            cContext.sendTranslated(NEGATIVE, "The player {user} does not have access to {input#permission} in {world}",
                                   player, permission, context);
        }
        else
        {
            // cContext.sendTranslated(NEGATIVE, "Permission {input} neither set nor registered!", permission);
            // cContext.sendTranslated(NEGATIVE, "Permission {input} not set but default is: {name#default}!", permission, defaultFor.name());
            return;
        }
        // TODO find origin
        //cContext.sendTranslated(NEUTRAL, "Permission inherited from:");
        //cContext.sendTranslated(NEUTRAL, "{input#permission} directly assigned to the user!", permission);
        //cContext.sendTranslated(NEUTRAL, "{input#permission} in the role {name}!", permission, store.getName());
    }

    @Alias(value = "listuperm")
    @Command(alias = "listperm", desc = "List permission assigned to a user [in context]")
    public void listpermission(CommandContext cContext, @Default User player, @Named("in") @Default Context context, @Flag boolean all)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        Map<String, Boolean> permissions = player.getPlayer().get().getSubjectData().getPermissions(contexts);
        if (all)
        {
            // TODO recursive
        }
        if (permissions.isEmpty())
        {
            if (all)
            {
                cContext.sendTranslated(NEUTRAL, "{user} has no permissions set in {context}.", player, context);
                return;
            }
            cContext.sendTranslated(NEUTRAL, "{user} has no permissions set directly in {context}.", player, context);
            return;
        }
        cContext.sendTranslated(NEUTRAL, "Permissions of {user} in {context}:", player, context);
        for (Map.Entry<String, Boolean> entry : permissions.entrySet())
        {
            cContext.sendMessage(String.format(LISTELEM_VALUE, entry.getKey(), entry.getValue()));
        }
    }

    @Alias(value = "checkumeta")
    @Command(alias = {"checkdata", "checkmeta"}, desc = "Checks for metadata of a user [in context]")
    public void checkmetadata(CommandContext cContext, @Default User player, String metadatakey, @Named("in") @Default Context context)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        String value = ((OptionSubjectData)player.getPlayer().get().getSubjectData()).getOptions(contexts).get(metadatakey);
        if (value == null)
        {
            cContext.sendTranslated(NEUTRAL, "{input#key} is not set for {user} in {context}.", metadatakey, player, context);
            return;
        }
        cContext.sendTranslated(NEUTRAL, "{input#key}: {input#value} is set for {user} in {world}.", metadatakey, value, player, context);
        // TODO find origin
        // cContext.sendTranslated(NEUTRAL, "Origin: {name#role}", metadata.get(metadatakey).getOrigin().getName());
        // cContext.sendTranslated(NEUTRAL, "Origin: {text:directly assigned}");
    }

    @Alias(value = "listumeta")
    @Command(alias = {"listdata", "listmeta"}, desc = "Lists assigned metadata from a user [in context]")
    public void listmetadata(CommandContext cContext, @Default User player, @Named("in") @Default Context context, @Flag boolean all)
    {
        Set<Context> contexts = RoleCommands.toSet(context);
        Map<String, String> options = ((OptionSubjectData)player.getPlayer().get().getSubjectData()).getOptions(contexts);
        if (all)
        {
            // TODO recursive
        }
        cContext.sendTranslated(NEUTRAL, "Metadata of {user} in {world}:", player, context);
        for (Map.Entry<String, String> entry : options.entrySet())
        {
            cContext.sendMessage(String.format(LISTELEM_VALUE, entry.getKey(), entry.getValue()));
        }
    }
}
