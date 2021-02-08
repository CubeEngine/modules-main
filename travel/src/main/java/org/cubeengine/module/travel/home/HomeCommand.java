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
package org.cubeengine.module.travel.home;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Greedy;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Parser;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.util.ConfirmManager;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.TravelPerm;
import org.cubeengine.module.travel.config.Home;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import static java.util.stream.Collectors.toSet;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
@Command(name = "home", desc = "Teleport to your home")
public class HomeCommand extends DispatcherCommand
{
    private final HomeManager manager;
    private TravelPerm perms;
    private final Travel module;
    @Inject(optional = true) private Selector selector;
    private I18n i18n;

    @Inject
    public HomeCommand(Travel module, I18n i18n, HomeManager manager, TravelPerm perms)
    {
        super(Travel.class);
        this.module = module;
        this.i18n = i18n;
        this.manager = manager;
        this.perms = perms;
    }

    @Restricted
    @Command(name = "tp", desc = "Teleport to a home", dispatcher = true)
    public void dispatcher(ServerPlayer sender, @Parser(completer = HomeCompleter.class) @Option String home)
    {
        this.tp(sender, home, sender.getUser());
    }

    @Restricted
    @Command(desc = "Teleport to a home")
    public void tp(ServerPlayer sender, @Parser(completer = HomeCompleter.class) @Option String home, @Default User owner)
    {
        home = home == null ? "home" : home;
        // TODO find close match and display as click cmd
        Home h = this.manager.find(sender, home, owner).orElse(null);
        if (h == null)
        {
            homeNotFoundMessage(sender, owner, home);
            i18n.send(sender, NEUTRAL, "Use {text:/sethome} to set your home"); // TODO create on click
            return;
        }
        if (!h.isInvited(sender.getUser()) && !owner.getUniqueId().equals(sender.getUniqueId()))
        {
            if (!perms.HOME_TP_OTHER.check(sender, i18n))
            {
                return;
            }
        }
        Transform transform = h.transform;
        sender.remove(Keys.VEHICLE);
        sender.setLocationAndRotation(h.world.getWorld().getLocation(transform.getPosition()), transform.getRotation());
        if (h.welcomeMsg != null)
        {
            sender.sendMessage(Identity.nil(), Component.text(h.welcomeMsg));
            return;
        }
        if (h.owner.equals(sender.getUniqueId()))
        {
            i18n.send(ChatType.ACTION_BAR, sender, POSITIVE, "You have been teleported to your home {name}!", h.name);
            return;
        }
        i18n.send(ChatType.ACTION_BAR, sender, POSITIVE, "You have been teleported to the home {name} of {user}!", h.name, h.getOwner());
    }

    @Alias("sethome")
    @Command(alias = {"create", "sethome", "createhome"}, desc = "Set your home")
    @Restricted(msg = "Ok so I'll need your new address then. No seriously this won't work!")
    public void set(ServerPlayer context, @Option String name)
    {
        if (this.manager.getCount(context) >= this.module.getConfig().homes.max
            && !context.hasPermission(perms.HOME_SET_MORE.getId()))
        {
            i18n.send(context, NEGATIVE, "You have reached your maximum number of homes!");
            i18n.send(context, NEUTRAL, "You have to delete a home to make a new one");
            return;
        }
        name = name == null ? "home" : name;
        if (name.contains(":") || name.length() >= 32)
        {
            i18n.send(context, NEGATIVE, "Homes may not have names that are longer then 32 characters nor contain colon(:)'s!");
            return;
        }
        if (this.manager.has(context.getUniqueId(), name))
        {
            i18n.send(context, NEGATIVE, "The home already exists! You can move it with {text:/home move}");
            return;
        }
        Home home = this.manager.create(context.getUser(), name, context.getWorld(), context.getTransform());
        i18n.send(context, POSITIVE, "Your home {name} has been created!", home.name);
    }

    @Command(desc = "Set the welcome message of homes", alias = {"setgreeting", "setwelcome", "setwelcomemsg"})
    public void greeting(CommandCause sender,
                         @Parser(completer = HomeCompleter.class) String home,
                         @Option @Label("welcome message") @Greedy String message,
                         @Default @Named("owner") User owner,
                         @Flag boolean append)
    {
        Home h = this.manager.get(owner.getUniqueId(), home).orElse(null);
        if (h == null)
        {
            homeNotFoundMessage(sender.getAudience(), owner, home);
            return;
        }
        if (append)
        {
            h.welcomeMsg += message;
        }
        else
        {
            h.welcomeMsg = message;
        }
        manager.save();
        if (h.isOwner(sender))
        {
            i18n.send(sender, POSITIVE, "The welcome message for your home {name} is now set to:", h.name);
        }
        else
        {
            i18n.send(sender, POSITIVE, "The welcome message for the home {name} of {user} is now set to:", h.name, owner);
        }
        sender.sendMessage(Identity.nil(), Component.text(h.welcomeMsg));
    }

    @Restricted(msg = "I am calling the moving company right now!")
    @Command(alias = "replace", desc = "Move a home")
    public void move(ServerPlayer sender, @Parser(completer = HomeCompleter.class) @Option String name, @Default User owner)
    {
        name = name == null ? "home" : name;
        Home home = this.manager.get(owner.getUniqueId(), name).orElse(null);
        if (home == null)
        {
            homeNotFoundMessage(sender, owner, name);
            i18n.send(sender, NEUTRAL, "Use {text:/sethome} to set your home"); // TODO create on click
            return;
        }

        if (!home.isOwner(sender.getUser()) && !perms.HOME_MOVE_OTHER.check(sender, i18n))
        {
            return;
        }
        home.setTransform(sender.getWorld(), sender.getTransform());
        manager.save();
        if (home.owner.equals(sender.getUniqueId()))
        {
            i18n.send(sender, POSITIVE, "Your home {name} has been moved to your current location!", home.name);
            return;
        }
        i18n.send(sender, POSITIVE, "The home {name} of {user} has been moved to your current location", home.name, owner);
    }

    @Alias(value = "remhome", alias = {"removehome", "delhome", "deletehome"})
    @Command(alias = {"delete", "rem", "del"}, desc = "Remove a home")
    public void remove(CommandCause sender, @Parser(completer = HomeCompleter.class) @Option String name, @Default User owner)
    {
        name = name == null ? "home" : name;
        Home home = this.manager.get(owner.getUniqueId(), name).orElse(null);
        if (home == null)
        {
            homeNotFoundMessage(sender.getAudience(), owner, name);
            return;
        }

        if (!home.isOwner(sender) && !perms.HOME_REMOVE_OTHER.check(sender.getSubject(), sender.getAudience(), i18n))
        {
            return;
        }
        this.manager.delete(home);
        if (sender.getAudience() instanceof ServerPlayer && owner.getUniqueId().equals(((ServerPlayer)sender.getAudience()).getUniqueId()))
        {
            i18n.send(sender, POSITIVE, "Your home {name} has been removed!", name);
            return;
        }
        i18n.send(sender, POSITIVE, "The home {name} of {user} has been removed", name, owner);
    }

    @Command(desc = "Rename a home")
    public void rename(CommandCause sender, @Parser(completer = HomeCompleter.class) String home, @Label("new name") String newName, @Default @Option User owner)
    {
        Home h = manager.get(owner.getUniqueId(), home).orElse(null);
        if (h == null)
        {
            homeNotFoundMessage(sender.getAudience(), owner, home);
            return;
        }

        if (!h.isOwner(sender) && !perms.HOME_RENAME_OTHER.check(sender.getSubject(), sender.getAudience(), i18n))
        {
            return;
        }
        if (home.contains(":") || home.length() >= 32)
        {
            i18n.send(sender, NEGATIVE, "Homes may not have names that are longer than 32 characters or contain colon(:)'s!");
            return;
        }
        String oldName = h.name;

        if (!manager.rename(h, newName))
        {
            i18n.send(sender, POSITIVE, "Could not rename the home to {name}", newName);
            return;
        }
        if (h.isOwner(sender))
        {
            i18n.send(sender, POSITIVE, "Your home {name} has been renamed to {name}", oldName, newName);
            return;
        }
        i18n.send(sender, POSITIVE, "The home {name} of {user} has been renamed to {name}", oldName, owner, newName);
    }

    @Alias(value = "listhomes", alias = "homes")
    @Command(alias = "listhomes", desc = "Lists homes a player can access")
    public void list(CommandCause sender, @Default User owner,
                     @Flag boolean owned,
                     @Flag boolean invited) throws Exception
    {

        final boolean isOwner = sender.getAudience() instanceof ServerPlayer && owner.getUniqueId().equals(((ServerPlayer)sender.getAudience()).getUniqueId());
        if (!isOwner)
        {
            if (!perms.HOME_LIST_OTHER.check(sender.getSubject(), sender.getAudience(), i18n))
            {
                return;
            }
        }
        if (!owned && !invited)
        {
            owned = true;
            invited = true;
        }
        Set<Home> homes = this.manager.list(owner, owned, invited);
        if (homes.isEmpty())
        {
            if (isOwner)
            {
                i18n.send(sender, NEGATIVE, "No homes are available to you!");
                return;
            }
            i18n.send(sender, NEGATIVE, "No homes are available to {user}!", owner);
            return;
        }
        if (isOwner)
        {
            i18n.send(sender, NEUTRAL, "The following homes are available to you:");
        }
        else
        {
            i18n.send(sender, NEUTRAL, "The following homes are available to {user}:", owner);
        }

        for (Home home : homes)
        {
            Component teleport = i18n.translate(sender, "(tp)").color(NamedTextColor.BLUE)
                                     .clickEvent(ClickEvent.runCommand("/home tp " + home.name + " " + home.getOwner().getName().get()))
                                     .hoverEvent(HoverEvent.showText(i18n.translate(sender, POSITIVE, "Click to teleport to {name}", home.name)));
            if (home.isOwner(sender))
            {
                sender.sendMessage(Identity.nil(), Component.text().append(Component.text("   - "))
                                                            .append(Component.text(home.name + " ", NamedTextColor.GOLD))
                                                            .append(teleport).build());
            }
            else
            {
                sender.sendMessage(Identity.nil(), Component.text().append(Component.text("   - "))
                                                            .append(Component.text(home.getOwner().getName() +":" + home.name + " ", NamedTextColor.GOLD))
                                                            .append(teleport).build());
            }
        }
    }

    @Command(name = "ilist", alias = "invited", desc = "List all players invited to your homes")
    public void invitedList(CommandCause sender, @Default User owner)
    {
        final boolean isOwner = sender.getAudience() instanceof ServerPlayer && owner.getUniqueId().equals(((ServerPlayer)sender.getAudience()).getUniqueId());

        if (!isOwner && !perms.HOME_LIST_OTHER.check(sender.getSubject(), sender.getAudience(), i18n))
        {
            return;
        }
        Set<Home> homes = this.manager.list(owner, true, false).stream()
                                      .filter(home -> !home.invites.isEmpty())
                                      .collect(toSet());
        if (homes.isEmpty())
        {
            if (isOwner)
            {
                i18n.send(sender, NEGATIVE, "You have no homes with players invited to them!");
                return;
            }
            i18n.send(sender, NEGATIVE, "{user} has no homes with players invited to them!", owner);
            return;
        }
        if (isOwner)
        {
            i18n.send(sender, NEUTRAL, "Your following homes have players invited to them:");
        }
        else
        {
            i18n.send(sender, NEUTRAL, "The following homes of {user} have players invited to them:", owner);
        }
        for (Home home : homes)
        {
            sender.sendMessage(Identity.nil(), Component.text("  " + home.name + ":", NamedTextColor.GOLD));
            for (UUID invite : home.invites)
            {
                final String name = Sponge.getServer().getUserManager().get(invite).get().getName();
                final Component unInvite = Component.text("(-)", NamedTextColor.RED).hoverEvent(
                    HoverEvent.showText(i18n.translate(sender, NEUTRAL, "Click to uninvite {user} from {name}", name, home.name)))
                                                    .clickEvent(ClickEvent.runCommand("/home uninvite " + name + " " + home.name));
                sender.sendMessage(Identity.nil(), unInvite);
            }
        }
    }

    @Restricted(msg = "How about making a phone call to invite someone instead?")
    @Command(desc = "Invite a user to one of your homes")
    public void invite(ServerPlayer sender, User player, @Parser(completer = HomeCompleter.class) @Option String home)
    {
        home = home == null ? "home" : home;
        Home h = this.manager.get(sender.getUniqueId(), home).orElse(null);
        if (h == null || !h.owner.equals(sender.getUniqueId()))
        {
            i18n.send(sender, NEGATIVE, "You do not own a home named {name#home}!", home);
            return;
        }
        if (player.getUniqueId().equals(sender.getUniqueId()))
        {
            i18n.send(sender, NEGATIVE, "You cannot invite yourself to your own home!");
            return;
        }
        if (h.isInvited(player))
        {
            i18n.send(sender, NEGATIVE, "{user} is already invited to your home!", player);
            return;
        }
        h.invites.add(player.getUniqueId());
        this.manager.save();
        if (player.isOnline())
        {
            i18n.send(player.getPlayer().get(), NEUTRAL,
                      "{user} invited you to their home. To teleport to it use: /home {name#home} {name}", sender,
                      h.name, sender.getName());
        }
        i18n.send(sender, POSITIVE, "{user} is now invited to your home {name}", player, h.name);
    }

    @Restricted(ServerPlayer.class)
    @Command(desc = "Uninvite a player from one of your homes")
    public void unInvite(ServerPlayer sender, User player, @Parser(completer = HomeCompleter.class) @Option String home)
    {
        home = home == null ? "home" : home;
        Home h = this.manager.get(sender.getUniqueId(), home).orElse(null);
        if (h == null || !h.owner.equals(sender.getUniqueId()))
        {
            i18n.send(sender, NEGATIVE, "You do not own a home named {name#home}!", home);
            return;
        }
        if (player.getUniqueId().equals(sender.getUniqueId()))
        {
            i18n.send(sender, NEGATIVE, "You cannot uninvite yourself from your own home!");
            return;
        }
        if (!h.isInvited(player))
        {
            i18n.send(sender, NEGATIVE, "{user} is not invited to your home!", player);
            return;
        }
        h.invites.remove(player.getUniqueId());
        if (player.isOnline())
        {
            i18n.send(player.getPlayer().get(), NEUTRAL, "You are no longer invited to {user}'s home {name#home}", sender, h.name);
        }
        i18n.send(sender, POSITIVE, "{user} is no longer invited to your home {name}", player, h.name);
    }

    @Alias("purgehomes")
    @Command(desc = "Removes all homes in a world")
    public void purge(CommandCause context, ServerWorld world)
    {
        this.manager.purge(world);
        i18n.send(context, POSITIVE, "Purged all homes in {world}", world);
    }

    @Alias("clearhomes")
    @Command(desc = "Clear all homes [of a player]")
    public void clear(final CommandCause context, @Option final User owner,
                      @Flag(value = "sel", longName = "selection") final boolean selection)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(context.getAudience() instanceof SystemSubject))
        {
            i18n.send(context, NEGATIVE, "This command has been disabled for ingame use via the configuration");
            return;
        }
        final ServerLocation firstPoint;
        final ServerLocation secondPoint;
        if (selection)
        {
            if (selector == null)
            {
                i18n.send(context, NEGATIVE, "You need to use the Selector module to delete homes in a selection!");
                return;
            }
            if (!(context instanceof Player))
            {
                i18n.send(context, NEGATIVE, "You have to be in game to use the selection flag");
                return;
            }
            Shape shape = selector.getSelection((Player)context);
            if (!(shape instanceof Cuboid))
            {
                i18n.send(context, NEGATIVE, "Invalid selection!");
                return;
            }
            firstPoint = selector.getFirstPoint((Player)context);
            secondPoint = selector.getSecondPoint((Player)context);
            if (owner != null)
            {
                i18n.send(context, NEGATIVE, "Are you sure you want to delete all homes created by {user} in your current selection?", owner);
            }
            else
            {
                i18n.send(context, NEGATIVE, "Are you sure you want to delete all homes created in your current selection?");
            }
        }
        else
        {
            firstPoint = null;
            secondPoint = null;
            if (owner != null)
            {
                i18n.send(context, NEGATIVE, "Are you sure you want to delete all homes ever created by {user}?", owner);
            }
            else
            {
                i18n.send(context, NEGATIVE, "Are you sure you want to delete all homes ever created on this server?");
            }
        }
        Component confirmText = i18n.translate(context, NEUTRAL, "Confirm before 30 seconds have passed to delete the homes");
        ConfirmManager.requestConfirmation(i18n, confirmText, context.getAudience(), () -> {
            Predicate<Home> predicate = home -> true;
            if (owner != null)
            {
                predicate = predicate.and(h -> h.owner.equals(owner.getUniqueId()));
            }
            if (selection)
            {
                Cuboid cuboid = new Cuboid(firstPoint.getPosition(), secondPoint.getPosition());
                predicate = predicate.and(h -> {
                    Vector3d chp = h.transform.getPosition();
                    return h.world.getWorld().equals(firstPoint.getWorld())
                        && cuboid.contains(new Vector3d(chp.getX(), chp.getY(), chp.getZ()));
                });
                manager.massDelete(predicate);
                if (owner != null)
                {
                    i18n.send(context, POSITIVE, "The homes of {user} in the selection are now deleted", owner);
                    return;
                }
                i18n.send(context, POSITIVE, "The homes in the selection are now deleted.");
                return;
            }
            manager.massDelete(predicate);
            if (owner != null)
            {
                i18n.send(context, POSITIVE, "The homes of {user} are now deleted", owner);
                return;
            }
            i18n.send(context, POSITIVE, "The homes are now deleted.");
        });

    }

    private void homeNotFoundMessage(Audience sender, User user, String name)
    {
        if (sender instanceof ServerPlayer && ((ServerPlayer)sender).getUniqueId().equals(user.getUniqueId()))
        {
            i18n.send(sender, NEGATIVE, "You have no home named {name#home}!", name);
        }
        else
        {
            i18n.send(sender, NEGATIVE, "{user} has no home named {name#home}!", user, name);
        }
    }
}
