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
package org.cubeengine.module.travel.home;

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
import de.cubeisland.engine.modularity.core.Maybe;
import org.cubeengine.module.travel.TpPointCommand;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.storage.TeleportInvite;
import org.cubeengine.module.travel.storage.TeleportPointModel.Visibility;
import org.cubeengine.service.command.property.RawPermission;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.annotation.ParameterPermission;
import org.cubeengine.service.command.exception.PermissionDeniedException;
import org.cubeengine.service.Selector;
import org.cubeengine.service.confirm.ConfirmResult;
import org.cubeengine.service.user.MultilingualCommandSource;
import org.cubeengine.service.user.MultilingualPlayer;
import org.cubeengine.module.core.util.math.Cuboid;
import org.cubeengine.module.core.util.math.shape.Shape;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldManager;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.world.Location;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.module.travel.storage.TableInvite.TABLE_INVITE;
import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static java.util.stream.Collectors.toSet;

@Command(name = "home", desc = "Teleport to your home")
public class HomeCommand extends TpPointCommand
{
    private final HomeManager manager;
    private final Travel module;
    private Maybe<Selector> selector;
    private I18n i18n;
    private UserManager um;
    private WorldManager wm;

    public HomeCommand(Travel module, Maybe<Selector> selector, I18n i18n, UserManager um, WorldManager wm)
    {
        super(module);
        this.module = module;
        this.selector = selector;
        this.i18n = i18n;
        this.um = um;
        this.wm = wm;
        this.manager = module.getHomeManager();
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof MultilingualPlayer)
        {
            return getCommand("tp").execute(invocation);
        }
        return super.selfExecute(invocation);
    }

    @Restricted(MultilingualPlayer.class)
    @Command(desc = "Teleport to a home")
    public void tp(MultilingualPlayer sender, @Optional String home, @Default MultilingualPlayer owner)
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
            if (!sender.hasPermission(module.getPermissions().HOME_TP_OTHER.getId()))
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
        sender.original().setLocation(location);
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
    @Restricted(value = MultilingualPlayer.class, msg = "Ok so I'll need your new address then. No seriously this won't work!")
    public void set(CommandContext context, @Optional String name, @ParameterPermission @Flag(longName = "public", name = "pub") boolean isPublic)
    {
        MultilingualPlayer sender = (MultilingualPlayer)context.getSource();
        if (this.manager.getCount(sender) >= this.module.getConfig().homes.max
            && !context.getSource().hasPermission(module.getPermissions().HOME_SET_MORE.getId()))
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
        Home home = this.manager.create(sender, name, sender.original().getLocation(), sender.original().getRotation(), isPublic);
        context.sendTranslated(POSITIVE, "Your home {name} has been created!", home.getName());
    }

    @Command(desc = "Set the welcome message of homes", alias = {"setgreeting", "setwelcome", "setwelcomemsg"})
    public void greeting(MultilingualCommandSource sender,
                         String home,
                         @Optional @Label("welcome message") @Greed(INFINITE) String message,
                         @Default @Named("owner") MultilingualPlayer owner,
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
        sender.getSource().sendMessage(Texts.of(h.getWelcomeMsg()));
    }

    @Restricted(value = MultilingualPlayer.class, msg = "I am calling the moving company right now!")
    @Command(alias = "replace", desc = "Move a home")
    public void move(MultilingualPlayer sender, @Optional String name, @Default MultilingualPlayer owner)
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
            if (!sender.hasPermission(module.getPermissions().HOME_MOVE_OTHER.getId()))
            {
                throw new PermissionDeniedException(module.getPermissions().HOME_MOVE_OTHER);
            }
        }
        home.setLocation(sender.original().getLocation(), sender.original().getRotation(), wm);
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
    public void remove(MultilingualCommandSource sender, @Optional String name, @Default @Optional MultilingualPlayer owner)
    {
        name = name == null ? "home" : name;
        Home home = this.manager.getExact(owner, name);
        if (home == null)
        {
            homeNotFoundMessage(sender, owner, name);
            return;
        }
        if (!home.isOwnedBy(sender) && !sender.hasPermission(module.getPermissions().HOME_REMOVE_OTHER.getId()))
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
    public void rename(MultilingualCommandSource sender, String name, @Label("new name") String newName, @Default @Optional MultilingualPlayer owner)
    {
        Home home = manager.getExact(owner, name);
        if (home == null)
        {
            homeNotFoundMessage(sender, owner, name);
            return;
        }
        if (!home.isOwnedBy(sender) && !sender.hasPermission(module.getPermissions().HOME_RENAME_OTHER.getId()))
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
    public void list(MultilingualCommandSource sender, @Default MultilingualPlayer owner,
                     @Flag(name = "pub", longName = "public") boolean isPublic,
                     @Flag boolean owned,
                     @Flag boolean invited) throws Exception
    {
        if (!owner.equals(sender.getSource()))
        {
            PermissionDescription otherPerm = module.getPermissions().HOME_LIST_OTHER;
            if (!sender.hasPermission(otherPerm.getId()))
            {
                throw new PermissionDeniedException(new RawPermission(otherPerm.getId(), Texts.toPlain(otherPerm.getDescription())));
            }
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
            sender.sendTranslated(NEUTRAL, "The following homes are available to {user}:", owner);
        }
        showList(sender, owner, homes);
    }

    @Command(alias = "listhomes", desc = "Lists all homes")
    public void listAll(MultilingualCommandSource sender)
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
    public void invitedList(MultilingualCommandSource sender, @Default MultilingualPlayer owner)
    {
        if (!owner.equals(sender) && !sender.hasPermission(module.getPermissions().HOME_LIST_OTHER.getId()))
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
                sender.getSource().sendMessage(Texts.of(TextColors.GOLD, "  ", home.getName(), ":"));
                for (TeleportInvite invite : invites)
                {
                    sender.getSource().sendMessage(Texts.of("    ", TextColors.DARK_GREEN, um.getById(invite.getValue(
                        TABLE_INVITE.USERKEY)).get().getUser().getName()));
                }
            }
        }
    }

    @Restricted(value = MultilingualPlayer.class, msg = "How about making a phone call to invite someone instead?")
    @Command(desc = "Invite a user to one of your homes")
    public void invite(MultilingualPlayer sender, MultilingualPlayer player, @Optional String home)
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
        h.invite(player.getSource());
        player.sendTranslated(NEUTRAL, "{user} invited you to their home. To teleport to it use: /home {name#home} {name}", sender, h.getName(), sender.getName());
        sender.sendTranslated(POSITIVE, "{user} is now invited to your home {name}", player, h.getName());
    }

    @Restricted(MultilingualPlayer.class)
    @Command(desc = "Uninvite a player from one of your homes")
    public void unInvite(MultilingualPlayer sender, MultilingualPlayer player, @Optional String home )
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
        player.sendTranslated(NEUTRAL, "You are no longer invited to {user}'s home {name#home}", sender, h.getName());
        sender.sendTranslated(POSITIVE, "{user} is no longer invited to your home {name}", player, h.getName());
    }

    @Command(name = "private", alias = {"makeprivate", "setprivate"}, desc = "Make one of your homes private")
    public void makePrivate(MultilingualCommandSource sender, @Optional String home, @Default MultilingualPlayer owner)
    {
        if (!owner.equals(sender) && !sender.hasPermission(module.getPermissions().HOME_PRIVATE_OTHER.getId()))
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
        h.setVisibility(Visibility.PRIVATE);
        if (h.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "Your home {name} is now private", h.getName());
            return;
        }
        sender.sendTranslated(POSITIVE, "The home {name} of {user} is now private", h.getOwnerName(), h.getName());
    }

    @Command(name = "public", alias = {"makepublic", "setpublic"}, desc = "Make one of your homes public")
    public void makePublic(MultilingualCommandSource sender, @Optional String home, @Default MultilingualPlayer owner)
    {
        if (!owner.equals(sender) && !sender.hasPermission(module.getPermissions().HOME_PUBLIC_OTHER.getId()))
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
        h.setVisibility(Visibility.PUBLIC);
        if (h.isOwnedBy(sender))
        {
            sender.sendTranslated(POSITIVE, "Your home {name} is now public", h.getName());
            return;
        }
        sender.sendTranslated(POSITIVE, "The home {name} of {user} is now public", h.getOwnerName(), h.getName());
    }

    @Alias(value = {"clearhomes"})
    @Command(desc = "Clear all homes (of an user)")
    public CommandResult clear(final CommandContext context, @Optional final MultilingualPlayer owner,
                               @Flag(name = "pub", longName = "public") final boolean isPublic,
                               @Flag(name = "priv", longName = "private") final boolean isPrivate,
                               @Flag(name = "sel", longName = "selection") final boolean selection)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(context.getSource() instanceof ConsoleSource))
        {
            context.sendTranslated(NEGATIVE, "This command has been disabled for ingame use via the configuration");
            return null;
        }
        String type = "";
        if (isPublic)
        {
            type = i18n.translate(context.getLocale(), "public");
            type += " ";
        }
        else if (isPrivate)
        {
            type = i18n.translate(context.getLocale(), "private");
            type += " ";
        }
        final Location firstPoint;
        final Location secondPoint;
        if (selection)
        {
            if (selector.isAvailable())
            {
                context.sendTranslated(NEGATIVE, "You need to use the Selector module to delete homes in a selection!");
                return null;
            }
            if (!context.isSource(MultilingualPlayer.class))
            {
                context.sendTranslated(NEGATIVE, "You have to be in game to use the selection flag");
                return null;
            }
            Selector selector = this.selector.value();
            Shape shape = selector.getSelection((MultilingualPlayer)context.getSource());
            if (!(shape instanceof Cuboid))
            {
                context.sendTranslated(NEGATIVE, "Invalid selection!");
                return null;
            }
            firstPoint = selector.getFirstPoint((MultilingualPlayer)context.getSource());
            secondPoint = selector.getSecondPoint((MultilingualPlayer)context.getSource());
            if (owner != null)
            {
                context.sendTranslated(NEUTRAL,
                                      "Are you sure you want to delete all {input#public|private}homes created by {user} in your current selection?",
                                      type, owner);
            }
            else
            {
                context.sendTranslated(NEUTRAL,
                                      "Are you sure you want to delete all {input#public|private}homes created in your current selection?",
                                      type);
            }
        }
        else
        {
            firstPoint = null;
            secondPoint = null;
            if (owner != null)
            {
                context.sendTranslated(NEUTRAL,
                                      "Are you sure you want to delete all {input#public|private}homes ever created by {user}?",
                                      type, owner);
            }
            else
            {
                context.sendTranslated(NEUTRAL,
                                      "Are you sure you want to delete all {input#public|private}homes ever created on this server?",
                                      type);
            }
        }
        context.sendTranslated(NEUTRAL,
                              "Confirm with: {text:/confirm} before 30 seconds have passed to delete the homes");
        return new ConfirmResult(module, () -> {
            if (selection)
            {
                manager.massDelete(owner, isPrivate, isPublic, firstPoint, secondPoint);
                if (context.hasPositional(0))
                {
                    context.sendTranslated(POSITIVE, "The homes of {user} in the selection are now deleted", owner);
                    return;
                }
                context.sendTranslated(POSITIVE, "The homes in the selection are now deleted.");
                return;
            }
            manager.massDelete(owner, isPrivate, isPublic);
            if (context.hasPositional(0))
            {
                context.sendTranslated(POSITIVE, "The homes of {user} are now deleted", owner);
                return;
            }
            context.sendTranslated(POSITIVE, "The homes are now deleted.");
        }, context);
    }

    private void homeInDeletedWorldMessage(MultilingualCommandSource sender, Home home)
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

    private void homeNotFoundMessage(MultilingualCommandSource sender, MultilingualPlayer user, String name)
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
