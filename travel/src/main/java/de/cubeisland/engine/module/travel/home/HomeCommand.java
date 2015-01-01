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

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;

import de.cubeisland.engine.command.CommandInvocation;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.command.methodic.parametric.Greed;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.command.methodic.parametric.Named;
import de.cubeisland.engine.command.methodic.parametric.Optional;
import de.cubeisland.engine.command.result.CommandResult;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.annotation.ParameterPermission;
import de.cubeisland.engine.core.command.result.confirm.ConfirmResult;
import de.cubeisland.engine.core.command.sender.ConsoleCommandSender;
import de.cubeisland.engine.core.module.service.Selector;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.math.Cuboid;
import de.cubeisland.engine.core.util.math.shape.Shape;
import de.cubeisland.engine.module.travel.TpPointCommand;
import de.cubeisland.engine.module.travel.Travel;
import de.cubeisland.engine.module.travel.storage.TeleportInvite;

import static de.cubeisland.engine.command.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.core.util.ChatFormat.DARK_GREEN;
import static de.cubeisland.engine.core.util.ChatFormat.YELLOW;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.travel.storage.TableInvite.TABLE_INVITE;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PRIVATE;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PUBLIC;
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
    public void tp(CommandContext context, @Optional @Label("home") String name, @Default @Label("owner") User owner)
    {
        User sender = (User)context.getSource();
        name = name == null ? "home" : name;
        Home home = this.manager.findOne(owner, name);
        if (home == null)
        {
            homeNotFoundMessage(context, owner, name);
            context.sendTranslated(NEUTRAL, "Use {text:/sethome} to set your home");
            return;
        }
        if (!home.canAccess(sender))
        {
            if (owner.equals(sender))
            {
                homeNotFoundMessage(context, owner, name);
                return;
            }
            context.ensurePermission(module.getPermissions().HOME_TP_OTHER);
        }
        Location location = home.getLocation();
        if (location == null)
        {
            homeInDeletedWorldMessage(context, owner, home);
            return;
        }
        if (!sender.teleport(location, COMMAND))
        {
            context.sendTranslated(CRITICAL, "The teleportation got aborted!");
            return;
        }
        if (home.getWelcomeMsg() != null)
        {
            context.sendMessage(home.getWelcomeMsg());
            return;
        }
        if (home.isOwner(sender))
        {
            context.sendTranslated(POSITIVE, "You have been teleported to your home {name}!", home.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "You have been teleported to the home {name} of {user}!", home.getName(), home.getOwnerName());
    }

    @Alias("sethome")
    @Command(alias = {"create", "sethome", "createhome"}, desc = "Set your home")
    @Restricted(value = User.class, msg = "Ok so I'll need your new address then. No seriously this won't work!")
    public void set(CommandContext context, @Optional @Label("name") String name, @ParameterPermission @Flag(longName = "public", name = "pub") boolean isPublic)
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
    public void greeting(CommandContext context,
                         @Label("home") String name,
                         @Optional @Label("welcome message") @Greed(INFINITE) String message,
                         @Default @Named("owner") @Label("owner") User owner,
                         @Flag(longName = "append", name = "a") boolean append)
    {
        Home home = this.manager.getExact(owner, name);
        if (home == null)
        {
            homeNotFoundMessage(context, owner, name);
            return;
        }
        if (append)
        {
            home.setWelcomeMsg(home.getWelcomeMsg() + message);
        }
        else
        {
            home.setWelcomeMsg(message);
        }
        home.update();
        if (home.isOwner(context.getSource()))
        {
            context.sendTranslated(POSITIVE, "The welcome message for your home {name} is now set to:", home.getName());
        }
        else
        {
            context.sendTranslated(POSITIVE, "The welcome message for the home {name} of {user} is now set to:", home.getName(), owner);
        }
        context.sendMessage(home.getWelcomeMsg());
    }

    @Restricted(value = User.class, msg = "I am calling the moving company right now!")
    @Command(alias = "replace", desc = "Move a home")
    public void move(CommandContext context, @Label("name") @Optional String name, @Default @Label("owner") User owner)
    {
        name = name == null ? "home" : name;
        Home home = this.manager.getExact(owner, name);
        if (home == null)
        {
            homeNotFoundMessage(context, owner, name);
            context.sendTranslated(NEUTRAL, "Use {text:/sethome} to set your home");
            return;
        }
        if (!home.isOwner(context.getSource()))
        {
            context.ensurePermission(module.getPermissions().HOME_MOVE_OTHER);
        }
        User sender = (User)context.getSource();
        home.setLocation(sender.getLocation());
        home.update();
        if (home.isOwner(sender))
        {
            context.sendTranslated(POSITIVE, "Your home {name} has been moved to your current location!", home.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "The home {name} of {user} has been moved to your current location", home.getName(), owner);
    }

    @Alias(value = {"remhome", "removehome", "delhome", "deletehome"})
    @Command(alias = {"delete", "rem", "del"}, desc = "Remove a home")
    public void remove(CommandContext context, @Label("name") @Optional String name, @Default @Label("owner") @Optional User owner)
    {
        Home home = this.manager.getExact(owner, name);
        if (home == null)
        {
            homeNotFoundMessage(context, owner, name);
            return;
        }
        if (!home.isOwner(context.getSource()))
        {
            context.ensurePermission(module.getPermissions().HOME_REMOVE_OTHER);
        }
        this.manager.delete(home);
        if (owner.equals(context.getSource()))
        {
            context.sendTranslated(POSITIVE, "Your home {name} has been removed!", name);
            return;
        }
        context.sendTranslated(POSITIVE, "The home {name} of {user} has been removed", name, owner);
    }

    @Command(desc = "Rename a home")
    public void rename(CommandContext context,
                       @Label("name") @Optional String name,
                       @Label("new name") @Optional String newName,
                       @Default @Label("owner") @Optional User owner)
    {
        Home home = manager.getExact(owner, name);
        if (home == null)
        {
            homeNotFoundMessage(context, owner, name);
            return;
        }
        if (!home.isOwner(context.getSource()))
        {
            context.ensurePermission(module.getPermissions().HOME_RENAME_OTHER);
        }
        if (name.contains(":") || name.length() >= 32)
        {
            context.sendTranslated(NEGATIVE, "Homes may not have names that are longer than 32 characters or contain colon(:)'s!");
            return;
        }
        String oldName = home.getName();
        if (!manager.rename(home, newName))
        {
            context.sendTranslated(POSITIVE, "Could not rename the home to {name}", newName);
            return;
        }
        if (home.isOwner(context.getSource()))
        {
            context.sendTranslated(POSITIVE, "Your home {name} has been renamed to {name}", oldName, newName);
            return;
        }
        context.sendTranslated(POSITIVE, "The home {name} of {user} has been renamed to {name}", oldName, owner, newName);
    }

    @Alias(value = {"listhomes", "homes"})
    @Command(alias = "listhomes", desc = "Lists homes a player can access")
    public void list(CommandContext context, @Default @Label("owner") User user,
                     @Flag(name = "pub", longName = "public") boolean isPublic,
                     @Flag(name = "o", longName = "owned") boolean owned,
                     @Flag(name = "i", longName = "invited") boolean invited) throws Exception
    {
        if (!user.equals(context.getSource()))
        {
            context.ensurePermission(module.getPermissions().HOME_LIST_OTHER);
        }
        Set<Home> homes = this.manager.list(user, owned, isPublic, invited);
        if (homes.isEmpty())
        {
            if (user.equals(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "No homes are available to you!");
                return;
            }
            context.sendTranslated(NEGATIVE, "No homes are available to {user}!", user);
            return;
        }
        if (user.equals(context.getSource()))
        {
            context.sendTranslated(NEUTRAL, "The following homes are available to you:");
        }
        else
        {
            context.sendTranslated(NEUTRAL, "The following homes are available to {user}:", user.getDisplayName());
        }
        showList(context, user, homes);
    }

    @Command(alias = "listhomes", desc = "Lists all homes")
    public void listAll(CommandContext context)
    {
        int count = this.manager.getCount();
        if (count == 0)
        {
            context.sendTranslated(POSITIVE, "There are no homes set.");
            return;
        }
        context.sendTranslatedN(POSITIVE, count, "There is one home set:", "There are {amount} homes set:", count);
        this.showList(context, null, this.manager.list(true, true));
    }

    @Command(alias = {"ilist", "invited"}, desc = "List all players invited to your homes")
    public void invitedList(CommandContext context, @Default @Label("owner") User user)
    {
        if (!user.equals(context.getSource()))
        {
            context.ensurePermission(module.getPermissions().HOME_LIST_OTHER);// TODO permission "other"
        }
        Set<Home> homes = new HashSet<>();
        for (Home home : this.manager.list(user, true, false, false))
        {
            if (!home.getInvited().isEmpty())
            {
                homes.add(home);
            }
        }
        if (homes.isEmpty())
        {
            if (user.equals(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You have no homes with players invited to them!");
                return;
            }
            context.sendTranslated(NEGATIVE, "{user} has no homes with players invited to them!", user);
            return;
        }
        if (user.equals(context.getSource()))
        {
            context.sendTranslated(NEUTRAL, "Your following homes have players invited to them:");
        }
        else
        {
            context.sendTranslated(NEUTRAL, "The following homes of {user} have players invited to them:", user);
        }
        for (Home home : homes)
        {
            Set<TeleportInvite> invites = this.iManager.getInvites(home.getModel());
            if (!invites.isEmpty())
            {
                context.sendMessage(YELLOW + "  " + home.getName() + ":");
                for (TeleportInvite invite : invites)
                {
                    context.sendMessage("    " + DARK_GREEN + this.module.getCore().getUserManager().getUser(invite.getValue(TABLE_INVITE.USERKEY)).getDisplayName());
                }
            }
        }
    }

    @Restricted(value = User.class, msg = "How about making a phone call to invite someone instead?")
    @Command(desc = "Invite a user to one of your homes")
    public void invite(CommandContext context, @Label("player") User invited, @Label("home") @Optional String name)
    {
        User sender = (User)context.getSource();
        name = name == null ? "home" : name;
        Home home = this.manager.getExact(sender, name);
        if (home == null || !home.isOwner(sender))
        {
            context.sendTranslated(NEGATIVE, "You do not own a home named {name#home}!", name);
            return;
        }
        if (home.isPublic())
        {
            context.sendTranslated(NEGATIVE, "You can't invite a person to a public home.");
            return;
        }
        if (invited.equals(sender))
        {
            context.sendTranslated(NEGATIVE, "You cannot invite yourself to your own home!");
            return;
        }
        if (home.isInvited(invited))
        {
            context.sendTranslated(NEGATIVE, "{user} is already invited to your home!", invited);
            return;
        }
        home.invite(invited);
        if (invited.isOnline())
        {
            invited.sendTranslated(NEUTRAL, "{user} invited you to their home. To teleport to it use: /home {name#home} {user}", sender, home.getName(), sender);
        }
        context.sendTranslated(POSITIVE, "{user} is now invited to your home {name}", invited, home.getName());
    }

    @Restricted(User.class)
    @Command(desc = "Uninvite a player from one of your homes")
    public void unInvite(CommandContext context, @Label("player") User invited, @Label("home") @Optional String name )
    {
        User sender = (User)context.getSource();
        name = name == null ? "home" : name;
        Home home = this.manager.getExact(sender, name);
        if (home == null || !home.isOwner(sender))
        {
            context.sendTranslated(NEGATIVE, "You do not own a home named {name#home}!", name);
            return;
        }
        if (home.isPublic())
        {
            context.sendTranslated(NEGATIVE, "This home is public. Make it private to disallow others to access it.");
            return;
        }
        if (invited.equals(sender))
        {
            context.sendTranslated(NEGATIVE, "You cannot uninvite yourself from your own home!");
            return;
        }
        if (!home.isInvited(invited))
        {
            context.sendTranslated(NEGATIVE, "{user} is not invited to your home!", invited);
            return;
        }
        home.unInvite(invited);
        if (invited.isOnline())
        {
            invited.sendTranslated(NEUTRAL, "You are no longer invited to {user}'s home {name#home}", sender, home.getName());
        }
        context.sendTranslated(POSITIVE, "{user} is no longer invited to your home {name}", invited, home.getName());
    }

    @Command(name = "private", alias = {"makeprivate", "setprivate"}, desc = "Make one of your homes private")
    public void makePrivate(CommandContext context, @Optional @Label("home") String name, @Default @Label("owner") User user)
    {
        if (!user.equals(context.getSource()))
        {
            context.ensurePermission(module.getPermissions().HOME_PRIVATE_OTHER);
        }
        name = name == null ? "home" : name;
        Home home = this.manager.findOne(user, name);
        if (home == null)
        {
            homeNotFoundMessage(context, user, name);
            return;
        }
        if (!home.isPublic())
        {
            context.sendTranslated(NEGATIVE, "This home is already private!");
            return;
        }
        home.setVisibility(PRIVATE);
        if (home.isOwner(context.getSource()))
        {
            context.sendTranslated(POSITIVE, "Your home {name} is now private", home.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "The home {name} of {user} is now private", home.getOwnerName(), home.getName());
    }

    @Command(name = "public", alias = {"makepublic", "setpublic"}, desc = "Make one of your homes public")
    public void makePublic(CommandContext context, @Optional @Label("home") String name, @Default @Label("owner") User user)
    {
        if (!user.equals(context.getSource()))
        {
            context.ensurePermission(module.getPermissions().HOME_PUBLIC_OTHER);
        }
        name = name == null ? "home" : name;
        Home home = this.manager.findOne(user, name);
        if (home == null)
        {
            homeNotFoundMessage(context, user, name);
            return;
        }
        if (home.isPublic())
        {
            context.sendTranslated(NEGATIVE, "This home is already public!");
            return;
        }
        home.setVisibility(PUBLIC);
        if (home.isOwner(context.getSource()))
        {
            context.sendTranslated(POSITIVE, "Your home {name} is now public", home.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "The home {name} of {user} is now public", home.getOwnerName(), home.getName());
    }

    @Alias(value = {"clearhomes"})
    @Command(desc = "Clear all homes (of an user)")
    public CommandResult clear(final CommandContext context, final @Label("owner") User user,
                               @Flag(name = "pub", longName = "public") final boolean isPublic,
                               @Flag(name = "priv", longName = "private") final boolean isPrivate,
                               @Flag(name = "sel", longName = "selection") final boolean isSelection)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(context.getSource() instanceof ConsoleCommandSender))
        {
            context.sendTranslated(NEGATIVE, "This command has been disabled for ingame use via the configuration");
            return null;
        }
        String type = "";
        if (isPublic)
        {
            type = context.getCore().getI18n().translate(context.getSource().getLocale(), "public");
            type += " ";
        }
        else if (isPrivate)
        {
            type = context.getCore().getI18n().translate(context.getSource().getLocale(), "private");
            type += " ";
        }
        final Location firstPoint;
        final Location secondPoint;
        if (isSelection)
        {
            if (!context.getCore().getModuleManager().getServiceManager().isImplemented(Selector.class))
            {
                context.sendTranslated(NEGATIVE, "You need to use the Selector module to delete homes in a selection!");
                return null;
            }
            if (!context.isSource(User.class))
            {
                context.sendTranslated(NEGATIVE, "You have to be in game to use the selection flag");
                return null;
            }
            Selector selector = context.getCore().getModuleManager().getServiceManager().getServiceImplementation(Selector.class);
            Shape selection = selector.getSelection((User)context.getSource());
            if (!(selection instanceof Cuboid))
            {
                context.sendTranslated(NEGATIVE, "Invalid selection!");
                return null;
            }
            firstPoint = selector.getFirstPoint((User)context.getSource());
            secondPoint = selector.getSecondPoint((User)context.getSource());
            if (context.hasPositional(0))
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all {input#public|private}homes created by {user} in your current selection?", type, user);
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all {input#public|private}homes created in your current selection?", type);
            }
        }
        else
        {
            firstPoint = null;
            secondPoint = null;
            if (context.hasPositional(0))
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all {input#public|private}homes ever created by {user}?", type, user);
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all {input#public|private}homes ever created on this server?", type);
            }
        }
        context.sendTranslated(NEUTRAL, "Confirm with: {text:/confirm} before 30 seconds have passed to delete the homes");
        return new ConfirmResult(new Runnable()
        {
            @Override
            public void run()
            {
                if (isSelection)
                {
                    manager.massDelete(user, isPrivate, isPublic, firstPoint, secondPoint);
                    if (context.hasPositional(0))
                    {
                        context.sendTranslated(POSITIVE, "The homes of {user} in the selection are now deleted", user);
                        return;
                    }
                    context.sendTranslated(POSITIVE, "The homes in the selection are now deleted.");
                    return;
                }
                manager.massDelete(user, isPrivate, isPublic);
                if (context.hasPositional(0))
                {
                    context.sendTranslated(POSITIVE, "The homes of {user} are now deleted", user);
                    return;
                }
                context.sendTranslated(POSITIVE, "The homes are now deleted.");
            }
        }, context);
    }

    private void homeInDeletedWorldMessage(CommandContext context, User user, Home home)
    {
        if (home.isOwner(user))
        {
            context.sendTranslated(NEGATIVE, "Your home {name} is in a world that no longer exists!", home.getName());
            return;
        }
        context.sendTranslated(NEGATIVE, "The home {name} of {user} is in a world that no longer exists!", home.getName(), home.getOwnerName());
    }

    private void homeNotFoundMessage(CommandContext context, User user, String name)
    {
        if (context.getSource().equals(user))
        {
            context.sendTranslated(NEGATIVE, "You have no home named {name#home}!", name);
            return;
        }
        context.sendTranslated(NEGATIVE, "{user} has no home named {name#home}!", user, name);
    }
}
