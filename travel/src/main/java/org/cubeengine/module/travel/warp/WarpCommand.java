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
package org.cubeengine.module.travel.warp;

import java.util.Set;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.travel.TpPointCommand;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.storage.TeleportInvite;
import org.cubeengine.module.travel.storage.TeleportPointModel.Visibility;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.exception.PermissionDeniedException;
import org.cubeengine.service.confirm.ConfirmResult;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.module.core.util.ChatFormat.DARK_GREEN;
import static org.cubeengine.module.core.util.ChatFormat.YELLOW;
import static org.cubeengine.module.travel.storage.TableInvite.TABLE_INVITE;
import static java.util.stream.Collectors.toSet;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

@Command(name = "warp", desc = "Teleport to a warp")
public class WarpCommand extends TpPointCommand
{
    private final Travel module;
    private I18n i18n;
    private final WarpManager manager;

    public WarpCommand(Travel module, I18n i18n)
    {
        super(module, i18n);
        this.module = module;
        this.i18n = i18n;
        this.manager = module.getWarpManager();
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof Player)
        {
            return getCommand("tp").execute(invocation);
        }
        return super.selfExecute(invocation);
    }

    @Restricted(Player.class)
    @Command(desc = "Teleport to a warp")
    public void tp(Player sender, String warp, @Default Player owner)
    {
        Warp w = manager.findOne(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (!w.canAccess(sender) && sender.hasPermission(module.getPermissions().WARP_TP_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_TP_OTHER);
        }
        Transform<World> location = w.getTransform();
        if (location == null)
        {
            warpInDeletedWorldMessage(sender, w);
            return;
        }
        sender.setTransform(location);
        if (w.getWelcomeMsg() != null)
        {
            sender.sendMessage(Text.of(w.getWelcomeMsg()));
            return;
        }
        if (w.isOwnedBy(sender))
        {
            i18n.sendTranslated(sender, POSITIVE, "You have been teleported to your warp {name}!", w.getName());
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "You have been teleported to the warp {name} of {user}!", w.getName(), w.getOwnerName());
    }

    @Restricted(Player.class)
    @Alias(value = {"createwarp", "mkwarp", "makewarp"})
    @Command(alias = "make", desc = "Create a warp")
    public void create(Player sender, String name, @Flag(name = "priv", longName = "private") boolean priv) // TODO flag permission "private"
    {
        if (this.manager.getCount() >= this.module.getConfig().warps.max)
        {
            i18n.sendTranslated(sender, CRITICAL, "The server have reached its maximum number of warps!");
            i18n.sendTranslated(sender, NEGATIVE, "Some warps must be deleted for new ones to be made");
            return;
        }
        if (manager.has(sender, name))
        {
            i18n.sendTranslated(sender, NEGATIVE, "A warp by that name already exist!");
            return;
        }
        if (name.contains(":") || name.length() >= 32)
        {
            i18n.sendTranslated(sender, NEGATIVE, "Warps may not have names that are longer than 32 characters nor contain colon(:)'s!");
            return;
        }
        if (this.manager.has(sender, name))
        {
            i18n.sendTranslated(sender, NEGATIVE, "The warp already exists! You can move it with {text:/warp move}");
            return;
        }
        Warp warp = manager.create(sender, name, sender.getTransform(), !priv);
        i18n.sendTranslated(sender, POSITIVE, "Your warp {name} has been created!", warp.getName());
    }

    @Command(desc = "Set the welcome message of warps", alias = {"setgreeting", "setwelcome", "setwelcomemsg"})
    public void greeting(CommandSource sender, String warp,
                         @Label("welcome message") @Greed(INFINITE) @Optional String message,
                         @Default @Named("owner") Player owner,
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
            i18n.sendTranslated(sender, POSITIVE, "The welcome message for your warp {name} is now set to:", w.getName());
        }
        else
        {
            i18n.sendTranslated(sender, POSITIVE, "The welcome message for the warp {name} of {user} is now set to:",
                                   w.getName(), owner);
        }
        sender.sendMessage(Text.of(w.getWelcomeMsg()));
    }

    @Restricted(Player.class)
    @Command(desc = "Move a warp")
    public void move(Player sender, String warp, @Default Player owner)
    {
        Warp w = manager.getExact(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (!w.isOwnedBy(sender) && sender.hasPermission(module.getPermissions().WARP_MOVE_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_MOVE_OTHER);
        }
        w.setLocation(sender.getTransform());
        w.update();
        if (w.isOwnedBy(sender))
        {
            i18n.sendTranslated(sender, POSITIVE, "Your warp {name} has been moved to your current location!", w.getName());
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "The warp {name} of {user} has been moved to your current location",
                               w.getName(), owner);
    }

    @Alias(value = {"removewarp", "deletewarp", "delwarp", "remwarp"})
    @Command(alias = "delete", desc = "Remove a warp")
    public void remove(CommandSource sender, String warp, @Default Player owner)
    {
        Warp w = manager.getExact(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (!w.isOwnedBy(sender) && sender.hasPermission(module.getPermissions().WARP_REMOVE_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_REMOVE_OTHER);
        }
        manager.delete(w);
        if (w.isOwnedBy(sender))
        {
            i18n.sendTranslated(sender, POSITIVE, "Your warp {name} has been removed", warp);
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "The warp {name} of {user} has been removed", warp, owner);
    }

    @Command(desc = "Rename a warp")
    public void rename(CommandSource sender, String warp, @Label("new name") String newName, @Default @Named("owner") Player owner)
    {
        Warp w = manager.getExact(owner, warp);
        if (w == null)
        {
            warpNotFoundMessage(sender, owner, warp);
            return;
        }
        if (!w.isOwnedBy(sender) && sender.hasPermission(module.getPermissions().WARP_RENAME_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_RENAME_OTHER);
        }
        if (warp.contains(":") || warp.length() >= 32)
        {
            i18n.sendTranslated(sender, NEGATIVE, "Warps may not have names that are longer than 32 characters or contain colon(:)'s!");
            return;
        }
        if (manager.rename(w, newName))
        {
            if (w.isOwnedBy(sender))
            {
                i18n.sendTranslated(sender, POSITIVE, "Your warp {name} has been renamed to {name}", w.getName(), newName);
                return;
            }
            i18n.sendTranslated(sender, POSITIVE, "The warp {name} of {user} has been renamed to {name}", w.getName(), owner, newName);
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "Could not rename the warp to {name}", newName);
    }

    @Command(desc = "List warps of a player")
    public void list(CommandContext context, @Default Player owner,
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
    public void listAll(CommandSource sender)
    {
        int count = this.manager.getCount();
        if (count == 0)
        {
            i18n.sendTranslated(sender, POSITIVE, "There are no warps set.");
            return;
        }
        i18n.sendTranslatedN(sender, POSITIVE, count, "There is one warp set:", "There are {amount} warps set:", count);
        this.showList(sender, null, this.manager.list(true, true));
    }

    @Command(alias = {"ilist", "invited"}, desc = "List all players invited to your warps")
    public void invitedList(CommandContext sender, @Default Player owner) // TODO named permission "other"
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
                    String name = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(invite.getValue(TABLE_INVITE.USERKEY)).get().getName();
                    sender.sendMessage("    " + DARK_GREEN + name);
                }
            }
        }
    }

    @Restricted(Player.class)
    @Command(desc = "Invite a user to one of your warps")
    public void invite(Player sender, String warp, Player player)
    {
        Warp w = this.manager.findOne(sender, warp);
        if (w == null || !w.isOwnedBy(sender))
        {
            i18n.sendTranslated(sender, NEGATIVE, "You do not own a warp named {name#warp}!", warp);
            return;
        }
        if (w.isPublic())
        {
            i18n.sendTranslated(sender, NEGATIVE, "You can't invite a person to a public warp.");
            return;
        }
        if (player.equals(sender))
        {
            i18n.sendTranslated(sender, NEGATIVE, "You cannot invite yourself to your own warp!");
            return;
        }
        if (w.isInvited(player))
        {
            i18n.sendTranslated(sender, NEGATIVE, "{user} is already invited to your warp!", player);
            return;
        }
        w.invite(player);
        i18n.sendTranslated(sender, NEUTRAL,
                              "{user} invited you to their private warp. To teleport to it use: /warp {name#warp} {user}",
                              sender, w.getName(), sender);
        i18n.sendTranslated(sender, POSITIVE, "{user} is now invited to your warp {name}", player, w.getName());
    }

    @Restricted(Player.class)
    @Command(desc = "Uninvite a player from one of your warps")
    public void unInvite(Player sender, String warp, Player player)
    {
        Warp w = this.manager.getExact(sender, warp);
        if (w == null || !w.isOwnedBy(sender))
        {
            i18n.sendTranslated(sender, NEGATIVE, "You do not own a warp named {name#warp}!", warp);
            return;
        }
        if (w.isPublic())
        {
            i18n.sendTranslated(sender, NEGATIVE, "This warp is public. Make it private to disallow others to access it.");
            return;
        }
        if (player.equals(sender))
        {
            i18n.sendTranslated(sender, NEGATIVE, "You cannot uninvite yourself from your own warp!");
            return;
        }
        if (!w.isInvited(player))
        {
            i18n.sendTranslated(sender, NEGATIVE, "{user} is not invited to your warp!", player);
            return;
        }
        w.unInvite(player);
        i18n.sendTranslated(player, NEUTRAL, "You are no longer invited to {user}'s warp {name#warp}", sender, w.getName());
        i18n.sendTranslated(sender, POSITIVE, "{user} is no longer invited to your warp {name}", player, w.getName());
    }

    @Command(name = "private", alias = "makeprivate", desc = "Make a players warp private")
    public void makePrivate(CommandSource sender, @Optional String warp, @Default Player owner)
    {
        if (!owner.equals(sender) && sender.hasPermission(module.getPermissions().WARP_PUBLIC_OTHER.getId()))
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
            i18n.sendTranslated(sender, NEGATIVE, "This warp is already private!");
            return;
        }
        w.setVisibility(Visibility.PRIVATE);
        if (w.isOwnedBy(sender))
        {
            i18n.sendTranslated(sender, POSITIVE, "Your warp {name} is now private", w.getName());
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "The warp {name} of {user} is now private", w.getOwnerName(), w.getName());
    }

    @Command(name = "public", desc = "Make a users warp public")
    public void makePublic(CommandSource sender, @Optional String warp, @Default Player owner)
    {
        if (!owner.equals(sender) && sender.hasPermission(module.getPermissions().WARP_PUBLIC_OTHER.getId()))
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
            i18n.sendTranslated(sender, NEGATIVE, "This warp is already public!");
            return;
        }
        w.setVisibility(Visibility.PUBLIC);
        if (w.isOwnedBy(sender))
        {
            i18n.sendTranslated(sender, POSITIVE, "Your warp {name} is now public", w.getName());
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "The warp {name} of {user} is now public", w.getOwnerName(), w.getName());
    }

    @Alias(value = "clearwarps")
    @Command(desc = "Clear all warps (of a player)")
    public ConfirmResult clear(final CommandContext context, @Optional Player owner,
                               @Flag(name = "pub", longName = "public") boolean pub,
                               @Flag(name = "priv", longName = "private") boolean priv)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(context.getSource() instanceof ConsoleSource))
        {
            context.sendTranslated(NEGATIVE, "This command has been disabled for ingame use via the configuration");
            return null;
        }
        if (owner != null)
        {
            if (pub)
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all public warps ever created by {user}?", owner);
            }
            else if (priv)
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all private warps ever created by {user}?", owner);
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all warps ever created by {user}?", owner);
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
            if (owner != null)
            {
                manager.massDelete(owner, priv, pub);
                context.sendTranslated(POSITIVE, "Deleted warps.");
            }
            else
            {
                manager.massDelete(priv, pub);
                context.sendTranslated(POSITIVE, "The warps are now deleted");
            }
        }, context);
    }


    private void warpInDeletedWorldMessage(Player sender, Warp warp)
    {
        if (warp.isOwnedBy(sender))
        {
            i18n.sendTranslated(sender, NEGATIVE, "Your warp {name} is in a world that no longer exists!", warp.getName());
        }
        else
        {
            i18n.sendTranslated(sender, NEGATIVE, "The warp {name} of {user} is in a world that no longer exists!", warp.getName(), warp.getOwnerName());
        }
    }

    private void warpNotFoundMessage(CommandSource sender, Player user, String name)
    {
        if (sender.equals(user))
        {
            i18n.sendTranslated(sender, NEGATIVE, "You have no warp named {name#warp}!", name);
        }
        else
        {
            i18n.sendTranslated(sender, NEGATIVE, "{user} has no warp named {name#warp}!", user, name);
        }
    }
}
