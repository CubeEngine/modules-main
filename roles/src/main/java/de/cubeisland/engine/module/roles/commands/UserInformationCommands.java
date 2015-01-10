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

import java.util.Map;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.command.methodic.parametric.Named;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.permission.PermDefault;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.role.Role;
import de.cubeisland.engine.module.roles.role.RolesAttachment;
import de.cubeisland.engine.module.roles.role.TempDataStore;
import de.cubeisland.engine.module.roles.role.UserDatabaseStore;
import de.cubeisland.engine.module.roles.role.resolved.ResolvedMetadata;
import de.cubeisland.engine.module.roles.role.resolved.ResolvedPermission;
import org.bukkit.World;

import static de.cubeisland.engine.command.parameter.property.Requirement.OPTIONAL;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Command(name = "user", desc = "Manage users")
public class UserInformationCommands extends UserCommandHelper
{
    public UserInformationCommands(Roles module)
    {
        super(module);
    }

    @Alias(value = "listuroles")
    @Command(desc = "Lists roles of a user [in world]")
    public void list(CommandContext context, @Default User player, @Named("in") World world)
    {
        world = this.getWorld(context, world);
        if (world == null)
        {
            return;
        }
        RolesAttachment rolesAttachment = this.manager.getRolesAttachment(player);
        // List all assigned roles
        context.sendTranslated(NEUTRAL, "Roles of {user} in {world}:", player, world);
        for (Role pRole : rolesAttachment.getDataHolder(world).getRoles())
        {
            if (pRole.isGlobal())
            {
                context.sendMessage(String.format(this.LISTELEM_VALUE, "global", pRole.getName()));
                continue;
            }
            context.sendMessage(String.format(this.LISTELEM_VALUE, world.getName(), pRole.getName()));
        }
    }

    @Alias(value = "checkuperm")
    @Command(alias = "checkperm", desc = "Checks for permissions of a user [in world]")
    public void checkpermission(CommandContext context, @Default User player, String permission, @Named("in") World world)
    {
        world = this.getWorld(context, world);
        if (world == null)
        {
            return;
        }
        RolesAttachment rolesAttachment = this.manager.getRolesAttachment(player);
        // Search for permission
        ResolvedPermission resolvedPermission = rolesAttachment.getDataHolder(world).getPermissions().get(permission);
        if (player.isOp())
        {
            context.sendTranslated(POSITIVE, "{user} is op!", player);
        }
        if (player.isOnline()) // Can have superperm
        {
            boolean superPerm = player.hasPermission(permission);
            context.sendTranslated(NEUTRAL, "SuperPerm Node: {bool}", superPerm);
        }
        if (resolvedPermission == null)
        {

            PermDefault defaultFor = this.module.getCore().getPermissionManager().getDefaultFor(permission);
            if (defaultFor == null)
            {
                context.sendTranslated(NEGATIVE, "Permission {input} neither set nor registered!", permission);
            }
            else
            {
                context.sendTranslated(NEGATIVE, "Permission {input} not set but default is: {name#default}!", permission, defaultFor.name());
            }
            return;
        }
        if (resolvedPermission.isSet())
        {
            context.sendTranslated(POSITIVE, "The player {user} does have access to {input#permission} in {world}", player, permission, world);
        }
        else
        {
            context.sendTranslated(NEGATIVE, "The player {user} does not have access to {input#permission} in {world}", player, permission, world);
        }
        // Display origin
        TempDataStore store = resolvedPermission.getOrigin();
        if (resolvedPermission.getOriginPermission() != null) // indirect permission
        {
            permission = resolvedPermission.getOriginPermission();
        }
        context.sendTranslated(NEUTRAL, "Permission inherited from:");
        if (player.getName().equals(store.getName()))
        {
            context.sendTranslated(NEUTRAL, "{input#permission} directly assigned to the user!", permission);
            return;
        }
        context.sendTranslated(NEUTRAL, "{input#permission} in the role {name}!", permission, store.getName());
    }

    @Alias(value = "listuperm")
    @Command(alias = "listperm", desc = "List permission assigned to a user in a world")
    public void listpermission(CommandContext context, @Default User player, @Named("in") World world, @Flag boolean all)
    {
        world = this.getWorld(context, world);
        if (world == null)
        {
            return;
        }
        RolesAttachment rolesAttachment = this.manager.getRolesAttachment(player);
        UserDatabaseStore rawData = rolesAttachment.getDataHolder(world);
        Map<String, Boolean> perms = all ? rawData.getAllRawPermissions() : rawData.getRawPermissions();
        if (perms.isEmpty())
        {
            if (all)
            {
                context.sendTranslated(NEUTRAL, "{user} has no permissions set in {world}.", player, world);
                return;
            }
            context.sendTranslated(NEUTRAL, "{user} has no permissions set directly in {world}.", player, world);
            return;
        }
        context.sendTranslated(NEUTRAL, "Permissions of {user} in {world}.", player, world);
        for (Map.Entry<String, Boolean> entry : perms.entrySet())
        {
            context.sendMessage(String.format(this.LISTELEM_VALUE, entry.getKey(), entry.getValue()));
        }
    }

    @Alias(value = "checkumeta")
    @Command(alias = {"checkdata", "checkmeta"}, desc = "Checks for metadata of a user [in world]")
    public void checkmetadata(CommandContext context, @Default User player, String metadatakey, @Named("in") World world)
    {
        world = this.getWorld(context, world);
        if (world == null)
        {
            return;
        }
        RolesAttachment rolesAttachment = this.manager.getRolesAttachment(player);
        // Check metadata
        UserDatabaseStore dataHolder = rolesAttachment.getDataHolder(world);
        Map<String, ResolvedMetadata> metadata = dataHolder.getMetadata();
        if (!metadata.containsKey(metadatakey))
        {
            context.sendTranslated(NEUTRAL, "{input#key} is not set for {user} in {world}.", metadatakey, player, world);
            return;
        }
        context.sendTranslated(NEUTRAL, "{input#key}: {input#value} is set for {user} in {world}.", metadatakey, metadata.get(metadatakey).getValue(), player, world);
        if (metadata.get(metadatakey).getOrigin() != dataHolder)
        {
            context.sendTranslated(NEUTRAL, "Origin: {name#role}", metadata.get(metadatakey).getOrigin().getName());
            return;
        }
        context.sendTranslated(NEUTRAL, "Origin: {text:directly assigned}");
    }

    @Alias(value = "listumeta")
    @Command(alias = {"listdata", "listmeta"}, desc = "Lists assigned metadata from a user [in world]")
    public void listmetadata(CommandContext context, @Default User player, @Named("in") World world, @Flag boolean all)
    {
        world = this.getWorld(context, world);
        if (world == null)
        {
            return;
        }
        RolesAttachment rolesAttachment = this.manager.getRolesAttachment(player);
        UserDatabaseStore rawData = rolesAttachment.getDataHolder(world);
        Map<String, String> metadata = all ? rawData.getAllRawMetadata() : rawData.getRawMetadata();
        // List all metadata
        context.sendTranslated(NEUTRAL, "Metadata of {user} in {world}:", player, world);
        for (Map.Entry<String, String> entry : metadata.entrySet())
        {
            context.sendMessage(String.format(this.LISTELEM_VALUE, entry.getKey(), entry.getValue()));
        }
    }
}
