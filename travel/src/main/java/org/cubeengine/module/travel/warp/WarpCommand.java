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

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.task.thread.TrackedThread;
import org.cubeengine.libcube.util.ConfirmManager;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.config.Home;
import org.cubeengine.module.travel.config.Warp;
import org.cubeengine.libcube.service.command.CommandUtil;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.command.exception.PermissionDeniedException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.world.World;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;
import static org.spongepowered.api.text.format.TextColors.BLUE;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

@Command(name = "warp", desc = "Teleport to a warp")
public class WarpCommand extends ContainerCommand
{
    private final Travel module;
    private I18n i18n;
    private final WarpManager manager;

    public WarpCommand(CommandManager base, Travel module, I18n i18n)
    {
        super(base, Travel.class);
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

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        List<String> list = super.getSuggestions(invocation);
        list.addAll(invocation.getManager().getCompleter(Warp.class).getSuggestions(invocation));
        return list;
    }

    @Restricted(Player.class)
    @Command(desc = "Teleport to a warp")
    public void tp(Player sender, @Complete(WarpCompleter.class) String warp)
    {
        // TODO find close match and display as click cmd
        Warp w = manager.get(warp).orElse(null);
        if (w == null)
        {
            warpNotFoundMessage(sender,  warp);
            return;
        }
        if (!w.isOwner(sender) && !w.isAllowed(sender) && sender.hasPermission(module.getPermissions().WARP_TP_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_TP_OTHER);
        }
        Transform<World> location = w.transform.getTransformIn(w.world.getWorld());
        sender.setTransform(location);
        if (w.welcomeMsg != null)
        {
            sender.sendMessage(Text.of(w.welcomeMsg));
            return;
        }
        if (w.isOwner(sender))
        {
            i18n.sendTranslated(ACTION_BAR, sender, POSITIVE, "You have been teleported to your warp {name}!", w.name);
            return;
        }
        i18n.sendTranslated(ACTION_BAR, sender, POSITIVE, "You have been teleported to the warp {name} of {user}!", w.name, w.getOwner());
    }

    @Restricted(Player.class)
    @Alias(value = {"createwarp", "mkwarp", "makewarp"})
    @Command(alias = "make", desc = "Create a warp")
    public void create(Player sender, String name)
    {
        if (this.manager.getCount() >= this.module.getConfig().warps.max)
        {
            i18n.sendTranslated(sender, CRITICAL, "The server have reached its maximum number of warps!");
            i18n.sendTranslated(sender, NEGATIVE, "Some warps must be deleted for new ones to be made");
            return;
        }
        if (manager.has(name))
        {
            i18n.sendTranslated(sender, NEGATIVE, "A warp by that name already exist!");
            return;
        }
        if (name.contains(":") || name.length() >= 32)
        {
            i18n.sendTranslated(sender, NEGATIVE, "Warps may not have names that are longer than 32 characters nor contain colon(:)'s!");
            return;
        }
        if (this.manager.has(name))
        {
            i18n.sendTranslated(sender, NEGATIVE, "The warp already exists! You can move it with {text:/warp move}");
            return;
        }
        Warp warp = manager.create(sender, name, sender.getTransform());
        i18n.sendTranslated(sender, POSITIVE, "Your warp {name} has been created!", warp.name);
    }

    @Command(desc = "Set the welcome message of warps", alias = {"setgreeting", "setwelcome", "setwelcomemsg"})
    public void greeting(CommandSource sender, @Complete(WarpCompleter.class) String warp,
                         @Label("welcome message") @Greed(INFINITE) @Optional String message,
                         @Flag boolean append)
    {
        // TODO permission other
        Warp w = this.manager.get(warp).orElse(null);
        if (w == null)
        {
            warpNotFoundMessage(sender, warp);
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
            i18n.sendTranslated(sender, POSITIVE, "The welcome message for your warp {name} is now set to:", w.name);
        }
        else
        {
            i18n.sendTranslated(sender, POSITIVE, "The welcome message for the warp {name} of {user} is now set to:", w.name, w.getOwner());
        }
        sender.sendMessage(Text.of(w.welcomeMsg));
    }

    @Restricted(Player.class)
    @Command(desc = "Move a warp")
    public void move(Player sender, @Complete(WarpCompleter.class) String warp)
    {
        Warp w = manager.get(warp).orElse(null);
        if (w == null)
        {
            warpNotFoundMessage(sender, warp);
            return;
        }
        if (!w.isOwner(sender) && sender.hasPermission(module.getPermissions().WARP_MOVE_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_MOVE_OTHER);
        }
        w.setTransform(sender.getTransform());
        manager.save();
        if (w.isOwner(sender))
        {
            i18n.sendTranslated(sender, POSITIVE, "Your warp {name} has been moved to your current location!", w.name);
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "The warp {name} of {user} has been moved to your current location", w.name, w.getOwner());
    }

    @Alias(value = {"removewarp", "deletewarp", "delwarp", "remwarp"})
    @Command(alias = "delete", desc = "Remove a warp")
    public void remove(CommandSource sender, @Complete(WarpCompleter.class) String warp)
    {
        Warp w = manager.get(warp).orElse(null);
        if (w == null)
        {
            warpNotFoundMessage(sender, warp);
            return;
        }
        if (!w.isOwner(sender) && sender.hasPermission(module.getPermissions().WARP_REMOVE_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.getPermissions().WARP_REMOVE_OTHER);
        }
        manager.delete(w);
        if (w.isOwner(sender))
        {
            i18n.sendTranslated(sender, POSITIVE, "Your warp {name} has been removed", warp);
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "The warp {name} of {user} has been removed", warp, w.getOwner());
    }

    @Command(desc = "Rename a warp")
    public void rename(CommandSource sender, @Complete(WarpCompleter.class) String warp, @Label("new name") String newName)
    {
        Warp w = manager.get(warp).orElse(null);
        if (w == null)
        {
            warpNotFoundMessage(sender, warp);
            return;
        }
        if (!w.isOwner(sender) && sender.hasPermission(module.getPermissions().WARP_RENAME_OTHER.getId()))
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
            if (w.isOwner(sender))
            {
                i18n.sendTranslated(sender, POSITIVE, "Your warp {name} has been renamed to {name}", w.name, newName);
                return;
            }
            i18n.sendTranslated(sender, POSITIVE, "The warp {name} of {user} has been renamed to {name}", w.name, w.getOwner(), newName);
            return;
        }
        i18n.sendTranslated(sender, POSITIVE, "Could not rename the warp to {name}", newName);
    }

    @Command(desc = "List warps of a player")
    public void list(CommandSource context, @Optional Player owner)
    {
        if (!context.equals(owner))
        {
            CommandUtil.ensurePermission(context, module.getPermissions().WARP_LIST_OTHER);
        }
        Set<Warp> warps = this.manager.list(owner);
        if (warps.isEmpty())
        {
            i18n.sendTranslated(context, POSITIVE, "There are no warps set.");
            return;
        }
        i18n.sendTranslatedN(context, POSITIVE, warps.size(), "There is one warp set:", "There are {amount} warps set:", warps.size());
        for (Warp warp : warps)
        {
            Text teleport = i18n.getTranslation(context, MessageType.NONE, "(tp)").toBuilder().color(BLUE)
                                .onClick(TextActions.runCommand("/warp tp " + warp.name))
                                .onHover(TextActions.showText(i18n.getTranslation(context, POSITIVE, "Click to teleport to {name}", warp.name)))
                                .build();
            if (warp.isOwner(context))
            {
                context.sendMessage(Text.of(YELLOW, "  ", warp.name, " ", teleport));
            }
            else
            {
                context.sendMessage(Text.of(YELLOW, "  ", warp.getOwner().getName(), ":", warp.name, " ", teleport));
            }
        }
    }

    @Alias(value = "clearwarps")
    @Command(desc = "Clear all warps [of a player]")
    public void clear(final CommandSource context, @Optional Player owner)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(context instanceof ConsoleSource))
        {
            i18n.sendTranslated(context, NEGATIVE, "This command has been disabled for ingame use via the configuration");
            return;
        }
        if (owner != null)
        {
            i18n.sendTranslated(context, NEGATIVE, "Are you sure you want to delete all warps ever created by {user}?", owner);
        }
        else
        {
            i18n.sendTranslated(context, NEGATIVE, "Are you sure you want to delete all warps ever created on this server!?");
        }
        Text confirmText = i18n.getTranslation(context, NEUTRAL, "Confirm before 30 seconds have passed to delete the warps");
        ConfirmManager.requestConfirmation(i18n, confirmText, context, () -> {
            Predicate<Warp> predicate = warp -> true;
            if (owner != null)
            {
                predicate = predicate.and(warp -> warp.getOwner().equals(owner));
                manager.massDelete(predicate);
                i18n.sendTranslated(context, POSITIVE, "Deleted warps.");
            }
            else
            {
                manager.massDelete(predicate);
                i18n.sendTranslated(context, POSITIVE, "The warps are now deleted");
            }
        });
    }


    private void warpNotFoundMessage(CommandSource sender, String name)
    {
        i18n.sendTranslated(sender, NEGATIVE, "There is no warp named {name#warp}!", name);
    }
}
