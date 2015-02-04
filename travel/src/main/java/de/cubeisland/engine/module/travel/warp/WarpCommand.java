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
package de.cubeisland.engine.module.travel.warp;

import java.util.Set;
import de.cubeisland.engine.command.CommandInvocation;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.parametric.Command;
import de.cubeisland.engine.command.parametric.Flag;
import de.cubeisland.engine.command.parametric.Default;
import de.cubeisland.engine.command.parametric.Greed;
import de.cubeisland.engine.command.parametric.Label;
import de.cubeisland.engine.command.parametric.Named;
import de.cubeisland.engine.command.parametric.Optional;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.command.exception.PermissionDeniedException;
import de.cubeisland.engine.core.command.result.confirm.ConfirmResult;
import de.cubeisland.engine.core.command.sender.ConsoleCommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.travel.TpPointCommand;
import de.cubeisland.engine.module.travel.Travel;
import de.cubeisland.engine.module.travel.storage.TeleportInvite;
import org.bukkit.Location;

import static de.cubeisland.engine.command.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.core.util.ChatFormat.DARK_GREEN;
import static de.cubeisland.engine.core.util.ChatFormat.YELLOW;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.travel.storage.TableInvite.TABLE_INVITE;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PRIVATE;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PUBLIC;
import static java.util.stream.Collectors.toSet;
import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND;

@Command(name = "warp", desc = "Teleport to a warp")
public class WarpCommand extends TpPointCommand
{
    private final Travel module;
    private final WarpManager manager;

    public WarpCommand(Travel module)
    {
        super(module);
        this.module = module;
        this.manager = module.getWarpManager();
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof User)
        {
            return getCommand("tp").execute(invocation);
        }
        return super.selfExecute(invocation);
    }

    @Restricted(User.class)
    @Command(desc = "Teleport to a warp")
    public void tp(User sender, String warp, @Default User owner)
    {
        Warp w = manager.findOne(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (!w.canAccess(sender) && !module.getPermissions().WARP_TP_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_TP_OTHER);
        }
        Location location = w.getLocation();
        if (location == null)
        {
            warpInDeletedWorldMessage(sender, w);
            return;
        }
        if (!sender.teleport(location, COMMAND))
        {
            sender.sendTranslated(CRITICAL, "The teleportation got aborted!");
            return;
        }
        if (w.getWelcomeMsg() != null)
        {
            sender.sendMessage(w.getWelcomeMsg());
            return;
        }
        if (w.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "You have been teleported to your warp {name}!", w.getName());
            return;
        }
        sender.sendTranslated(POSITIVE, "You have been teleported to the warp {name} of {user}!", w.getName(),
                               w.getOwnerName());
    }

    @Restricted(User.class)
    @Alias(value = {"createwarp", "mkwarp", "makewarp"})
    @Command(alias = "make", desc = "Create a warp")
    public void create(User sender, String name, @Flag(name = "priv", longName = "private") boolean priv) // TODO flag permission "private"
    {
        if (this.manager.getCount() >= this.module.getConfig().warps.max)
        {
            sender.sendTranslated(CRITICAL, "The server have reached its maximum number of warps!");
            sender.sendTranslated(NEGATIVE, "Some warps must be deleted for new ones to be made");
            return;
        }
        if (manager.has(sender, name))
        {
            sender.sendTranslated(NEGATIVE, "A warp by that name already exist!");
            return;
        }
        if (name.contains(":") || name.length() >= 32)
        {
            sender.sendTranslated(NEGATIVE,
                                   "Warps may not have names that are longer than 32 characters nor contain colon(:)'s!");
            return;
        }
        if (this.manager.has(sender, name))
        {
            sender.sendTranslated(NEGATIVE, "The warp already exists! You can move it with {text:/warp move}");
            return;
        }
        Warp warp = manager.create(sender, name, sender.getLocation(), !priv);
        sender.sendTranslated(POSITIVE, "Your warp {name} has been created!", warp.getName());
    }

    @Command(desc = "Set the welcome message of warps", alias = {"setgreeting", "setwelcome", "setwelcomemsg"})
    public void greeting(CommandSender sender, String warp,
                         @Label("welcome message") @Greed(INFINITE) @Optional String message,
                         @Default @Named("owner") User owner,
                         @Flag boolean append)
    {
        // TODO permission other
        Warp w = this.manager.getExact(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (append)
        {
            w.setWelcomeMsg(w.getWelcomeMsg() + message);
        }
        else
        {
            w.setWelcomeMsg(message);
        }
        w.update();
        if (w.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "The welcome message for your warp {name} is now set to:", w.getName());
        }
        else
        {
            sender.sendTranslated(POSITIVE, "The welcome message for the warp {name} of {user} is now set to:",
                                   w.getName(), owner);
        }
        sender.sendMessage(w.getWelcomeMsg());
    }

    @Restricted(User.class)
    @Command(desc = "Move a warp")
    public void move(User sender, String warp, @Default User owner)
    {
        Warp w = manager.getExact(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (!w.isOwnedBy(sender) && !module.getPermissions().WARP_MOVE_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_MOVE_OTHER);
        }
        w.setLocation(sender.getLocation());
        w.update();
        if (w.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "Your warp {name} has been moved to your current location!", w.getName());
            return;
        }
        sender.sendTranslated(POSITIVE, "The warp {name} of {user} has been moved to your current location",
                               w.getName(), owner);
    }

    @Alias(value = {"removewarp", "deletewarp", "delwarp", "remwarp"})
    @Command(alias = "delete", desc = "Remove a warp")
    public void remove(CommandSender sender, String warp, @Default User owner)
    {
        Warp w = manager.getExact(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (!w.isOwnedBy(sender) && !module.getPermissions().WARP_REMOVE_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_REMOVE_OTHER);
        }
        manager.delete(w);
        if (w.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "Your warp {name} has been removed", warp);
            return;
        }
        sender.sendTranslated(POSITIVE, "The warp {name} of {user} has been removed", warp, owner);
    }

    @Command(desc = "Rename a warp")
    public void rename(CommandSender sender, String warp, @Label("new name") String newName, @Default @Named("owner") User owner)
    {
        Warp w = manager.getExact(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (!w.isOwnedBy(sender) && !module.getPermissions().WARP_RENAME_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_RENAME_OTHER);
        }
        if (warp.contains(":") || warp.length() >= 32)
        {
            sender.sendTranslated(NEGATIVE,
                                   "Warps may not have names that are longer than 32 characters or contain colon(:)'s!");
            return;
        }
        if (manager.rename(w, newName))
        {
            if (w.isOwnedBy(sender))
            {
                sender.sendTranslated(POSITIVE, "Your warp {name} has been renamed to {name}", w.getName(), newName);
                return;
            }
            sender.sendTranslated(POSITIVE, "The warp {name} of {user} has been renamed to {name}", w.getName(), owner,
                                   newName);
            return;
        }
        sender.sendTranslated(POSITIVE, "Could not rename the warp to {name}", newName);
    }

    @Command(desc = "List warps of a player")
    public void list(CommandContext context, @Default User owner,
                     @Flag(name = "pub", longName = "public") boolean pub,
                     @Flag boolean owned, @Flag boolean invited)
    {
        if (!owner.equals(context.getSource()))
        {
            context.ensurePermission(module.getPermissions().WARP_LIST_OTHER);
        }
        Set<Warp> warps = this.manager.list(owner, owned, pub, invited);
        if (warps.isEmpty())
        {
            context.sendTranslated(NEGATIVE, "No warps are available to you!");
            return;
        }
        context.sendTranslated(NEUTRAL, "The following warps are available to you:");
        for (Warp warp : warps)
        {
            if (warp.isPublic())
            {
                if (warp.isOwnedBy(owner))
                {
                    context.sendTranslated(NEUTRAL, "  {name#warp} ({text:public})", warp.getName());
                }
                else
                {
                    context.sendTranslated(NEUTRAL, "  {user}:{name#warp} ({text:public})", warp.getOwnerName(), warp.getName());
                }
                continue;
            }
            if (warp.isOwnedBy(owner))
            {
                context.sendTranslated(NEUTRAL, "  {name#warp} ({text:private})", warp.getName());
            }
            else
            {
                context.sendTranslated(NEUTRAL, "  {user}:{name#warp} ({text:private})", warp.getOwnerName(), warp.getName());
            }
        }
    }

    @Command(desc = "List all available warps")
    public void listAll(CommandSender sender)
    {
        int count = this.manager.getCount();
        if (count == 0)
        {
            sender.sendTranslated(POSITIVE, "There are no warps set.");
            return;
        }
        sender.sendTranslatedN(POSITIVE, count, "There is one warp set:", "There are {amount} warps set:", count);
        this.showList(sender, null, this.manager.list(true, true));
    }

    @Command(alias = {"ilist", "invited"}, desc = "List all players invited to your warps")
    public void invitedList(CommandContext sender, @Default User owner) // TODO named permission "other"
    {
        Set<Warp> warps = this.manager.list(owner, true, false, false).stream()
                                      .filter(w -> !w.getInvited().isEmpty())
                                      .collect(toSet());
        if (warps.isEmpty())
        {
            if (owner.equals(sender.getSource()))
            {
                sender.sendTranslated(NEGATIVE, "You have no warps with players invited to them!");
                return;
            }
            sender.sendTranslated(NEGATIVE, "{user} has no warps with players invited to them!", owner);
            return;
        }
        if (owner.equals(sender.getSource()))
        {
            sender.sendTranslated(NEUTRAL, "Your following warps have players invited to them:");
        }
        else
        {
            sender.sendTranslated(NEUTRAL, "The following warps of {user} have players invited to them:", owner);
        }
        // TODO do async db access here
        for (Warp w : warps)
        {
            Set<TeleportInvite> invites = this.iManager.getInvites(w.getModel());
            if (!invites.isEmpty())
            {
                sender.sendMessage(YELLOW + "  " + w.getName() + ":");
                for (TeleportInvite invite : invites)
                {
                    sender.sendMessage("    " + DARK_GREEN + this.module.getCore().getUserManager().getUser(
                        invite.getValue(TABLE_INVITE.USERKEY)).getDisplayName());
                }
            }
        }
    }

    @Restricted(User.class)
    @Command(desc = "Invite a user to one of your warps")
    public void invite(User sender, String warp, User player)
    {
        Warp w = this.manager.findOne(sender, warp);
        if (w == null || !w.isOwnedBy(sender))
        {
            sender.sendTranslated(NEGATIVE, "You do not own a warp named {name#warp}!", warp);
            return;
        }
        if (w.isPublic())
        {
            sender.sendTranslated(NEGATIVE, "You can't invite a person to a public warp.");
            return;
        }
        if (player.equals(sender))
        {
            sender.sendTranslated(NEGATIVE, "You cannot invite yourself to your own warp!");
            return;
        }
        if (w.isInvited(player))
        {
            sender.sendTranslated(NEGATIVE, "{user} is already invited to your warp!", player);
            return;
        }
        w.invite(player);
        if (player.isOnline())
        {
            player.sendTranslated(NEUTRAL, "{user} invited you to their private warp. To teleport to it use: /warp {name#warp} {user}", sender, w.getName(), sender);
        }
        sender.sendTranslated(POSITIVE, "{user} is now invited to your warp {name}", player, w.getName());
    }

    @Restricted(User.class)
    @Command(desc = "Uninvite a player from one of your warps")
    public void unInvite(User sender, String warp, User player)
    {
        Warp w = this.manager.getExact(sender, warp);
        if (w == null || !w.isOwnedBy(sender))
        {
            sender.sendTranslated(NEGATIVE, "You do not own a warp named {name#warp}!", warp);
            return;
        }
        if (w.isPublic())
        {
            sender.sendTranslated(NEGATIVE, "This warp is public. Make it private to disallow others to access it.");
            return;
        }
        if (player.equals(sender))
        {
            sender.sendTranslated(NEGATIVE, "You cannot uninvite yourself from your own warp!");
            return;
        }
        if (!w.isInvited(player))
        {
            sender.sendTranslated(NEGATIVE, "{user} is not invited to your warp!", player);
            return;
        }
        w.unInvite(player);
        if (player.isOnline())
        {
            player.sendTranslated(NEUTRAL, "You are no longer invited to {user}'s warp {name#warp}", sender, w.getName());
        }
        sender.sendTranslated(POSITIVE, "{user} is no longer invited to your warp {name}", player, w.getName());
    }

    @Command(name = "private", alias = "makeprivate", desc = "Make a players warp private")
    public void makePrivate(CommandSender sender, @Optional String warp, @Default User owner)
    {
        if (!owner.equals(sender) && !module.getPermissions().WARP_PUBLIC_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_PUBLIC_OTHER);
        }
        Warp w = this.manager.findOne(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (!w.isPublic())
        {
            sender.sendTranslated(NEGATIVE, "This warp is already private!");
            return;
        }
        w.setVisibility(PRIVATE);
        if (w.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "Your warp {name} is now private", w.getName());
            return;
        }
        sender.sendTranslated(POSITIVE, "The warp {name} of {user} is now private", w.getOwnerName(), w.getName());
    }

    @Command(name = "public", desc = "Make a users warp public")
    public void makePublic(CommandSender sender, @Optional String warp, @Default User owner)
    {
        if (!owner.equals(sender) && !module.getPermissions().WARP_PUBLIC_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_PUBLIC_OTHER);
        }
        Warp w = this.manager.findOne(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (w.isPublic())
        {
            sender.sendTranslated(NEGATIVE, "This warp is already public!");
            return;
        }
        w.setVisibility(PUBLIC);
        if (w.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "Your warp {name} is now public", w.getName());
            return;
        }
        sender.sendTranslated(POSITIVE, "The warp {name} of {user} is now public", w.getOwnerName(), w.getName());
    }

    @Alias(value = {"clearwarps"})
    @Command(desc = "Clear all warps (of a player)")
    public ConfirmResult clear(final CommandContext context, @Optional User player,
                               @Flag(name = "pub", longName = "public") boolean pub,
                               @Flag(name = "priv", longName = "private") boolean priv)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(context.getSource() instanceof ConsoleCommandSender))
        {
            context.sendTranslated(NEGATIVE, "This command has been disabled for ingame use via the configuration");
            return null;
        }
        if (player != null)
        {
            if (pub)
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all public warps ever created by {user}?", player);
            }
            else if (priv)
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all private warps ever created by {user}?", player);
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all warps ever created by {user}?", player);
            }
        }
        else
        {
            if (pub)
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all public warps ever created on this server!?");
            }
            else if (priv)
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all private warps ever created on this server?");
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all warps ever created on this server!?");
            }
        }
        context.sendTranslated(NEUTRAL, "Confirm with: {text:/confirm} before 30 seconds have passed to delete the warps");
        return new ConfirmResult(module, () -> {
            if (player != null)
            {
                manager.massDelete(player, priv, pub);
                context.sendTranslated(POSITIVE, "Deleted warps.");
            }
            else
            {
                manager.massDelete(priv, pub);
                context.sendTranslated(POSITIVE, "The warps are now deleted");
            }
        }, context);
    }


    private void warpInDeletedWorldMessage(CommandSender sender, Warp warp)
    {
        if (warp.isOwnedBy(sender))
        {
            sender.sendTranslated(NEGATIVE, "Your warp {name} is in a world that no longer exists!", warp.getName());
        }
        else
        {
            sender.sendTranslated(NEGATIVE, "The warp {name} of {user} is in a world that no longer exists!", warp.getName(), warp.getOwnerName());
        }
    }

    private void warpNotFoundMessage(CommandSender sender, User user, String name)
    {
        if (sender.equals(user))
        {
            sender.sendTranslated(NEGATIVE, "You have no warp named {name#warp}!", name);
        }
        else
        {
            sender.sendTranslated(NEGATIVE, "{user} has no warp named {name#warp}!", user, name);
        }
    }
}
