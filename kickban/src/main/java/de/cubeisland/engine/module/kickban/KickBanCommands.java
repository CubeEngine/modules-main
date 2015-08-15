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
package de.cubeisland.engine.module.kickban;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import de.cubeisland.engine.butler.parameter.TooFewArgumentsException;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.converter.converter.generic.CollectionConverter;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.StringUtils;
import de.cubeisland.engine.module.core.util.TimeConversionException;
import de.cubeisland.engine.service.command.CommandContext;
import de.cubeisland.engine.service.command.CommandSender;
import de.cubeisland.engine.service.command.sender.WrappedCommandSender;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.service.user.UserList;
import de.cubeisland.engine.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.Ban.Ip;
import org.spongepowered.api.util.ban.Bans;
import org.spongepowered.api.util.command.CommandSource;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.module.core.util.ChatFormat.*;
import static de.cubeisland.engine.service.i18n.formatter.MessageType.*;

/**
 * Contains commands to manage kicks/bans.
 * /kick
 * /ban
 * /unban
 * /ipban
 * /ipunban
 */
public class KickBanCommands
{
    private final KickBan module;
    private final BanService banService;
    private final UserManager um;
    private Game game;

    private static final String kickMessage = "You have been kicked from the server!";
    private static final String banMessage = "You have been banned from this server!";

    public KickBanCommands(KickBan module, BanService banService, UserManager um, Game game)
    {
        this.module = module;
        this.banService = banService;
        this.um = um;
        this.game = game;
    }

    @Command(desc = "Kicks a player from the server")
    public void kick(CommandContext context, UserList players, @Optional @Greed(INFINITE) String reason)
    {
        reason = parseReason(reason, module.perms().COMMAND_KICK_NOREASON, context.getSource());
        if (players.isAll())
        {
            context.ensurePermission(module.perms().COMMAND_KICK_ALL);
            for (User toKick : this.um.getOnlineUsers())
            {
                if (!context.getSource().equals(toKick))
                {
                    toKick.asPlayer().kick(Texts.of(toKick.getTranslation(NEGATIVE, kickMessage) + "\n\n" + reason));
                }
            }
            return;
        }
        for (User user : players.list())
        {
            user.asPlayer().kick(Texts.of(user.getTranslation(NEGATIVE, kickMessage) + "\n\n" + reason));
            this.um.broadcastTranslatedWithPerm(NEGATIVE, "{user} was kicked from the server by {user}!",
                                                module.perms().KICK_RECEIVEMESSAGE.getId(), user, context.getSource());
        }
        this.um.broadcastMessageWithPerm(NONE, reason, module.perms().KICK_RECEIVEMESSAGE.getId());
    }

    @Command(alias = "kickban", desc = "Bans a player permanently on your server.")
    public void ban(CommandSender context, org.spongepowered.api.entity.player.User player, @Optional @Greed(INFINITE) String reason,
                    @Flag(longName = "ipban", name = "ip") boolean ipban, @Flag boolean force)
    {

        if (this.cannotBanUser(context)) return;
        User user = null;
        if (player.isOnline() || (player.get(JoinData.class).isPresent()))
        {
            user = um.getExactUser(player.getUniqueId());
        }
        else if (!force)
        {
            context.sendTranslated(NEGATIVE,"{user} has never played on this server before! Use the -force flag to ban him anyway.", player);
            return;
        }
        reason = parseReason(reason, module.perms().COMMAND_BAN_NOREASON, context);
        if (ipban)
        {
            if (user == null)
            {
                context.sendTranslated(NEGATIVE, "You cannot IP ban a player that has never played on the server before!");
                return;
            }
            if (user.getAddress() != null)
            {
                InetAddress ipAdress = user.getAddress().getAddress();
                if (this.banService.isBanned(ipAdress))
                {
                    context.sendTranslated(NEGATIVE, "{user} is already IP banned!", player);
                    return;
                }
                this.banService.ban(Bans.builder().address(ipAdress).reason(Texts.of(reason)).source(asCommandSource(context)).build());
                Set<String> bannedUsers = new HashSet<>();
                for (User ipPlayer : um.getOnlineUsers())
                {
                    if (ipPlayer.getAddress() != null && ipPlayer.getAddress().getAddress().equals(ipAdress))
                    {
                        ipPlayer.asPlayer().kick(Texts.of(ipPlayer.getTranslation(NEGATIVE, banMessage) + "\n\n"
                                                              + reason));
                        bannedUsers.add(ipPlayer.getName());
                    }
                }
                context.sendTranslated(NEGATIVE, "You banned the IP: {input#ip}!", ipAdress.getHostAddress());
                um.broadcastTranslatedWithPerm(NEGATIVE, "{user} was banned from the server by {sender}!",
                                               module.perms().BAN_RECEIVEMESSAGE.getId(), user, context);
                um.broadcastMessageWithPerm(NONE, reason, module.perms().BAN_RECEIVEMESSAGE.getId());
                um.broadcastTranslatedWithPerm(NEGATIVE, "And with it kicked: {user#list}!",
                                               module.perms().BAN_RECEIVEMESSAGE.getId(), StringUtils.implode(
                        RED + "," + DARK_GREEN, bannedUsers));
            }
            else
            {
                context.sendTranslated(NEUTRAL, "You cannot IP ban {user} because he was offline for too long!", player);
            }
            return;
        }
        else
        {
            if (this.banService.isBanned(player))
            {
                context.sendTranslated(NEGATIVE, "{user} is already banned!", player);
                return;
            }
            this.banService.ban(Bans.builder().user(player).reason(Texts.of(reason)).source(asCommandSource(context)).build());
            if (user != null && user.asPlayer().isOnline())
            {
                user.asPlayer().kick(Texts.of(user.getTranslation(NEGATIVE, banMessage) + "\n\n" + reason));
            }
        }
        context.sendTranslated(NEGATIVE, "You banned {user}!", player);
        um.broadcastTranslatedWithPerm(NEGATIVE, "{user} was banned from the server by {sender}!", module.perms().BAN_RECEIVEMESSAGE.getId(), player, context);
        um.broadcastMessageWithPerm(NONE, reason, module.perms().BAN_RECEIVEMESSAGE.getId());
    }

    private CommandSource asCommandSource(CommandSender context)
    {
        CommandSource banSource = null;
        if (context instanceof WrappedCommandSender)
        {
            banSource = ((WrappedCommandSender)context).getWrappedSender();
        }
        return banSource;
    }

    private String parseReason(String reason, PermissionDescription permission, CommandSender sender)
    {
        if (reason == null)
        {
            if (!sender.hasPermission(permission.getId()))
            {
                sender.sendTranslated(NEGATIVE, "You need to specify a reason!");
                throw new TooFewArgumentsException();
            }
            return  "";
        }
        return ChatFormat.parseFormats(reason);
    }


    @Command(alias = "pardon", desc = "Unbans a previously banned player.")
    public void unban(CommandContext context, org.spongepowered.api.entity.player.User player)
    {
        if (banService.isBanned(player))
        {
            this.banService.pardon(player);
            context.sendTranslated(POSITIVE, "You unbanned {user}({name#uuid})!", player, player.getUniqueId().toString());
            return;
        }
        context.sendTranslated(NEGATIVE, "{user} is not banned, maybe you misspelled his name?", player);
    }

    @Command(alias = "banip", desc = "Bans the IP from this server.")
    public void ipban(CommandContext context, @Label("IP address") String ipaddress, @Optional @Greed(INFINITE) String reason)
    {
        try
        {
            InetAddress address = InetAddress.getByName(ipaddress);
            if (this.banService.isBanned(address))
            {
                context.sendTranslated(NEUTRAL, "The IP {input#ip} is already banned!", address.getHostAddress());
                return;
            }
            reason = parseReason(reason, module.perms().COMMAND_IPBAN_NOREASON, context.getSource());
            this.banService.ban(Bans.builder().address(address).reason(Texts.of(reason)).source(asCommandSource(context.getSource())).build());
            context.sendTranslated(NEGATIVE, "You banned the IP {input#ip} from your server!", address.getHostAddress());
            Set<String> bannedUsers = new HashSet<>();
            for (User user : um.getOnlineUsers())
            {
                if (user.getAddress() != null && user.getAddress().getAddress().getHostAddress().equals(ipaddress))
                {
                    user.asPlayer().kick(Texts.of(user.getTranslation(NEGATIVE, banMessage) + "\n\n" + reason));
                    bannedUsers.add(user.getName());
                }
            }
            um.broadcastTranslatedWithPerm(NEGATIVE, "The IP {input#ip} was banned from the server by {sender}!",
                                           module.perms().BAN_RECEIVEMESSAGE.getId(), ipaddress, context.getSource());
            um.broadcastMessageWithPerm(NONE, reason, module.perms().BAN_RECEIVEMESSAGE.getId());
            if (!bannedUsers.isEmpty())
            {
                um.broadcastTranslatedWithPerm(NEGATIVE, "And with it kicked: {user#list}!",
                                               module.perms().BAN_RECEIVEMESSAGE.getId(), StringUtils.implode(
                    RED + "," + DARK_GREEN, bannedUsers));
            }
        }
        catch (UnknownHostException e)
        {
            context.sendTranslated(NEGATIVE, "{input#ip} is not a valid IP address!", ipaddress);
        }
    }

    @Command(alias = {"unbanip", "pardonip"}, desc = "Bans the IP from this server.")
    public void ipunban(CommandContext context, @Label("IP address") String ipaddress)
    {
        try
        {
            InetAddress address = InetAddress.getByName(ipaddress);
            if (this.banService.isBanned(address))
            {
                this.banService.pardon(address);
                context.sendTranslated(POSITIVE, "You unbanned the IP {input#ip}!", address.getHostAddress());

            }
            context.sendTranslated(NEGATIVE, "The IP {input#ip} was not banned!", address.getHostAddress());
        }
        catch (UnknownHostException e)
        {
            context.sendTranslated(NEGATIVE, "{input#ip} is not a valid IP address!", ipaddress);
        }
    }

    @Command(alias = "tban", desc = "Bans a player for a given time.")
    public void tempban(CommandSender context, org.spongepowered.api.entity.player.User player, String time, @Optional @Greed(INFINITE) String reason, @Flag boolean force)
    {
        if (this.cannotBanUser(context)) return;
        User user = null;

        if (player.isOnline() || (player.get(JoinData.class).isPresent()))
        {
            user = um.getExactUser(player.getName());
        }
        else if (!force)
        {
            context.sendTranslated(NEUTRAL, "{user} has never played on this server before! Use the -force flag to ban him anyways.", player);
            return;
        }
        reason = parseReason(reason, module.perms().COMMAND_TEMPBAN_NOREASON, context);
        if (this.banService.isBanned(player))
        {
            context.sendTranslated(NEGATIVE, "{user} is already banned!", player);
            return;
        }
        try
        {
            long millis = StringUtils.convertTimeToMillis(time);
            Date toDate = new Date(System.currentTimeMillis() + millis);
            this.banService.ban(Bans.builder().user(player).reason(Texts.of(reason)).expirationDate(toDate).source(asCommandSource(context)).build());
            if (player.isOnline())
            {
                if (user == null) throw new IllegalStateException();
                user.asPlayer().kick(Texts.of(user.getTranslation(NEGATIVE, banMessage) + "\n\n" + reason));
            }
            context.sendTranslated(POSITIVE, "You banned {user} temporarily!", player);
            um.broadcastTranslatedWithPerm(NEGATIVE, "{user} was banned temporarily from the server by {sender}!",
                                           module.perms().BAN_RECEIVEMESSAGE.getId(), player, context);
            um.broadcastMessageWithPerm(NONE, reason, module.perms().BAN_RECEIVEMESSAGE.getId());
        }
        catch (TimeConversionException ex)
        {
            context.sendTranslated(NEGATIVE, "Invalid time value! Examples: 1d 12h 5m");
        }
    }

    private boolean cannotBanUser(CommandSender context)
    {
        if (!game.getServer().getOnlineMode())
        {
            if (this.module.getConfiguration().disallowBanIfOfflineMode)
            {
                context.sendTranslated(NEGATIVE, "Banning players by name is not allowed in offline-mode!");
                context.sendTranslated(NEUTRAL, "You can change this in your Basics-Configuration.");
                return true;
            }
            context.sendTranslated(NEUTRAL, "The server is running in {text:OFFLINE-mode:color=DARK_RED}.");
            context.sendTranslated(NEUTRAL, "Players could change their username with a cracked client!");
            context.sendTranslated(POSITIVE, "You can IP-ban to prevent banning a real player in that case.");
        }
        return false;
    }

    /* TODO reload bans?
    @Command(desc = "Reloads the ban lists")
    public void reloadbans(CommandContext context)
    {
        context.sendTranslated(POSITIVE, "Reload ban lists successfully!");
    }
    */

    public enum BanListType
    {
        IPS, PLAYERS
    }

    @Command(desc = "View all players banned from this server")
    public void banlist(CommandSender context, BanListType type)
    {
        // TODO paging
        switch (type)
        {
            case PLAYERS:
                banlistPlayer(context);
                return;
            case IPS:
                banlistIps(context);
        }
    }

    private void banlistIps(CommandSender context)
    {
        Collection<Ip> ipbans = this.banService.getIpBans();
        if (ipbans.isEmpty())
        {
            context.sendTranslated(POSITIVE, "There are no IPs banned on this server!");
            return;
        }
        if (ipbans.size() == 1)
        {
            Ip next = ipbans.iterator().next();
            context.sendTranslated(POSITIVE, "Only the IP {name#ip} is banned on this server", next.getAddress().getHostAddress());
            return;
        }
        context.sendTranslated(POSITIVE, "The following {amount} IPs are banned from this server", ipbans.size());
        context.sendMessage(Texts.of(StringUtils.implode(GREY + ", ", ipbans)));
    }

    private void banlistPlayer(CommandSender context)
    {
        Collection<Ban.User> userBans = this.banService.getUserBans();
        if (userBans.isEmpty())
        {
            context.sendTranslated(POSITIVE, "There are no players banned on this server!");
            return;
        }
        if (userBans.size() == 1)
        {
            Ban.User next = userBans.iterator().next();
            context.sendTranslated(POSITIVE, "Only {user}({name#uuid}) is banned on this server",
                                   next.getUser().getName(), next.getUser().getUniqueId());
            return;
        }
        context.sendTranslated(POSITIVE, "The following {amount} players are banned from this server",
                               userBans.size());
        context.sendMessage(Texts.of(StringUtils.implode(GREY + ", ", userBans)));
        return;
    }
}
