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

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;

import de.cubeisland.engine.core.command.CubeContext;
import de.cubeisland.engine.core.command.reflected.Alias;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.OnlyIngame;
import de.cubeisland.engine.core.command.reflected.context.Flag;
import de.cubeisland.engine.core.command.reflected.context.Flags;
import de.cubeisland.engine.core.command.reflected.context.Grouped;
import de.cubeisland.engine.core.command.reflected.context.IParams;
import de.cubeisland.engine.core.command.reflected.context.Indexed;
import de.cubeisland.engine.core.command.reflected.context.NParams;
import de.cubeisland.engine.core.command.reflected.context.Named;
import de.cubeisland.engine.core.command.result.confirm.ConfirmResult;
import de.cubeisland.engine.core.command.sender.ConsoleCommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.travel.TpPointCommand;
import de.cubeisland.engine.module.travel.Travel;
import de.cubeisland.engine.module.travel.storage.TeleportInvite;
import de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility;

import static de.cubeisland.engine.core.util.ChatFormat.DARK_GREEN;
import static de.cubeisland.engine.core.util.ChatFormat.YELLOW;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.travel.storage.TableInvite.TABLE_INVITE;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PUBLIC;
import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND;

public class WarpCommand extends TpPointCommand
{
    private final Travel module;
    private final WarpManager manager;

    public WarpCommand(Travel module)
    {
        super(module, "warp", "Teleport to a warp");
        this.delegateChild(new DelegatingContextFilter()
        {
            @Override
            public String delegateTo(CubeContext context)
            {
                return context.isSender(User.class) && context.getIndexedCount() > 0 ? "tp" : null;
            }
        });
        this.module = module;
        this.manager = module.getWarpManager();
    }

    @OnlyIngame
    @Command(desc = "Teleport to a warp")
    @IParams({@Grouped(@Indexed(label = "warp")),
              @Grouped(req = false, value = @Indexed(label = "owner", type = User.class))})
    public void tp(CubeContext context)
    {
        User user = getUser(context, 1);
        User sender = (User)context.getSender();
        Warp warp = manager.findOne(user, context.getString(0));
        if (warp == null)
        {
            warpNotFoundMessage(context, user, context.getString(0));
            return;
        }
        if (!warp.canAccess(sender))
        {
            context.ensurePermission(module.getPermissions().WARP_TP_OTHER);
        }
        Location location = warp.getLocation();
        if (location == null)
        {
            warpInDeletedWorldMessage(context, user, warp);
            return;
        }
        if (sender.teleport(location, COMMAND))
        {
            if (warp.getWelcomeMsg() != null)
            {
                context.sendMessage(warp.getWelcomeMsg());
            }
            else
            {
                if (warp.isOwner(sender))
                {
                    context.sendTranslated(POSITIVE, "You have been teleported to your warp {name}!", warp.getName());
                }
                else
                {
                    context.sendTranslated(POSITIVE, "You have been teleported to the warp {name} of {user}!", warp.getName(), warp.getOwnerName());
                }
            }
            return;
        }
        context.sendTranslated(CRITICAL, "The teleportation got aborted!");
    }

    @OnlyIngame
    @Alias(names = {"createwarp", "mkwarp", "makewarp"})
    @Command(alias = "make", desc = "Create a warp")
    @IParams(@Grouped(@Indexed(label = "name")))
    @Flags(@Flag(name = "priv", longName = "private", permission = "private"))
    public void create(CubeContext context)
    {
        if (this.manager.getCount() >= this.module.getConfig().warps.max)
        {
            context.sendTranslated(CRITICAL, "The server have reached its maximum number of warps!");
            context.sendTranslated(NEGATIVE, "Some warps must be deleted for new ones to be made");
            return;
        }
        User sender = (User)context.getSender();
        String name = context.getArg(0);
        if (manager.has(sender, name))
        {
            context.sendTranslated(NEGATIVE, "A warp by that name already exist!");
            return;
        }
        if (name.contains(":") || name.length() >= 32)
        {
            context.sendTranslated(NEGATIVE, "Warps may not have names that are longer than 32 characters nor contain colon(:)'s!");
            return;
        }
        if (this.manager.has(sender, name))
        {
            context.sendTranslated(NEGATIVE, "The warp already exists! You can move it with {text:/warp move}");
            return;
        }
        Warp warp = manager.create(sender, name, sender.getLocation(), !context.hasFlag("priv"));
        context.sendTranslated(POSITIVE, "Your warp {name} has been created!", warp.getName());
    }

    @Command(desc = "Set the welcome message of warps", alias = {"setgreeting", "setwelcome", "setwelcomemsg"})
    @IParams({@Grouped(@Indexed(label = "warp")),
              @Grouped(req = false, value = @Indexed(label = "welcome message"), greedy = true)})
    @NParams(@Named(names = "owner", type = User.class, permission = "other"))
    @Flags(@Flag(longName = "append", name = "a"))
    public void greeting(CubeContext context)
    {
        User user = this.getUser(context, "owner");
        String name = context.getArg(0);
        Warp warp = this.manager.getExact(user, name);
        if (warp == null)
        {
            warpNotFoundMessage(context, user, name);
            return;
        }
        if (context.hasFlag("a"))
        {
            warp.setWelcomeMsg(warp.getWelcomeMsg() + context.getStrings(1));
        }
        else
        {
            warp.setWelcomeMsg(context.getStrings(1));
        }
        warp.update();
        if (warp.isOwner(context.getSender()))
        {
            context.sendTranslated(POSITIVE, "The welcome message for your warp {name} is now set to:", warp.getName());
        }
        else
        {
            context.sendTranslated(POSITIVE, "The welcome message for the warp {name} of {user} is now set to:", warp.getName(), user);
        }
        context.sendMessage(warp.getWelcomeMsg());
    }

    @OnlyIngame
    @Command(desc = "Move a warp")
    @IParams({@Grouped(value = @Indexed(label = "warp")),
              @Grouped(req = false, value = @Indexed(label = "owner", type = User.class))})
    public void move(CubeContext context)
    {
        User user = this.getUser(context, 1);
        String name = context.getArg(0);
        Warp warp = manager.getExact(user, name);
        if (warp == null)
        {
            warpNotFoundMessage(context, user, name);
            return;
        }
        if (!warp.isOwner(context.getSender()))
        {
            context.ensurePermission(module.getPermissions().WARP_MOVE_OTHER);
        }
        User sender = (User)context.getSender();
        warp.setLocation(sender.getLocation());
        warp.update();
        if (warp.isOwner(sender))
        {
            context.sendTranslated(POSITIVE, "Your warp {name} has been moved to your current location!", warp.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "The warp {name} of {user} has been moved to your current location", warp.getName(), user);
    }

    @Alias(names = {"removewarp", "deletewarp", "delwarp", "remwarp"})
    @Command(alias = "delete", desc = "Remove a warp")
    @IParams({@Grouped(@Indexed(label = "warp")),
              @Grouped(req = false, value = @Indexed(label = "owner", type = User.class))})
    public void remove(CubeContext context)
    {
        User user = getUser(context, 1);
        String name = context.getArg(0);
        Warp warp = manager.getExact(user, name);
        if (warp == null)
        {
            warpNotFoundMessage(context, user, name);
            return;
        }
        if (!warp.isOwner(context.getSender()))
        {
            context.ensurePermission(module.getPermissions().WARP_REMOVE_OTHER);
        }
        manager.delete(warp);
        if (warp.isOwner(context.getSender()))
        {
            context.sendTranslated(POSITIVE, "Your warp {name} has been removed", name);
            return;
        }
        context.sendTranslated(POSITIVE, "The warp {name} of {user} has been removed", name, user);
    }

    @Command(desc = "Rename a warp")
    @IParams({@Grouped(@Indexed(label = "warp")),
              @Grouped(@Indexed(label = "new name"))})
    @NParams(@Named(names = "owner", type = User.class))
    public void rename(CubeContext context)
    {
        User user = getUser(context, "owner");
        String name = context.getArg(0);
        Warp warp = manager.getExact(user, name);
        if (warp == null)
        {
            warpNotFoundMessage(context, user, name);
            return;
        }
        if (!warp.isOwner(context.getSender()))
        {
            context.ensurePermission(module.getPermissions().WARP_RENAME_OTHER);
        }
        String newName = context.getArg(1);
        if (name.contains(":") || name.length() >= 32)
        {
            context.sendTranslated(NEGATIVE, "Warps may not have names that are longer than 32 characters or contain colon(:)'s!");
            return;
        }
        if (manager.rename(warp, newName))
        {
            if (warp.isOwner(context.getSender()))
            {
                context.sendTranslated(POSITIVE, "Your warp {name} has been renamed to {name}", warp.getName(), newName);
                return;
            }
            context.sendTranslated(POSITIVE, "The warp {name} of {user} has been renamed to {name}", warp.getName(), user, newName);
            return;
        }
        context.sendTranslated(POSITIVE, "Could not rename the warp to {name}", newName);
    }

    @Command(desc = "List all available warps")
    @IParams(@Grouped(req = false, value = @Indexed(label = {"owner","!*"}, type = {User.class, String.class})))
    @Flags({@Flag(name = "pub", longName = "public"),
            @Flag(name = "o", longName = "owned"),
            @Flag(name = "i", longName = "invited")})
    public void list(CubeContext context)
    {
        if ((context.hasIndexed(0) && "*".equals(context.getArg(0))) || !(context.hasIndexed(0) || context.isSender(User.class)))
        {
            context.ensurePermission(module.getPermissions().WARP_LIST_OTHER);
            this.listAll(context);
            return;
        }
        User user = this.getUser(context, 0);
        if (!user.equals(context.getSender()))
        {
            context.ensurePermission(module.getPermissions().WARP_LIST_OTHER);
        }
        Set<Warp> warps = this.manager.list(user, context.hasFlag("o"), context.hasFlag("pub"), context.hasFlag("i"));
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
                if (warp.isOwner(user))
                {
                    context.sendTranslated(NEUTRAL, "  {name#warp} ({text:public})", warp.getName());
                }
                else
                {
                    context.sendTranslated(NEUTRAL, "  {user}:{name#warp} ({text:public})", warp.getOwnerName(), warp.getName());
                }
            }
            else
            {
                if (warp.isOwner(user))
                {
                    context.sendTranslated(NEUTRAL, "  {name#warp} ({text:private})", warp.getName());
                }
                else
                {
                    context.sendTranslated(NEUTRAL, "  {user}:{name#warp} ({text:private})", warp.getOwnerName(), warp.getName());
                }
            }
        }
    }

    private void listAll(CubeContext context)
    {
        int count = this.manager.getCount();
        if (count == 0)
        {
            context.sendTranslated(POSITIVE, "There are no warps set.");
            return;
        }
        context.sendTranslatedN(POSITIVE, count, "There is one warp set:", "There are {amount} warps set:", count);
        this.showList(context, null, this.manager.list(true, true));
    }

    @Command(alias = {"ilist", "invited"}, desc = "List all players invited to your warps")
    @IParams(@Grouped(req = false, value = @Indexed(label = "warp")))
    @NParams(@Named(names = "owner", type = User.class, permission = "other"))
    public void invitedList(CubeContext context)
    {
        User user = this.getUser(context, "owner");
        Set<Warp> warps = new HashSet<>();
        for (Warp warp : this.manager.list(user, true, false, false))
        {
            if (!warp.getInvited().isEmpty())
            {
                warps.add(warp);
            }
        }
        if (warps.isEmpty())
        {
            if (user.equals(context.getSender()))
            {
                context.sendTranslated(NEGATIVE, "You have no warps with players invited to them!");
                return;
            }
            context.sendTranslated(NEGATIVE, "{user} has no warps with players invited to them!", user);
            return;
        }
        if (user.equals(context.getSender()))
        {
            context.sendTranslated(NEUTRAL, "Your following warps have players invited to them:");
        }
        else
        {
            context.sendTranslated(NEUTRAL, "The following warps of {user} have players invited to them:", user);
        }
        for (Warp warp : warps)
        {
            Set<TeleportInvite> invites = this.iManager.getInvites(warp.getModel());
            if (!invites.isEmpty())
            {
                context.sendMessage(YELLOW + "  " + warp.getName() + ":");
                for (TeleportInvite invite : invites)
                {
                    context.sendMessage("    " + DARK_GREEN + this.module.getCore().getUserManager().getUser(invite.getValue(TABLE_INVITE.USERKEY)).getDisplayName());
                }
            }
        }
    }

    @OnlyIngame
    @Command(desc = "Invite a user to one of your warps")
    @IParams({@Grouped(@Indexed(label = "warp")),
              @Grouped(@Indexed(label = "player", type = User.class))})
    public void invite(CubeContext context)
    {
        User sender = (User)context.getSender();
        Warp warp = this.manager.findOne(sender, context.getString(0));
        if (warp == null || !warp.isOwner(sender))
        {
            context.sendTranslated(NEGATIVE, "You do not own a warp named {name#warp}!", context.getArg(0));
            return;
        }
        if (warp.isPublic())
        {
            context.sendTranslated(NEGATIVE, "You can't invite a person to a public warp.");
            return;
        }
        User invited = context.getArg(1);
        if (invited.equals(sender))
        {
            context.sendTranslated(NEGATIVE, "You cannot invite yourself to your own warp!");
            return;
        }
        if (warp.isInvited(invited))
        {
            context.sendTranslated(NEGATIVE, "{user} is already invited to your warp!", invited);
            return;
        }
        warp.invite(invited);
        if (invited.isOnline())
        {
            invited.sendTranslated(NEUTRAL, "{user} invited you to their private warp. To teleport to it use: /warp {name#warp} {user}", sender, warp.getName(), sender);
        }
        context.sendTranslated(POSITIVE, "{user} is now invited to your warp {name}", invited, warp.getName());
    }

    @OnlyIngame
    @Command(desc = "Uninvite a player from one of your warps")
    @IParams({@Grouped(value = @Indexed(label = "warp")),
              @Grouped(@Indexed(label = "player", type = User.class))})
    public void unInvite(CubeContext context)
    {
        User sender = (User)context.getSender();
        Warp warp = this.manager.getExact(sender, context.getString(0));
        if (warp == null || !warp.isOwner(sender))
        {
            context.sendTranslated(NEGATIVE, "You do not own a warp named {name#warp}!", context.getArg(0));
            return;
        }
        if (warp.isPublic())
        {
            context.sendTranslated(NEGATIVE, "This warp is public. Make it private to disallow others to access it.");
            return;
        }
        User invited = context.getArg(1);
        if (invited.equals(sender))
        {
            context.sendTranslated(NEGATIVE, "You cannot uninvite yourself from your own warp!");
            return;
        }
        if (!warp.isInvited(invited))
        {
            context.sendTranslated(NEGATIVE, "{user} is not invited to your warp!", invited);
            return;
        }
        warp.unInvite(invited);
        if (invited.isOnline())
        {
            invited.sendTranslated(NEUTRAL, "You are no longer invited to {user}'s warp {name#warp}", sender, warp.getName());
        }
        context.sendTranslated(POSITIVE, "{user} is no longer invited to your warp {name}", invited, warp.getName());
    }

    @Command(name = "private", alias = "makeprivate", desc = "Make a players warp private")
    @IParams({@Grouped(req = false, value = @Indexed(label = "warp")),
              @Grouped(req = false, value = @Indexed(label = "owner", type = User.class))})
    public void makePrivate(CubeContext context)
    {
        User user = this.getUser(context, 1);
        if (!user.equals(context.getSender()))
        {
            context.ensurePermission(module.getPermissions().WARP_PUBLIC_OTHER);
        }
        String name = context.getArg(0);
        Warp warp = this.manager.findOne(user, name);
        if (warp == null)
        {
            warpNotFoundMessage(context, user, name);
            return;
        }
        if (!warp.isPublic())
        {
            context.sendTranslated(NEGATIVE, "This warp is already private!");
            return;
        }
        warp.setVisibility(Visibility.PRIVATE);
        if (warp.isOwner(context.getSender()))
        {
            context.sendTranslated(POSITIVE, "Your warp {name} is now private", warp.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "The warp {name} of {user} is now private", warp.getOwnerName(), warp.getName());
    }

    @Command(name = "public", desc = "Make a users warp public")
    @IParams({@Grouped(req = false, value = @Indexed(label = "warp")),
              @Grouped(req = false, value = @Indexed(label = "owner", type = User.class))})
    public void makePublic(CubeContext context)
    {
        User user = this.getUser(context, 1);
        if (!user.equals(context.getSender()))
        {
            context.ensurePermission(module.getPermissions().WARP_PUBLIC_OTHER);
        }
        String name = context.getArg(0);
        Warp warp = this.manager.findOne(user, name);
        if (warp == null)
        {
            warpNotFoundMessage(context, user, name);
            return;
        }
        if (warp.isPublic())
        {
            context.sendTranslated(NEGATIVE, "This warp is already public!");
            return;
        }
        warp.setVisibility(PUBLIC);
        if (warp.isOwner(context.getSender()))
        {
            context.sendTranslated(POSITIVE, "Your warp {name} is now public", warp.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "The warp {name} of {user} is now public", warp.getOwnerName(), warp.getName());
    }

    @Alias(names = {"clearwarps"})
    @Command(desc = "Clear all warps (of a player)")
    @IParams(@Grouped(req = false, value = @Indexed(label = "player", type = User.class)))
    @Flags({@Flag(name = "pub", longName = "public"),
            @Flag(name = "priv", longName = "private")})
    public ConfirmResult clear(final CubeContext context)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(context.getSender() instanceof ConsoleCommandSender))
        {
            context.sendTranslated(NEGATIVE, "This command has been disabled for ingame use via the configuration");
            return null;
        }
        final User user = context.getArg(0, null);
        if (context.hasIndexed(0))
        {
            if (context.hasFlag("pub"))
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all public warps ever created by {user}?",
                                       user);
            }
            else if (context.hasFlag("priv"))
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all private warps ever created by {user}?",
                                       user);
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all warps ever created by {user}?",
                                       user);
            }
        }
        else
        {
            if (context.hasFlag("pub"))
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all public warps ever created on this server!?");
            }
            else if (context.hasFlag("priv"))
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all private warps ever created on this server?");
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all warps ever created on this server!?");
            }
        }
        context.sendTranslated(NEUTRAL, "Confirm with: {text:/confirm} before 30 seconds have passed to delete the warps");
        return new ConfirmResult(new Runnable()
        {
            @Override
            public void run()
            {
                if (context.hasIndexed(0))
                {
                    manager.massDelete(user, context.hasFlag("priv"), context.hasFlag("pub"));
                    context.sendTranslated(POSITIVE, "Deleted warps.");
                }
                else
                {
                    manager.massDelete(context.hasFlag("priv"), context.hasFlag("pub"));
                    context.sendTranslated(POSITIVE, "The warps are now deleted");
                }
            }
        }, context);
    }


    private void warpInDeletedWorldMessage(CubeContext context, User user, Warp warp)
    {
        if (warp.isOwner(user))
        {
            context.sendTranslated(NEGATIVE, "Your warp {name} is in a world that no longer exists!", warp.getName());
            return;
        }
        context.sendTranslated(NEGATIVE, "The warp {name} of {user} is in a world that no longer exists!",warp.getName(), warp.getOwnerName());
    }

    private void warpNotFoundMessage(CubeContext context, User user, String name)
    {
        if (context.getSender().equals(user))
        {
            context.sendTranslated(NEGATIVE, "You have no warp named {name#warp}!", name);
            return;
        }
        context.sendTranslated(NEGATIVE, "{user} has no warp named {name#warp}!", user, name);
    }
}