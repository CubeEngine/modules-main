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

import static java.util.stream.Collectors.toSet;
import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;
import static org.spongepowered.api.text.format.TextColors.BLUE;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.RED;

import com.flowpowered.math.vector.Vector3d;
import de.cubeisland.engine.modularity.core.Maybe;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.command.exception.PermissionDeniedException;
import org.cubeengine.libcube.service.command.property.RawPermission;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.util.ConfirmManager;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.config.Home;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Command(name = "home", desc = "Teleport to your home")
public class HomeCommand extends ContainerCommand
{
    private final HomeManager manager;
    private final Travel module;
    private Maybe<Selector> selector;
    private I18n i18n;

    public HomeCommand(CommandManager base, Travel module, Maybe<Selector> selector, I18n i18n)
    {
        super(base, Travel.class);
        this.module = module;
        this.selector = selector;
        this.i18n = i18n;
        this.manager = module.getHomeManager();
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

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        List<String> list = super.getSuggestions(invocation);
        list.addAll(invocation.getManager().getCompleter(Home.class).getSuggestions(invocation));
        return list;
    }

    @Restricted(Player.class)
    @Command(desc = "Teleport to a home")
    public void tp(Player sender, @Complete(HomeCompleter.class) @Optional String home, @Default User owner)
    {
        home = home == null ? "home" : home;
        // TODO find close match and display as click cmd
        Home h = this.manager.find(sender, home, owner).orElse(null);
        if (h == null)
        {
            homeNotFoundMessage(sender, owner, home);
            i18n.sendTranslated(sender, NEUTRAL, "Use {text:/sethome} to set your home"); // TODO create on click
            return;
        }
        if (!h.isInvited(sender) && !owner.equals(sender))
        {
            if (!sender.hasPermission(module.getPermissions().HOME_TP_OTHER.getId()))
            {
                throw new PermissionDeniedException(module.getPermissions().HOME_TP_OTHER);
            }
        }
        Transform<World> location = h.transform.getTransformIn(h.world.getWorld());
        sender.setTransform(location);
        if (h.welcomeMsg != null)
        {
            sender.sendMessage(Text.of(h.welcomeMsg));
            return;
        }
        if (h.owner.equals(sender.getUniqueId()))
        {
            i18n.sendTranslated(ACTION_BAR, sender, POSITIVE, "You have been teleported to your home {name}!", h.name);
            return;
        }
        i18n.sendTranslated(ACTION_BAR, sender, POSITIVE, "You have been teleported to the home {name} of {user}!", h.name, h.getOwner());
    }

    @Alias("sethome")
    @Command(alias = {"create", "sethome", "createhome"}, desc = "Set your home")
    @Restricted(value = Player.class, msg = "Ok so I'll need your new address then. No seriously this won't work!")
    public void set(CommandSource context, @Optional String name)
    {
        Player sender = (Player)context;
        if (this.manager.getCount(sender) >= this.module.getConfig().homes.max
            && !context.hasPermission(module.getPermissions().HOME_SET_MORE.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You have reached your maximum number of homes!");
            i18n.sendTranslated(context, NEUTRAL, "You have to delete a home to make a new one");
            return;
        }
        name = name == null ? "home" : name;
        if (name.contains(":") || name.length() >= 32)
        {
            i18n.sendTranslated(context, NEGATIVE, "Homes may not have names that are longer then 32 characters nor contain colon(:)'s!");
            return;
        }
        if (this.manager.has(sender, name))
        {
            i18n.sendTranslated(context, NEGATIVE, "The home already exists! You can move it with {text:/home move}");
            return;
        }
        Home home = this.manager.create(sender, name, sender.getTransform());
        i18n.sendTranslated(context, POSITIVE, "Your home {name} has been created!", home.name);
    }

    @Command(desc = "Set the welcome message of homes", alias = {"setgreeting", "setwelcome", "setwelcomemsg"})
    public void greeting(CommandSource sender,
                         @Complete(HomeCompleter.class) String home,
                         @Optional @Label("welcome message") @Greed(INFINITE) String message,
                         @Default @Named("owner") User owner,
                         @Flag boolean append)
    {
        Home h = this.manager.get(owner, home).orElse(null);
        if (h == null)
        {
            homeNotFoundMessage(sender, owner, home);
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
        if (h.getOwner().equals(sender))
        {
            i18n.sendTranslated(sender, POSITIVE, "The welcome message for your home {name} is now set to:", h.name);
        }
        else
        {
            i18n.sendTranslated(sender, POSITIVE, "The welcome message for the home {name} of {user} is now set to:", h.name, owner);
        }
        sender.sendMessage(Text.of(h.welcomeMsg));
    }

    @Restricted(value = Player.class, msg = "I am calling the moving company right now!")
    @Command(alias = "replace", desc = "Move a home")
    public void move(Player sender, @Complete(HomeCompleter.class) @Optional String name, @Default User owner)
    {
        name = name == null ? "home" : name;
        Home home = this.manager.get(owner, name).orElse(null);
        if (home == null)
        {
            homeNotFoundMessage(sender, owner, name);
            i18n.sendTranslated(sender, NEUTRAL, "Use {text:/sethome} to set your home"); // TODO create on click
            return;
        }
        if (!home.isInvited(sender))
        {
            if (!sender.hasPermission(module.getPermissions().HOME_MOVE_OTHER.getId()))
            {
                throw new PermissionDeniedException(module.getPermissions().HOME_MOVE_OTHER);
            }
        }
        home.setTransform(sender.getTransform());
        manager.save();
        if (home.owner.equals(sender.getUniqueId()))
        {
            i18n.sendTranslated(sender, POSITIVE, "Your home {name} has been moved to your current location!", home.name);
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "The home {name} of {user} has been moved to your current location", home.name, owner);
    }

    @Alias(value = {"remhome", "removehome", "delhome", "deletehome"})
    @Command(alias = {"delete", "rem", "del"}, desc = "Remove a home")
    public void remove(CommandSource sender, @Complete(HomeCompleter.class) @Optional String name, @Default @Optional Player owner)
    {
        name = name == null ? "home" : name;
        Home home = this.manager.get(owner, name).orElse(null);
        if (home == null)
        {
            homeNotFoundMessage(sender, owner, name);
            return;
        }
        if (!home.getOwner().equals(sender) && !sender.hasPermission(module.getPermissions().HOME_REMOVE_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.getPermissions().HOME_REMOVE_OTHER);
        }
        this.manager.delete(home);
        if (owner.equals(sender))
        {
            i18n.sendTranslated(sender, POSITIVE, "Your home {name} has been removed!", name);
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "The home {name} of {user} has been removed", name, owner);
    }

    @Command(desc = "Rename a home")
    public void rename(CommandSource sender, @Complete(HomeCompleter.class) String home, @Label("new name") String newName, @Default @Optional Player owner)
    {
        Home h = manager.get(owner, home).orElse(null);
        if (h == null)
        {
            homeNotFoundMessage(sender, owner, home);
            return;
        }
        if (!h.getOwner().equals(sender) && !sender.hasPermission(module.getPermissions().HOME_RENAME_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.getPermissions().HOME_RENAME_OTHER);
        }
        if (home.contains(":") || home.length() >= 32)
        {
            i18n.sendTranslated(sender, NEGATIVE,
                                   "Homes may not have names that are longer than 32 characters or contain colon(:)'s!");
            return;
        }
        String oldName = h.name;

        if (!manager.rename(h, newName))
        {
            i18n.sendTranslated(sender, POSITIVE, "Could not rename the home to {name}", newName);
            return;
        }
        if (h.getOwner().equals(sender))
        {
            i18n.sendTranslated(sender, POSITIVE, "Your home {name} has been renamed to {name}", oldName, newName);
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "The home {name} of {user} has been renamed to {name}", oldName, owner, newName);
    }

    @Alias(value = {"listhomes", "homes"})
    @Command(alias = "listhomes", desc = "Lists homes a player can access")
    public void list(CommandSource sender, @Default User owner,
                     @Flag boolean owned,
                     @Flag boolean invited) throws Exception
    {
        if (!owner.equals(sender))
        {
            Permission otherPerm = module.getPermissions().HOME_LIST_OTHER;
            if (!sender.hasPermission(otherPerm.getId()))
            {
                throw new PermissionDeniedException(new RawPermission(otherPerm.getId(), otherPerm.getDesc()));
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
            if (owner.equals(sender))
            {
                i18n.sendTranslated(sender, NEGATIVE, "No homes are available to you!");
                return;
            }
            i18n.sendTranslated(sender, NEGATIVE, "No homes are available to {user}!", owner);
            return;
        }
        if (owner.equals(sender))
        {
            i18n.sendTranslated(sender, NEUTRAL, "The following homes are available to you:");
        }
        else
        {
            i18n.sendTranslated(sender, NEUTRAL, "The following homes are available to {user}:", owner);
        }

        for (Home home : homes)
        {
            Text teleport = i18n.getTranslation(sender, MessageType.NONE, "(tp)").toBuilder().color(BLUE)
                .onClick(TextActions.runCommand("/home tp " + home.name + " " + home.getOwner().getName()))
                .onHover(TextActions.showText(i18n.getTranslation(sender, POSITIVE, "Click to teleport to {name}", home.name)))
                .build();
            if (home.isOwner(sender))
            {
                sender.sendMessage(Text.of("  - ", GOLD, home.name, " ", teleport));
            }
            else
            {
                sender.sendMessage(Text.of("  - ", GOLD, home.getOwner().getName(), ":", home.name, " ", teleport));
            }
        }
    }

    @Command(name = "ilist", alias = "invited", desc = "List all players invited to your homes")
    public void invitedList(CommandSource sender, @Default User owner)
    {
        if (!owner.equals(sender) && !sender.hasPermission(module.getPermissions().HOME_LIST_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.getPermissions().HOME_LIST_OTHER);
        }
        Set<Home> homes = this.manager.list(owner, true, false).stream()
                                      .filter(home -> !home.invites.isEmpty())
                                      .collect(toSet());
        if (homes.isEmpty())
        {
            if (owner.equals(sender))
            {
                i18n.sendTranslated(sender, NEGATIVE, "You have no homes with players invited to them!");
                return;
            }
            i18n.sendTranslated(sender, NEGATIVE, "{user} has no homes with players invited to them!", owner);
            return;
        }
        if (owner.equals(sender))
        {
            i18n.sendTranslated(sender, NEUTRAL, "Your following homes have players invited to them:");
        }
        else
        {
            i18n.sendTranslated(sender, NEUTRAL, "The following homes of {user} have players invited to them:", owner);
        }
        for (Home home : homes)
        {
            sender.sendMessage(Text.of(TextColors.GOLD, "  ", home.name, ":"));
            for (UUID invite : home.invites)
            {
                String name = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(invite).get().getName();
                Text uninvite = Text.of("(-)").toBuilder().color(RED)
                    .onHover(TextActions.showText(i18n.getTranslation(sender, NEUTRAL, "Click to uninvite {user} from {name}", name, home.name)))
                    .onClick(TextActions.runCommand("/home uninvite " + name + " " + home.name))
                    .build();
                sender.sendMessage(Text.of("    ", DARK_GREEN, name, " ", uninvite));
            }
        }
    }

    @Restricted(value = Player.class, msg = "How about making a phone call to invite someone instead?")
    @Command(desc = "Invite a user to one of your homes")
    public void invite(Player sender, User player, @Complete(HomeCompleter.class) @Optional String home)
    {
        home = home == null ? "home" : home;
        Home h = this.manager.get(sender, home).orElse(null);
        if (h == null || !h.owner.equals(sender.getUniqueId()))
        {
            i18n.sendTranslated(sender, NEGATIVE, "You do not own a home named {name#home}!", home);
            return;
        }
        if (player.equals(sender))
        {
            i18n.sendTranslated(sender, NEGATIVE, "You cannot invite yourself to your own home!");
            return;
        }
        if (h.isInvited(player))
        {
            i18n.sendTranslated(sender, NEGATIVE, "{user} is already invited to your home!", player);
            return;
        }
        h.invites.add(player.getUniqueId());
        this.manager.save();
        if (player.isOnline())
        {
            i18n.sendTranslated(player.getPlayer().get(), NEUTRAL,
                                "{user} invited you to their home. To teleport to it use: /home {name#home} {name}", sender,
                                h.name, sender.getName());
        }
        i18n.sendTranslated(sender, POSITIVE, "{user} is now invited to your home {name}", player, h.name);
    }

    @Restricted(Player.class)
    @Command(desc = "Uninvite a player from one of your homes")
    public void unInvite(Player sender, User player, @Complete(HomeCompleter.class) @Optional String home )
    {
        home = home == null ? "home" : home;
        Home h = this.manager.get(sender, home).orElse(null);
        if (h == null || !h.owner.equals(sender.getUniqueId()))
        {
            i18n.sendTranslated(sender, NEGATIVE, "You do not own a home named {name#home}!", home);
            return;
        }
        if (player.equals(sender))
        {
            i18n.sendTranslated(sender, NEGATIVE, "You cannot uninvite yourself from your own home!");
            return;
        }
        if (!h.isInvited(player))
        {
            i18n.sendTranslated(sender, NEGATIVE, "{user} is not invited to your home!", player);
            return;
        }
        h.invites.remove(player.getUniqueId());
        if (player.isOnline())
        {
            i18n.sendTranslated(player.getPlayer().get(), NEUTRAL, "You are no longer invited to {user}'s home {name#home}", sender, h.name);
        }
        i18n.sendTranslated(sender, POSITIVE, "{user} is no longer invited to your home {name}", player, h.name);
    }

    @Alias(value = {"clearhomes"})
    @Command(desc = "Clear all homes [of a player]")
    public void clear(final CommandSource context, @Optional final User owner,
                               @Flag(name = "sel", longName = "selection") final boolean selection)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(context instanceof ConsoleSource))
        {
            i18n.sendTranslated(context, NEGATIVE, "This command has been disabled for ingame use via the configuration");
            return;
        }
        final Location<World> firstPoint;
        final Location<World> secondPoint;
        if (selection)
        {
            if (selector.isAvailable())
            {
                i18n.sendTranslated(context, NEGATIVE, "You need to use the Selector module to delete homes in a selection!");
                return;
            }
            if (!(context instanceof Player))
            {
                i18n.sendTranslated(context, NEGATIVE, "You have to be in game to use the selection flag");
                return;
            }
            Selector selector = this.selector.value();
            Shape shape = selector.getSelection((Player)context);
            if (!(shape instanceof Cuboid))
            {
                i18n.sendTranslated(context, NEGATIVE, "Invalid selection!");
                return;
            }
            firstPoint = selector.getFirstPoint((Player)context);
            secondPoint = selector.getSecondPoint((Player)context);
            if (owner != null)
            {
                i18n.sendTranslated(context, NEGATIVE,
                                      "Are you sure you want to delete all homes created by {user} in your current selection?", owner);
            }
            else
            {
                i18n.sendTranslated(context, NEGATIVE,
                                      "Are you sure you want to delete all homes created in your current selection?");
            }
        }
        else
        {
            firstPoint = null;
            secondPoint = null;
            if (owner != null)
            {
                i18n.sendTranslated(context, NEGATIVE,
                                      "Are you sure you want to delete all homes ever created by {user}?", owner);
            }
            else
            {
                i18n.sendTranslated(context, NEGATIVE,
                                      "Are you sure you want to delete all homes ever created on this server?");
            }
        }
        Text confirmText = i18n.getTranslation(context, NEUTRAL, "Confirm before 30 seconds have passed to delete the homes");
        ConfirmManager.requestConfirmation(i18n,  confirmText, context,() -> {
            Predicate<Home> predicate = home -> true;
            if (owner != null)
            {
                predicate = predicate.and(h -> h.owner.equals(owner.getUniqueId()));
            }
            if (selection)
            {
                Cuboid cuboid = new Cuboid(new Vector3d(firstPoint.getX(), firstPoint.getY(), firstPoint.getZ()), new Vector3d(
                    secondPoint.getX(), secondPoint.getY(), secondPoint.getZ()));
                predicate = predicate.and(
                    h -> {
                        Vector3d chp = h.transform.getPosition();
                        return h.world.getWorld().equals(firstPoint.getExtent())
                            && cuboid.contains(new Vector3d(chp.getX(), chp.getY(), chp.getZ()));
                    });
                manager.massDelete(predicate);
                if (owner != null)
                {
                    i18n.sendTranslated(context, POSITIVE, "The homes of {user} in the selection are now deleted", owner);
                    return;
                }
                i18n.sendTranslated(context, POSITIVE, "The homes in the selection are now deleted.");
                return;
            }
            manager.massDelete(predicate);
            if (owner != null)
            {
                i18n.sendTranslated(context, POSITIVE, "The homes of {user} are now deleted", owner);
                return;
            }
            i18n.sendTranslated(context, POSITIVE, "The homes are now deleted.");

        });

    }

    private void homeNotFoundMessage(CommandSource sender, User user, String name)
    {
        if (sender.getIdentifier().equals(user.getIdentifier()))
        {
            i18n.sendTranslated(sender, NEGATIVE, "You have no home named {name#home}!", name);
        }
        else
        {
            i18n.sendTranslated(sender, NEGATIVE, "{user} has no home named {name#home}!", user, name);
        }
    }
}
