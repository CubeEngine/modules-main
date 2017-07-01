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
package org.cubeengine.module.kickban;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.libcube.service.command.CommandUtil.ensurePermission;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NONE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.libcube.util.ChatFormat.DARK_GREEN;
import static org.cubeengine.libcube.util.ChatFormat.GREY;
import static org.cubeengine.libcube.util.ChatFormat.RED;

import org.cubeengine.butler.parameter.TooFewArgumentsException;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.parser.PlayerList;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.util.TimeConversionException;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.Ban.Ip;
import org.spongepowered.api.util.ban.BanTypes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

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
    private I18n i18n;
    private BanService banService;
    private final Broadcaster bc;

    private static final String kickMessage = "You have been kicked from the server!";
    private static final String banMessage = "You have been banned from this server!";

    @Inject
    public KickBanCommands(KickBan module, Broadcaster bc, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
        this.bc = bc;
    }

    public void init()
    {
        this.banService = Sponge.getServiceManager().provideUnchecked(BanService.class);
    }

    @Command(desc = "Kicks a player from the server")
    public void kick(CommandSource context, PlayerList players, @Optional @Greed(INFINITE) String reason)
    {
        reason = parseReason(reason, module.perms().COMMAND_KICK_NOREASON, context);
        if (players.isAll())
        {
            ensurePermission(context, module.perms().COMMAND_KICK_ALL);
            for (Player toKick : Sponge.getServer().getOnlinePlayers())
            {
                if (!context.equals(toKick))
                {
                    toKick.kick(Text.of(NEGATIVE,i18n.getTranslation(toKick, kickMessage),"\n\n", reason));
                }
            }
            return;
        }
        for (Player user : players.list())
        {
            user.kick(Text.of(NEGATIVE, i18n.getTranslation(user, kickMessage),"\n\n", reason));
            bc.broadcastTranslatedWithPerm(NEGATIVE, "{user} was kicked from the server by {user}!",
                                                module.perms().KICK_RECEIVEMESSAGE.getId(), user, context);
        }
        bc.broadcastMessageWithPerm(NONE, reason, module.perms().KICK_RECEIVEMESSAGE.getId());
    }

    @Command(alias = "kickban", desc = "Bans a player permanently on your server.")
    public void ban(CommandSource context, User player, @Optional @Greed(INFINITE) String reason,
                    @Flag(longName = "ipban", name = "ip") boolean ipban, @Flag boolean force)
    {

        if (this.cannotBanUser(context)) return;
        Player user = null;
        if (player.isOnline() || (player.get(JoinData.class).isPresent()))
        {
            user = player.getPlayer().get();
        }
        else if (!force)
        {
            i18n.send(context, NEGATIVE,"{user} has never played on this server before! Use the -force flag to ban him anyway.", player);
            return;
        }
        reason = parseReason(reason, module.perms().COMMAND_BAN_NOREASON, context);
        if (ipban)
        {
            if (user == null)
            {
                i18n.send(context, NEGATIVE, "You cannot IP ban a player that has never played on the server before!");
                return;
            }
            if (user.getConnection().getAddress().getAddress() != null)
            {
                InetAddress ipAdress = user.getConnection().getAddress().getAddress();
                if (this.banService.isBanned(ipAdress))
                {
                    i18n.send(context, NEGATIVE, "{user} is already IP banned!", player);
                    return;
                }
                this.banService.addBan(Ban.builder().type(BanTypes.IP).address(ipAdress).reason(Text.of(reason)).source(context).build());
                Set<String> bannedUsers = new HashSet<>();
                for (Player ipPlayer : Sponge.getServer().getOnlinePlayers())
                {
                    if (ipPlayer.getConnection().getAddress().getAddress() != null && ipPlayer.getConnection().getAddress().getAddress().equals(ipAdress))
                    {
                        ipPlayer.kick(Text.of(NEGATIVE, i18n.getTranslation(ipPlayer, banMessage), "\n\n", reason));
                        bannedUsers.add(ipPlayer.getName());
                    }
                }
                i18n.send(context, NEGATIVE, "You banned the IP: {input#ip}!", ipAdress.getHostAddress());
                bc.broadcastTranslatedWithPerm(NEGATIVE, "{user} was banned from the server by {sender}!",
                                               module.perms().BAN_RECEIVEMESSAGE.getId(), user, context);
                bc.broadcastMessageWithPerm(NONE, reason, module.perms().BAN_RECEIVEMESSAGE.getId());
                bc.broadcastTranslatedWithPerm(NEGATIVE, "And with it kicked: {user#list}!",
                                               module.perms().BAN_RECEIVEMESSAGE.getId(), StringUtils.implode(RED + "," + DARK_GREEN, bannedUsers));
                // TODO implode with Text instead
            }
            else
            {
                i18n.send(context, NEUTRAL, "You cannot IP ban {user} because he was offline for too long!", player);
            }
            return;
        }
        else
        {
            if (this.banService.isBanned(player.getProfile()))
            {
                i18n.send(context, NEGATIVE, "{user} is already banned!", player);
                return;
            }
            this.banService.addBan(Ban.builder().type(BanTypes.PROFILE).profile(player.getProfile()).reason(Text.of(reason)).source(
                context).build());
            if (user != null && user.isOnline())
            {
                user.kick(Text.of(NEGATIVE, i18n.getTranslation(user, banMessage),"\n\n", reason));
            }
        }
        i18n.send(context, NEGATIVE, "You banned {user}!", player);
        bc.broadcastTranslatedWithPerm(NEGATIVE, "{user} was banned from the server by {sender}!", module.perms().BAN_RECEIVEMESSAGE.getId(), player, context);
        bc.broadcastMessageWithPerm(NONE, reason, module.perms().BAN_RECEIVEMESSAGE.getId());
    }

    private String parseReason(String reason, Permission permission, CommandSource sender)
    {
        if (reason == null)
        {
            if (!sender.hasPermission(permission.getId()))
            {
                i18n.send(sender, NEGATIVE, "You need to specify a reason!");
                throw new TooFewArgumentsException();
            }
            return  "";
        }
        return ChatFormat.parseFormats(reason);
    }


    @Command(alias = "pardon", desc = "Unbans a previously banned player.")
    public void unban(CommandSource context, User player)
    {
        if (banService.isBanned(player.getProfile()))
        {
            this.banService.pardon(player.getProfile());
            if (context instanceof Player)
            {
                i18n.send(context, POSITIVE, "You unbanned {user}!", player);
            }
            else
            {
                i18n.send(context, POSITIVE, "You unbanned {user}({name#uuid})!", player, player.getUniqueId().toString());
            }
            return;
        }
        i18n.send(context, NEGATIVE, "{user} is not banned, maybe you misspelled his name?", player);
    }

    @Command(alias = "banip", desc = "Bans the IP from this server.")
    public void ipban(CommandSource context, @Label("IP address") String ipaddress, @Optional @Greed(INFINITE) String reason)
    {
        try
        {
            InetAddress address = InetAddress.getByName(ipaddress);
            if (this.banService.isBanned(address))
            {
                i18n.send(context, NEUTRAL, "The IP {input#ip} is already banned!", address.getHostAddress());
                return;
            }
            reason = parseReason(reason, module.perms().COMMAND_IPBAN_NOREASON, context);
            this.banService.addBan(Ban.builder().type(BanTypes.IP).address(address).reason(Text.of(reason)).source(context).build());
            i18n.send(context, NEGATIVE, "You banned the IP {input#ip} from your server!", address.getHostAddress());
            Set<String> bannedUsers = new HashSet<>();
            for (Player user : Sponge.getServer().getOnlinePlayers())
            {
                if (user.getConnection().getAddress().getAddress() != null && user.getConnection().getAddress().getAddress().getHostAddress().equals(ipaddress))
                {
                    user.kick(Text.of(NEGATIVE,i18n.getTranslation(user, banMessage), "\n\n", reason));
                    bannedUsers.add(user.getName());
                }
            }
            bc.broadcastTranslatedWithPerm(NEGATIVE, "The IP {input#ip} was banned from the server by {sender}!",
                                           module.perms().BAN_RECEIVEMESSAGE.getId(), ipaddress, context);
            bc.broadcastMessageWithPerm(NONE, reason, module.perms().BAN_RECEIVEMESSAGE.getId());
            if (!bannedUsers.isEmpty())
            {
                bc.broadcastTranslatedWithPerm(NEGATIVE, "And with it kicked: {user#list}!",
                                               module.perms().BAN_RECEIVEMESSAGE.getId(),
                                               StringUtils.implode(RED + "," + DARK_GREEN, bannedUsers));
                // TODO implode with Text instead
            }
        }
        catch (UnknownHostException e)
        {
            i18n.send(context, NEGATIVE, "{input#ip} is not a valid IP address!", ipaddress);
        }
    }

    @Command(alias = {"unbanip", "pardonip"}, desc = "Bans the IP from this server.")
    public void ipunban(CommandSource context, @Label("IP address") String ipaddress)
    {
        try
        {
            InetAddress address = InetAddress.getByName(ipaddress);
            if (this.banService.isBanned(address))
            {
                this.banService.pardon(address);
                i18n.send(context, POSITIVE, "You unbanned the IP {input#ip}!", address.getHostAddress());
                return;
            }
            i18n.send(context, NEGATIVE, "The IP {input#ip} was not banned!", address.getHostAddress());
        }
        catch (UnknownHostException e)
        {
            i18n.send(context, NEGATIVE, "{input#ip} is not a valid IP address!", ipaddress);
        }
    }

    @Command(alias = "tban", desc = "Bans a player for a given time.")
    public void tempban(CommandSource context, User player, String time, @Optional @Greed(INFINITE) String reason, @Flag boolean force)
    {
        if (this.cannotBanUser(context)) return;
        Player user = null;

        if (player.isOnline() || (player.get(JoinData.class).isPresent()))
        {
            user = player.getPlayer().get();
        }
        else if (!force)
        {
            i18n.send(context, NEUTRAL, "{user} has never played on this server before! Use the -force flag to ban him anyways.", player);
            return;
        }
        reason = parseReason(reason, module.perms().COMMAND_TEMPBAN_NOREASON, context);
        if (this.banService.isBanned(player.getProfile()))
        {
            i18n.send(context, NEGATIVE, "{user} is already banned!", player);
            return;
        }
        try
        {
            long millis = StringUtils.convertTimeToMillis(time);
            Instant until = Instant.now().plusMillis(millis);
            this.banService.addBan(Ban.builder().type(BanTypes.PROFILE).profile(player.getProfile()).reason(Text.of(reason)).expirationDate(until).source(context).build());
            if (player.isOnline())
            {
                if (user == null) throw new IllegalStateException();
                user.kick(Text.of(NEGATIVE, i18n.getTranslation(user, banMessage) ,"\n\n" , reason));
            }
            i18n.send(context, POSITIVE, "You banned {user} temporarily!", player);
            bc.broadcastTranslatedWithPerm(NEGATIVE, "{user} was banned temporarily from the server by {sender}!",
                                           module.perms().BAN_RECEIVEMESSAGE.getId(), player, context);
            bc.broadcastMessageWithPerm(NONE, reason, module.perms().BAN_RECEIVEMESSAGE.getId());
        }
        catch (TimeConversionException ex)
        {
            i18n.send(context, NEGATIVE, "Invalid time value! Examples: 1d 12h 5m");
        }
    }

    private boolean cannotBanUser(CommandSource context)
    {
        if (!Sponge.getServer().getOnlineMode())
        {
            if (this.module.getConfiguration().disallowBanIfOfflineMode)
            {
                i18n.send(context, NEGATIVE, "Banning players by name is not allowed in offline-mode!");
                i18n.send(context, NEUTRAL, "You can change this in your Basics-Configuration.");
                return true;
            }
            i18n.send(context, NEUTRAL, "The server is running in {text:OFFLINE-mode:color=DARK_RED}.");
            i18n.send(context, NEUTRAL, "Players could change their username with a cracked client!");
            i18n.send(context, POSITIVE, "You can IP-ban to prevent banning a real player in that case.");
        }
        return false;
    }

    /* TODO reload bans?
    @Command(desc = "Reloads the ban lists")
    public void reloadbans(CommandContext context)
    {
        i18n.sendTranslated(context, POSITIVE, "Reload ban lists successfully!");
    }
    */

    public enum BanListType
    {
        IPS, PLAYERS
    }

    @Command(desc = "View all players banned from this server")
    public void banlist(CommandSource context, BanListType type, @Optional String filter)
    {
        // TODO clickable list
        //  -- current search --
        // <- abcdefg...0123...

        // TODO paging
        switch (type)
        {
            case PLAYERS:
                banlistPlayer(context, filter);
                return;
            case IPS:
                banlistIps(context, filter);
        }
    }

    private void banlistIps(CommandSource context, String filter)
    {
        // TODO filter
        Collection<Ip> ipbans = this.banService.getIpBans();
        if (ipbans.isEmpty())
        {
            i18n.send(context, POSITIVE, "There are no IPs banned on this server!");
            return;
        }
        if (ipbans.size() == 1)
        {
            Ip next = ipbans.iterator().next();
            i18n.send(context, POSITIVE, "Only the IP {name#ip} is banned on this server", next.getAddress().getHostAddress());
            return;
        }
        i18n.send(context, POSITIVE, "The following {amount} IPs are banned from this server", ipbans.size());
        context.sendMessage(Text.of(StringUtils.implode(GREY + ", ", ipbans)));
        // TODO implode on Text
    }

    private void banlistPlayer(CommandSource context, String filter)
    {
        // TODO filter
        Collection<Ban.Profile> userBans = this.banService.getProfileBans();
        if (userBans.isEmpty())
        {
            i18n.send(context, POSITIVE, "There are no players banned on this server!");
            return;
        }
        if (userBans.size() == 1)
        {
            Ban.Profile next = userBans.iterator().next();
            i18n.send(context, POSITIVE, "Only {user}({name#uuid}) is banned on this server",
                                   next.getProfile().getName().orElse("?"), next.getProfile().getUniqueId().toString());
            return;
        }
        i18n.send(context, POSITIVE, "The following {amount} players are banned from this server",
                               userBans.size());
        context.sendMessage(Text.of(StringUtils.implode(GREY + ", ", userBans)));
        // TODO implode on Text
    }
}
