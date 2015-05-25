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
package de.cubeisland.engine.module.travel.home;

import java.util.Set;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.alias.Alias;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.butler.result.CommandResult;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.command.annotation.ParameterPermission;
import de.cubeisland.engine.module.service.command.exception.PermissionDeniedException;
import de.cubeisland.engine.module.service.command.result.confirm.ConfirmResult;
import de.cubeisland.engine.module.service.command.sender.ConsoleCommandSender;
import de.cubeisland.engine.module.service.Selector;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.math.Cuboid;
import de.cubeisland.engine.module.core.util.math.shape.Shape;
import de.cubeisland.engine.module.travel.TpPointCommand;
import de.cubeisland.engine.module.travel.Travel;
import de.cubeisland.engine.module.travel.storage.TeleportInvite;
import org.bukkit.Location;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import de.cubeisland.engine.module.core.util.ChatFormat.DARK_GREEN;
import de.cubeisland.engine.module.core.util.ChatFormat.YELLOW;
import static de.cubeisland.engine.module.travel.storage.TableInvite.TABLE_INVITE;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PRIVATE;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PUBLIC;
import static java.util.stream.Collectors.toSet;
import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND;

@Command(name = "home", desc = "Teleport to your home")
public class HomeCommand extends TpPointCommand
{
    private final HomeManager manager;
    private final Travel module;

    public HomeCommand(Travel module)
    {
        super(module);
        this.module = module;
        this.manager = module.getHomeManager();
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
    @Command(desc = "Teleport to a home")
    public void tp(User sender, @Optional String home, @Default User owner)
    {
        home = home == null ? "home" : home;
        Home h = this.manager.findOne(owner, home);
        if (h == null)
        {
            homeNotFoundMessage(sender, owner, home);
            sender.sendTranslated(NEUTRAL, "Use {text:/sethome} to set your home");
            return;
        }
        if (!h.canAccess(sender))
        {
            if (owner.equals(sender))
            {
                homeNotFoundMessage(sender, owner, home);
                return;
            }
            if (!module.getPermissions().HOME_TP_OTHER.isAuthorized(sender))
            {
                throw new PermissionDeniedException(module.getPermissions().HOME_TP_OTHER);
            }
        }
        Location location = h.getLocation();
        if (location == null)
        {
            homeInDeletedWorldMessage(sender, h);
            return;
        }
        if (!sender.teleport(location, COMMAND))
        {
            sender.sendTranslated(CRITICAL, "The teleportation got aborted!");
            return;
        }
        if (h.getWelcomeMsg() != null)
        {
            sender.sendMessage(h.getWelcomeMsg());
            return;
        }
        if (h.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "You have been teleported to your home {name}!", h.getName());
            return;
        }
        sender.sendTranslated(POSITIVE, "You have been teleported to the home {name} of {user}!", h.getName(),
                               h.getOwnerName());
    }

    @Alias("sethome")
    @Command(alias = {"create", "sethome", "createhome"}, desc = "Set your home")
    @Restricted(value = User.class, msg = "Ok so I'll need your new address then. No seriously this won't work!")
    public void set(CommandContext context, @Optional String name, @ParameterPermission @Flag(longName = "public", name = "pub") boolean isPublic)
    {
        User sender = (User)context.getSource();
        if (this.manager.getCount(sender) >= this.module.getConfig().homes.max && !module.getPermissions().HOME_SET_MORE.isAuthorized(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You have reached your maximum number of homes!");
            context.sendTranslated(NEUTRAL, "You have to delete a home to make a new one");
            return;
        }
        name = name == null ? "home" : name;
        if (name.contains(":") || name.length() >= 32)
        {
            context.sendTranslated(NEGATIVE, "Homes may not have names that are longer then 32 characters nor contain colon(:)'s!");
            return;
        }
        if (this.manager.has(sender, name))
        {
            context.sendTranslated(NEGATIVE, "The home already exists! You can move it with {text:/home move}");
            return;
        }
        Home home = this.manager.create(sender, name, sender.getLocation(), isPublic);
        context.sendTranslated(POSITIVE, "Your home {name} has been created!", home.getName());
    }

    @Command(desc = "Set the welcome message of homes", alias = {"setgreeting", "setwelcome", "setwelcomemsg"})
    public void greeting(CommandSender sender,
                         String home,
                         @Optional @Label("welcome message") @Greed(INFINITE) String message,
                         @Default @Named("owner") User owner,
                         @Flag boolean append)
    {
        Home h = this.manager.getExact(owner, home);
        if (h == null)
        {
            homeNotFoundMessage(sender, owner, home);
            return;
        }
        if (append)
        {
            h.setWelcomeMsg(h.getWelcomeMsg() + message);
        }
        else
        {
            h.setWelcomeMsg(message);
        }
        h.update();
        if (h.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "The welcome message for your home {name} is now set to:", h.getName());
        }
        else
        {
            sender.sendTranslated(POSITIVE, "The welcome message for the home {name} of {user} is now set to:",
                                   h.getName(), owner);
        }
        sender.sendMessage(h.getWelcomeMsg());
    }

    @Restricted(value = User.class, msg = "I am calling the moving company right now!")
    @Command(alias = "replace", desc = "Move a home")
    public void move(User sender, @Optional String name, @Default User owner)
    {
        name = name == null ? "home" : name;
        Home home = this.manager.getExact(owner, name);
        if (home == null)
        {
            homeNotFoundMessage(sender, owner, name);
            sender.sendTranslated(NEUTRAL, "Use {text:/sethome} to set your home");
            return;
        }
        if (!home.isOwnedBy(sender))
        {
            if (!module.getPermissions().HOME_MOVE_OTHER.isAuthorized(sender))
            {
                throw new PermissionDeniedException(module.getPermissions().HOME_MOVE_OTHER);
            }
        }
        home.setLocation(sender.getLocation());
        home.update();
        if (home.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "Your home {name} has been moved to your current location!", home.getName());
            return;
        }
        sender.sendTranslated(POSITIVE, "The home {name} of {user} has been moved to your current location",
                               home.getName(), owner);
    }

    @Alias(value = {"remhome", "removehome", "delhome", "deletehome"})
    @Command(alias = {"delete", "rem", "del"}, desc = "Remove a home")
    public void remove(CommandSender sender, @Optional String name, @Default @Optional User owner)
    {
        Home home = this.manager.getExact(owner, name == null ? "home" : name);
        if (home == null)
        {
            homeNotFoundMessage(sender, owner, name == null ? "home" : name);
            return;
        }
        if (!home.isOwnedBy(sender) && !module.getPermissions().HOME_REMOVE_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().HOME_REMOVE_OTHER);
        }
        this.manager.delete(home);
        if (owner.equals(sender))
        {
            sender.sendTranslated(POSITIVE, "Your home {name} has been removed!", name);
            return;
        }
        sender.sendTranslated(POSITIVE, "The home {name} of {user} has been removed", name, owner);
    }

    @Command(desc = "Rename a home")
    public void rename(CommandSender sender, String name, @Label("new name") String newName, @Default @Optional User owner)
    {
        Home home = manager.getExact(owner, name);
        if (home == null)
        {
            homeNotFoundMessage(sender, owner, name);
            return;
        }
        if (!home.isOwnedBy(sender) && !module.getPermissions().HOME_RENAME_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().HOME_RENAME_OTHER);
        }
        if (name.contains(":") || name.length() >= 32)
        {
            sender.sendTranslated(NEGATIVE,
                                   "Homes may not have names that are longer than 32 characters or contain colon(:)'s!");
            return;
        }
        String oldName = home.getName();
        if (!manager.rename(home, newName))
        {
            sender.sendTranslated(POSITIVE, "Could not rename the home to {name}", newName);
            return;
        }
        if (home.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "Your home {name} has been renamed to {name}", oldName, newName);
            return;
        }
        sender.sendTranslated(POSITIVE, "The home {name} of {user} has been renamed to {name}", oldName, owner,
                               newName);
    }

    @Alias(value = {"listhomes", "homes"})
    @Command(alias = "listhomes", desc = "Lists homes a player can access")
    public void list(CommandContext sender, @Default User owner,
                     @Flag(name = "pub", longName = "public") boolean isPublic,
                     @Flag boolean owned,
                     @Flag boolean invited) throws Exception
    {
        if (!owner.equals(sender.getSource()))
        {
            sender.ensurePermission(module.getPermissions().HOME_LIST_OTHER);
        }
        Set<Home> homes = this.manager.list(owner, owned, isPublic, invited);
        if (homes.isEmpty())
        {
            if (owner.equals(sender.getSource()))
            {
                sender.sendTranslated(NEGATIVE, "No homes are available to you!");
                return;
            }
            sender.sendTranslated(NEGATIVE, "No homes are available to {user}!", owner);
            return;
        }
        if (owner.equals(sender.getSource()))
        {
            sender.sendTranslated(NEUTRAL, "The following homes are available to you:");
        }
        else
        {
            sender.sendTranslated(NEUTRAL, "The following homes are available to {user}:", owner.getDisplayName());
        }
        showList(sender.getSource(), owner, homes);
    }

    @Command(alias = "listhomes", desc = "Lists all homes")
    public void listAll(CommandSender sender)
    {
        int count = this.manager.getCount();
        if (count == 0)
        {
            sender.sendTranslated(POSITIVE, "There are no homes set.");
            return;
        }
        sender.sendTranslatedN(POSITIVE, count, "There is one home set:", "There are {amount} homes set:", count);
        this.showList(sender, null, this.manager.list(true, true));
    }

    @Command(name = "ilist", alias = "invited", desc = "List all players invited to your homes")
    public void invitedList(CommandSender sender, @Default User owner)
    {
        if (!owner.equals(sender) && !module.getPermissions().HOME_LIST_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().HOME_LIST_OTHER);
        }
        Set<Home> homes = this.manager.list(owner, true, false, false).stream()
                                      .filter(home -> !home.getInvited().isEmpty())
                                      .collect(toSet());
        if (homes.isEmpty())
        {
            if (owner.equals(sender))
            {
                sender.sendTranslated(NEGATIVE, "You have no homes with players invited to them!");
                return;
            }
            sender.sendTranslated(NEGATIVE, "{user} has no homes with players invited to them!", owner);
            return;
        }
        if (owner.equals(sender))
        {
            sender.sendTranslated(NEUTRAL, "Your following homes have players invited to them:");
        }
        else
        {
            sender.sendTranslated(NEUTRAL, "The following homes of {user} have players invited to them:", owner);
        }
        for (Home home : homes)
        {
            Set<TeleportInvite> invites = this.iManager.getInvites(home.getModel());
            if (!invites.isEmpty())
            {
                sender.sendMessage(YELLOW + "  " + home.getName() + ":");
                for (TeleportInvite invite : invites)
                {
                    sender.sendMessage("    " + DARK_GREEN + this.module.getCore().getUserManager().getUser(
                        invite.getValue(TABLE_INVITE.USERKEY)).getDisplayName());
                }
            }
        }
    }

    @Restricted(value = User.class, msg = "How about making a phone call to invite someone instead?")
    @Command(desc = "Invite a user to one of your homes")
    public void invite(User sender, User player, @Optional String home)
    {
        home = home == null ? "home" : home;
        Home h = this.manager.getExact(sender, home);
        if (h == null || !h.isOwnedBy(sender))
        {
            sender.sendTranslated(NEGATIVE, "You do not own a home named {name#home}!", home);
            return;
        }
        if (h.isPublic())
        {
            sender.sendTranslated(NEGATIVE, "You can't invite a person to a public home.");
            return;
        }
        if (player.equals(sender))
        {
            sender.sendTranslated(NEGATIVE, "You cannot invite yourself to your own home!");
            return;
        }
        if (h.isInvited(player))
        {
            sender.sendTranslated(NEGATIVE, "{user} is already invited to your home!", player);
            return;
        }
        h.invite(player);
        if (player.isOnline())
        {
            player.sendTranslated(NEUTRAL,
                                  "{user} invited you to their home. To teleport to it use: /home {name#home} {name}",
                                  sender, h.getName(), sender.getName());
        }
        sender.sendTranslated(POSITIVE, "{user} is now invited to your home {name}", player, h.getName());
    }

    @Restricted(User.class)
    @Command(desc = "Uninvite a player from one of your homes")
    public void unInvite(User sender, User player, @Optional String home )
    {
        home = home == null ? "home" : home;
        Home h = this.manager.getExact(sender, home);
        if (h == null || !h.isOwnedBy(sender))
        {
            sender.sendTranslated(NEGATIVE, "You do not own a home named {name#home}!", home);
            return;
        }
        if (h.isPublic())
        {
            sender.sendTranslated(NEGATIVE, "This home is public. Make it private to disallow others to access it.");
            return;
        }
        if (player.equals(sender))
        {
            sender.sendTranslated(NEGATIVE, "You cannot uninvite yourself from your own home!");
            return;
        }
        if (!h.isInvited(player))
        {
            sender.sendTranslated(NEGATIVE, "{user} is not invited to your home!", player);
            return;
        }
        h.unInvite(player);
        if (player.isOnline())
        {
            player.sendTranslated(NEUTRAL, "You are no longer invited to {user}'s home {name#home}", sender, h.getName());
        }
        sender.sendTranslated(POSITIVE, "{user} is no longer invited to your home {name}", player, h.getName());
    }

    @Command(name = "private", alias = {"makeprivate", "setprivate"}, desc = "Make one of your homes private")
    public void makePrivate(CommandSender sender, @Optional String home, @Default User owner)
    {
        if (!owner.equals(sender) && !module.getPermissions().HOME_PRIVATE_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().HOME_PRIVATE_OTHER);
        }
        home = home == null ? "home" : home;
        Home h = this.manager.findOne(owner, home);
        if (h == null)
        {
            homeNotFoundMessage(sender, owner, home);
            return;
        }
        if (!h.isPublic())
        {
            sender.sendTranslated(NEGATIVE, "This home is already private!");
            return;
        }
        h.setVisibility(PRIVATE);
        if (h.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "Your home {name} is now private", h.getName());
            return;
        }
        sender.sendTranslated(POSITIVE, "The home {name} of {user} is now private", h.getOwnerName(), h.getName());
    }

    @Command(name = "public", alias = {"makepublic", "setpublic"}, desc = "Make one of your homes public")
    public void makePublic(CommandSender sender, @Optional String home, @Default User owner)
    {
        if (!owner.equals(sender) && !module.getPermissions().HOME_PUBLIC_OTHER.isAuthorized(sender))
        {
            throw new PermissionDeniedException(module.getPermissions().HOME_PUBLIC_OTHER);
        }
        home = home == null ? "home" : home;
        Home h = this.manager.findOne(owner, home);
        if (h == null)
        {
            homeNotFoundMessage(sender, owner, home);
            return;
        }
        if (h.isPublic())
        {
            sender.sendTranslated(NEGATIVE, "This home is already public!");
            return;
        }
        h.setVisibility(PUBLIC);
        if (h.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "Your home {name} is now public", h.getName());
            return;
        }
        sender.sendTranslated(POSITIVE, "The home {name} of {user} is now public", h.getOwnerName(), h.getName());
    }

    @Alias(value = {"clearhomes"})
    @Command(desc = "Clear all homes (of an user)")
    public CommandResult clear(final CommandContext sender, @Optional final User owner,
                               @Flag(name = "pub", longName = "public") final boolean isPublic,
                               @Flag(name = "priv", longName = "private") final boolean isPrivate,
                               @Flag(name = "sel", longName = "selection") final boolean selection)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(sender.getSource() instanceof ConsoleCommandSender))
        {
            sender.sendTranslated(NEGATIVE, "This command has been disabled for ingame use via the configuration");
            return null;
        }
        String type = "";
        if (isPublic)
        {
            type = module.getCore().getI18n().translate(sender.getSource().getLocale(), "public");
            type += " ";
        }
        else if (isPrivate)
        {
            type = module.getCore().getI18n().translate(sender.getSource().getLocale(), "private");
            type += " ";
        }
        final Location firstPoint;
        final Location secondPoint;
        if (selection)
        {
            if (!module.getCore().getModuleManager().getServiceManager().isImplemented(Selector.class))
            {
                sender.sendTranslated(NEGATIVE, "You need to use the Selector module to delete homes in a selection!");
                return null;
            }
            if (!sender.isSource(User.class))
            {
                sender.sendTranslated(NEGATIVE, "You have to be in game to use the selection flag");
                return null;
            }
            Selector selector = module.getCore().getModuleManager().getServiceManager().getServiceImplementation(Selector.class);
            Shape shape = selector.getSelection((User)sender.getSource());
            if (!(shape instanceof Cuboid))
            {
                sender.sendTranslated(NEGATIVE, "Invalid selection!");
                return null;
            }
            firstPoint = selector.getFirstPoint((User)sender.getSource());
            secondPoint = selector.getSecondPoint((User)sender.getSource());
            if (owner != null)
            {
                sender.sendTranslated(NEUTRAL, "Are you sure you want to delete all {input#public|private}homes created by {user} in your current selection?", type, owner);
            }
            else
            {
                sender.sendTranslated(NEUTRAL, "Are you sure you want to delete all {input#public|private}homes created in your current selection?", type);
            }
        }
        else
        {
            firstPoint = null;
            secondPoint = null;
            if (owner != null)
            {
                sender.sendTranslated(NEUTRAL, "Are you sure you want to delete all {input#public|private}homes ever created by {user}?", type, owner);
            }
            else
            {
                sender.sendTranslated(NEUTRAL, "Are you sure you want to delete all {input#public|private}homes ever created on this server?", type);
            }
        }
        sender.sendTranslated(NEUTRAL, "Confirm with: {text:/confirm} before 30 seconds have passed to delete the homes");
        return new ConfirmResult(module, () -> {
            if (selection)
            {
                manager.massDelete(owner, isPrivate, isPublic, firstPoint, secondPoint);
                if (sender.hasPositional(0))
                {
                    sender.sendTranslated(POSITIVE, "The homes of {user} in the selection are now deleted", owner);
                    return;
                }
                sender.sendTranslated(POSITIVE, "The homes in the selection are now deleted.");
                return;
            }
            manager.massDelete(owner, isPrivate, isPublic);
            if (sender.hasPositional(0))
            {
                sender.sendTranslated(POSITIVE, "The homes of {user} are now deleted", owner);
                return;
            }
            sender.sendTranslated(POSITIVE, "The homes are now deleted.");
        }, sender);
    }

    private void homeInDeletedWorldMessage(CommandSender sender, Home home)
    {
        if (home.isOwnedBy(sender))
        {
            sender.sendTranslated(NEGATIVE, "Your home {name} is in a world that no longer exists!", home.getName());
        }
        else
        {
            sender.sendTranslated(NEGATIVE, "The home {name} of {user} is in a world that no longer exists!", home.getName(), home.getOwnerName());
        }
    }

    private void homeNotFoundMessage(CommandSender sender, User user, String name)
    {
        if (sender.equals(user))
        {
            sender.sendTranslated(NEGATIVE, "You have no home named {name#home}!", name);
        }
        else
        {
            sender.sendTranslated(NEGATIVE, "{user} has no home named {name#home}!", user, name);
        }
    }
}
