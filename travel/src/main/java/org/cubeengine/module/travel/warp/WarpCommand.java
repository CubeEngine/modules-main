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
package org.cubeengine.module.travel.warp;

import java.util.Set;
import java.util.function.Predicate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Greedy;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Parser;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.util.ConfirmManager;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.TravelPerm;
import org.cubeengine.module.travel.config.Warp;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
@Command(name = "warp", desc = "Teleport to a warp")
public class WarpCommand extends DispatcherCommand
{
    private final Travel module;
    private final I18n i18n;
    private final WarpManager manager;
    private final TravelPerm perms;

    @Inject
    public WarpCommand(Travel module, I18n i18n, WarpManager manager, TravelPerm perms)
    {
        super(Travel.class);
        this.module = module;
        this.i18n = i18n;
        this.manager = manager;
        this.perms = perms;
    }

    @Restricted
    @Command(name = "tp", desc = "Teleport to a warp", dispatcher = true)
    public void dispatcher(ServerPlayer sender, @Parser(completer = WarpCompleter.class) String warp)
    {
        this.tp(sender, warp);
    }

    @Restricted
    @Command(desc = "Teleport to a warp")
    public void tp(ServerPlayer sender, @Parser(completer = WarpCompleter.class) String warp)
    {
        // TODO find close match and display as click cmd
        Warp w = manager.get(warp).orElse(null);
        if (w == null)
        {
            warpNotFoundMessage(sender,  warp);
            return;
        }
        if (!w.isOwner(sender.user()) && !w.isAllowed(sender.user()) && !perms.WARP_TP_OTHER.check(sender, i18n))
        {
            return;
        }
        sender.setTransform(w.transform);
        if (w.welcomeMsg != null)
        {
            sender.sendMessage(Identity.nil(), Component.text(w.welcomeMsg));
            return;
        }
        if (w.isOwner(sender.user()))
        {
            i18n.send(ChatType.ACTION_BAR, sender, POSITIVE, "You have been teleported to your warp {name}!", w.name);
            return;
        }
        i18n.send(ChatType.ACTION_BAR, sender, POSITIVE, "You have been teleported to the warp {name} of {user}!", w.name, w.getOwner());
    }

    @Restricted
    @Alias(value = "createwarp", alias = {"mkwarp", "makewarp"})
    @Command(alias = "make", desc = "Create a warp")
    public void create(ServerPlayer sender, String name)
    {
        if (this.manager.getCount() >= this.module.getConfig().warps.max)
        {
            i18n.send(sender, CRITICAL, "The server have reached its maximum number of warps!");
            i18n.send(sender, NEGATIVE, "Some warps must be deleted for new ones to be made");
            return;
        }
        if (manager.has(name))
        {
            i18n.send(sender, NEGATIVE, "A warp by that name already exist!");
            return;
        }
        if (name.contains(":") || name.length() >= 32)
        {
            i18n.send(sender, NEGATIVE, "Warps may not have names that are longer than 32 characters nor contain colon(:)'s!");
            return;
        }
        if (this.manager.has(name))
        {
            i18n.send(sender, NEGATIVE, "The warp already exists! You can move it with {text:/warp move}");
            return;
        }
        Warp warp = manager.create(sender.user(), name, sender.world(), sender.transform());
        i18n.send(sender, POSITIVE, "Your warp {name} has been created!", warp.name);
    }

    @Command(desc = "Set the welcome message of warps", alias = {"setgreeting", "setwelcome", "setwelcomemsg"})
    public void greeting(CommandCause sender, @Parser(completer = WarpCompleter.class) String warp,
                         @Label("welcome message") @Greedy @Option String message,
                         @Flag boolean append)
    {
        // TODO permission other
        Warp w = this.manager.get(warp).orElse(null);
        if (w == null)
        {
            warpNotFoundMessage(sender.audience(), warp);
            return;
        }
        if (append)
        {
            w.welcomeMsg += message;
        }
        else
        {
            w.welcomeMsg = message;
        }
        manager.save();
        if (w.isOwner(sender))
        {
            i18n.send(sender, POSITIVE, "The welcome message for your warp {name} is now set to:", w.name);
        }
        else
        {
            i18n.send(sender, POSITIVE, "The welcome message for the warp {name} of {user} is now set to:", w.name, w.getOwner());
        }
        sender.sendMessage(Identity.nil(), Component.text(w.welcomeMsg));
    }

    @Restricted
    @Command(desc = "Move a warp")
    public void move(ServerPlayer sender, @Parser(completer = WarpCompleter.class) String warp)
    {
        Warp w = manager.get(warp).orElse(null);
        if (w == null)
        {
            warpNotFoundMessage(sender, warp);
            return;
        }
        if (!w.isOwner(sender.user()) && !perms.WARP_MOVE_OTHER.check(sender, i18n))
        {
            return;
        }
        w.setTransform(sender.world(), sender.transform());
        manager.save();
        if (w.isOwner(sender.user()))
        {
            i18n.send(sender, POSITIVE, "Your warp {name} has been moved to your current location!", w.name);
            return;
        }
        i18n.send(sender, POSITIVE, "The warp {name} of {user} has been moved to your current location", w.name, w.getOwner());
    }

    @Alias(value = "removewarp", alias = {"deletewarp", "delwarp", "remwarp"})
    @Command(alias = "delete", desc = "Remove a warp")
    public void remove(CommandCause sender, @Parser(completer = WarpCompleter.class) String warp)
    {
        Warp w = manager.get(warp).orElse(null);
        if (w == null)
        {
            warpNotFoundMessage(sender.audience(), warp);
            return;
        }
        final boolean isOwner = w.isOwner(sender);
        if (!isOwner && perms.WARP_REMOVE_OTHER.check(sender.subject(), sender.audience(), i18n))
        {
            return;
        }
        manager.delete(w);
        if (isOwner)
        {
            i18n.send(sender, POSITIVE, "Your warp {name} has been removed", warp);
            return;
        }
        i18n.send(sender, POSITIVE, "The warp {name} of {user} has been removed", warp, w.getOwner());
    }

    @Command(desc = "Rename a warp")
    public void rename(CommandCause sender, @Parser(completer = WarpCompleter.class) String warp, @Label("new name") String newName)
    {
        Warp w = manager.get(warp).orElse(null);
        if (w == null)
        {
            warpNotFoundMessage(sender.audience(), warp);
            return;
        }
        final boolean isOwner = w.isOwner(sender);
        if (!isOwner && !perms.WARP_RENAME_OTHER.check(sender.subject(), sender.audience(), i18n))
        {
            return;
        }
        if (warp.contains(":") || warp.length() >= 32)
        {
            i18n.send(sender, NEGATIVE, "Warps may not have names that are longer than 32 characters or contain colon(:)'s!");
            return;
        }
        if (manager.rename(w, newName))
        {
            if (isOwner)
            {
                i18n.send(sender, POSITIVE, "Your warp {name} has been renamed to {name}", w.name, newName);
                return;
            }
            i18n.send(sender, POSITIVE, "The warp {name} of {user} has been renamed to {name}", w.name, w.getOwner(), newName);
            return;
        }
        i18n.send(sender, POSITIVE, "Could not rename the warp to {name}", newName);
    }

    @Command(desc = "List warps of a player")
    public void list(CommandCause context, @Option User owner)
    {
        if (owner != null && !(context.audience() instanceof ServerPlayer && ((ServerPlayer)context.audience()).uniqueId().equals(owner.uniqueId())))
        {
            if (!perms.WARP_LIST_OTHER.check(context.subject(), context.audience(), i18n))
            {
                return;
            }
        }
        Set<Warp> warps = this.manager.list(owner);
        if (warps.isEmpty())
        {
            i18n.send(context, POSITIVE, "There are no warps set.");
            return;
        }
        i18n.sendN(context, POSITIVE, warps.size(), "There is one warp set:", "There are {amount} warps set:", warps.size());
        for (Warp warp : warps)
        {
            Component teleport = i18n.translate(context, "(tp)").color(NamedTextColor.BLUE)
                                     .clickEvent(ClickEvent.runCommand("/warp tp " + warp.name))
                                     .hoverEvent(HoverEvent.showText(i18n.translate(context, POSITIVE, "Click to teleport to {name}", warp.name)));

            if (warp.isOwner(context))
            {
                context.sendMessage(Identity.nil(), Component.text("  " + warp.name + " ", NamedTextColor.YELLOW).append(teleport));
            }
            else
            {
                context.sendMessage(Identity.nil(), Component.text("  "+ warp.getOwner().name() + ":" + warp.name + " ", NamedTextColor.YELLOW).append(teleport));
            }
        }
    }

    @Alias(value = "clearwarps")
    @Command(desc = "Clear all warps [of a player]")
    public void clear(final CommandCause context, @Option User owner)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(context.audience() instanceof SystemSubject))
        {
            i18n.send(context, NEGATIVE, "This command has been disabled for ingame use via the configuration");
            return;
        }
        if (owner != null)
        {
            i18n.send(context, NEGATIVE, "Are you sure you want to delete all warps ever created by {user}?", owner);
        }
        else
        {
            i18n.send(context, NEGATIVE, "Are you sure you want to delete all warps ever created on this server!?");
        }
        Component confirmText = i18n.translate(context, NEUTRAL, "Confirm before 30 seconds have passed to delete the warps");
        ConfirmManager.requestConfirmation(i18n, confirmText, context.audience(), () -> {
            Predicate<Warp> predicate = warp -> true;
            if (owner != null)
            {
                predicate = predicate.and(warp -> warp.isOwner(owner));
                manager.massDelete(predicate);
                i18n.send(context, POSITIVE, "Deleted warps.");
            }
            else
            {
                manager.massDelete(predicate);
                i18n.send(context, POSITIVE, "The warps are now deleted");
            }
        });
    }


    private void warpNotFoundMessage(Audience sender, String name)
    {
        i18n.send(sender, NEGATIVE, "There is no warp named {name#warp}!", name);
    }
}
