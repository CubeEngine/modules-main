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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Greedy;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.util.TimeConversionException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.CommandPermissionException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.Ban.IP;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.ban.BanTypes;
import org.spongepowered.api.service.pagination.PaginationService;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.cubeengine.libcube.util.ChatFormat.*;

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
    private KickBanPerms perms;

    @Inject
    public KickBanCommands(KickBan module, Broadcaster bc, I18n i18n, KickBanPerms perms)
    {
        this.module = module;
        this.i18n = i18n;
        this.bc = bc;
        this.perms = perms;
    }

    public void init()
    {
        this.banService = Sponge.getServer().getServiceProvider().banService();
    }

    @Command(desc = "Kicks a player from the server")
    public void kick(CommandCause context, Collection<ServerPlayer> players, @Option @Greedy String reason) throws CommandPermissionException
    {
        reason = parseReason(reason, perms.COMMAND_KICK_NOREASON, context);
        for (ServerPlayer player : players)
        {
            player.kick(this.getKickMessage(reason, player));
            bc.broadcastTranslatedWithPerm(NEGATIVE, "{user} was kicked from the server by {user}!",
                                                perms.KICK_RECEIVEMESSAGE.getId(), player, context.getSubject());
        }
        bc.broadcastMessageWithPerm(Style.empty(), reason, perms.KICK_RECEIVEMESSAGE.getId());
    }

    private Component getKickMessage(String reason, Audience player)
    {
        return i18n.translate(player, NEGATIVE, "You have been kicked from the server!").append(Component.newline()).append(Component.newline()).append(Component.text(reason));
    }

    private Component getBanMessage(String reason, Audience player)
    {
        return i18n.translate(player, NEGATIVE,"You have been banned from this server!").append(Component.newline()).append(Component.newline()).append(Component.text(reason));
    }

    @Command(alias = "kickban", desc = "Bans a player permanently on your server.")
    public void ban(CommandCause context, User user, @Option @Greedy String reason,
                    @Flag(longName = "ipban", value = "ip") boolean ipban, @Flag boolean force) throws CommandPermissionException
    {
        if (this.cannotBanUser(context)) return;
        ServerPlayer player = null;
        if (user.isOnline() || (user.get(Keys.LAST_DATE_JOINED).isPresent()))
        {
            player = user.getPlayer().get();
        }
        else if (!force)
        {
            i18n.send(context, NEGATIVE,"{user} has never played on this server before! Use the -force flag to ban him anyway.", user);
            return;
        }
        reason = parseReason(reason, perms.COMMAND_BAN_NOREASON, context);
        Component banSource = Component.text(context.getSubject().getFriendlyIdentifier().orElse(context.getSubject().getIdentifier()));
        if (ipban)
        {
            if (player == null)
            {
                i18n.send(context, NEGATIVE, "You cannot IP ban a player that has never played on the server before!");
                return;
            }
            if (player.getConnection().getAddress().getAddress() != null)
            {
                InetAddress ipAdress = player.getConnection().getAddress().getAddress();
                if (this.banService.isBanned(ipAdress))
                {
                    i18n.send(context, NEGATIVE, "{user} is already IP banned!", user);
                    return;
                }

                this.banService.addBan(Ban.builder().type(BanTypes.IP).address(ipAdress).reason(Component.text(reason)).source(banSource).build());
                Set<String> bannedUsers = new HashSet<>();
                for (ServerPlayer ipPlayer : Sponge.getServer().getOnlinePlayers())
                {
                    if (ipPlayer.getConnection().getAddress().getAddress() != null && ipPlayer.getConnection().getAddress().getAddress().equals(ipAdress))
                    {
                        ipPlayer.kick(this.getBanMessage(reason, ipPlayer));
                        bannedUsers.add(ipPlayer.getName());
                    }
                }
                i18n.send(context, NEGATIVE, "You banned the IP: {input#ip}!", ipAdress.getHostAddress());
                bc.broadcastTranslatedWithPerm(NEGATIVE, "{user} was banned from the server by {sender}!",
                                               perms.BAN_RECEIVEMESSAGE.getId(), player, context);
                bc.broadcastMessageWithPerm(Style.empty(), reason, perms.BAN_RECEIVEMESSAGE.getId());
                bc.broadcastTranslatedWithPerm(NEGATIVE, "And with it kicked: {text}!",
                                               perms.BAN_RECEIVEMESSAGE.getId(),
                                               Component.join(Component.text(",", NamedTextColor.RED), bannedUsers.stream().map(u -> Component.text(u, NamedTextColor.DARK_GREEN)).collect(Collectors.toList())));
            }
            else
            {
                i18n.send(context, NEUTRAL, "You cannot IP ban {user} because he was offline for too long!", user);
            }
            return;
        }
        else
        {
            if (this.banService.isBanned(user.getProfile()))
            {
                i18n.send(context, NEGATIVE, "{user} is already banned!", user);
                return;
            }
            this.banService.addBan(Ban.builder().type(BanTypes.PROFILE).profile(user.getProfile()).reason(Component.text(reason)).source(banSource).build());
            if (player != null && player.isOnline())
            {
                player.kick(this.getBanMessage(reason, player));
            }
        }
        i18n.send(context, NEGATIVE, "You banned {user}!", user);
        bc.broadcastTranslatedWithPerm(NEGATIVE, "{user} was banned from the server by {sender}!", perms.BAN_RECEIVEMESSAGE.getId(), user, context);
        bc.broadcastMessageWithPerm(Style.empty(), reason, perms.BAN_RECEIVEMESSAGE.getId());
    }

    private String parseReason(String reason, Permission permission, CommandCause cmdCause) throws CommandPermissionException
    {
        if (reason == null)
        {
            if (!permission.check(cmdCause.getSubject()))
            {
                throw new CommandPermissionException(i18n.translate(cmdCause, NEGATIVE, "You need to specify a reason!"));
            }
            return "";
        }
        return ChatFormat.parseFormats(reason);
    }


    @Command(alias = "pardon", desc = "Unbans a previously banned player.")
    public void unban(CommandCause context, User player)
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
    public void ipban(CommandCause context, @Label("IP address") String ipaddress, @Option @Greedy String reason)
    {
        try
        {
            InetAddress address = InetAddress.getByName(ipaddress);
            if (this.banService.isBanned(address))
            {
                i18n.send(context, NEUTRAL, "The IP {input#ip} is already banned!", address.getHostAddress());
                return;
            }
            reason = parseReason(reason, perms.COMMAND_IPBAN_NOREASON, context);
            Component banSource = Component.text(context.getSubject().getFriendlyIdentifier().orElse(context.getSubject().getIdentifier()));
            this.banService.addBan(Ban.builder().type(BanTypes.IP).address(address).reason(Component.text(reason)).source(banSource).build());
            i18n.send(context, NEGATIVE, "You banned the IP {input#ip} from your server!", address.getHostAddress());
            Set<String> bannedUsers = new HashSet<>();
            for (ServerPlayer player : Sponge.getServer().getOnlinePlayers())
            {
                if (player.getConnection().getAddress().getAddress() != null && player.getConnection().getAddress().getAddress().getHostAddress().equals(ipaddress))
                {
                    player.kick(this.getBanMessage(reason, player));
                    bannedUsers.add(player.getName());
                }
            }
            bc.broadcastTranslatedWithPerm(NEGATIVE, "The IP {input#ip} was banned from the server by {sender}!",
                                           perms.BAN_RECEIVEMESSAGE.getId(), ipaddress, context);
            bc.broadcastMessageWithPerm(Style.empty(), reason, perms.BAN_RECEIVEMESSAGE.getId());
            if (!bannedUsers.isEmpty())
            {
                bc.broadcastTranslatedWithPerm(NEGATIVE, "And with it kicked: {text}!",
                                               perms.BAN_RECEIVEMESSAGE.getId(),
                                               Component.join(Component.text(",", NamedTextColor.RED), bannedUsers.stream().map(u -> Component.text(u, NamedTextColor.DARK_GREEN)).collect(Collectors.toList())));
            }
        }
        catch (UnknownHostException | CommandPermissionException e)
        {
            i18n.send(context, NEGATIVE, "{input#ip} is not a valid IP address!", ipaddress);
        }
    }

    @Command(alias = {"unbanip", "pardonip"}, desc = "Bans the IP from this server.")
    public void ipunban(CommandCause context, @Label("IP address") String ipaddress)
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
    public void tempban(CommandCause context, User user, String time, @Option @Greedy String reason, @Flag boolean force) throws CommandPermissionException
    {
        if (this.cannotBanUser(context)) return;
        ServerPlayer player = null;

        if (user.isOnline() || (user.get(Keys.LAST_DATE_JOINED).isPresent()))
        {
            player = user.getPlayer().get();
        }
        else if (!force)
        {
            i18n.send(context, NEUTRAL, "{user} has never played on this server before! Use the -force flag to ban him anyways.", user);
            return;
        }
        reason = parseReason(reason, perms.COMMAND_TEMPBAN_NOREASON, context);
        if (this.banService.isBanned(user.getProfile()))
        {
            i18n.send(context, NEGATIVE, "{user} is already banned!", user);
            return;
        }
        try
        {
            long millis = StringUtils.convertTimeToMillis(time);
            Instant until = Instant.now().plusMillis(millis);
            Component banSource = Component.text(context.getSubject().getFriendlyIdentifier().orElse(context.getSubject().getIdentifier()));
            this.banService.addBan(Ban.builder().type(BanTypes.PROFILE).profile(user.getProfile()).reason(Component.text(reason)).expirationDate(until).source(banSource).build());
            if (user.isOnline())
            {
                if (player == null) throw new IllegalStateException();
                player.kick(getBanMessage(reason, player));
            }
            i18n.send(context, POSITIVE, "You banned {user} temporarily!", user);
            bc.broadcastTranslatedWithPerm(NEGATIVE, "{user} was banned temporarily from the server by {sender}!",
                                           perms.BAN_RECEIVEMESSAGE.getId(), user, context);
            bc.broadcastMessageWithPerm(Style.empty(), reason, perms.BAN_RECEIVEMESSAGE.getId());
        }
        catch (TimeConversionException ex)
        {
            i18n.send(context, NEGATIVE, "Invalid time value! Examples: 1d 12h 5m");
        }
    }

    private boolean cannotBanUser(CommandCause cmdCause)
    {
        if (!Sponge.getServer().isOnlineModeEnabled())
        {
            if (this.module.getConfiguration().disallowBanIfOfflineMode)
            {
                i18n.send(cmdCause, NEGATIVE, "Banning players by name is not allowed in offline-mode!");
                i18n.send(cmdCause, NEUTRAL, "You can change this in your Basics-Configuration.");
                return true;
            }
            i18n.send(cmdCause, NEUTRAL, "The server is running in {text:OFFLINE-mode:color=DARK_RED}.");
            i18n.send(cmdCause, NEUTRAL, "Players could change their username with a cracked client!");
            i18n.send(cmdCause, POSITIVE, "You can IP-ban to prevent banning a real player in that case.");
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
    public void banlist(CommandCause context, BanListType type, @Option String filter)
    {
        // TODO clickable list
        //  -- current search --
        // <- abcdefg...0123...

        switch (type)
        {
            case PLAYERS:
                banlistPlayer(context, filter);
                return;
            case IPS:
                banlistIps(context, filter);
        }
    }

    private void banlistIps(CommandCause context, String filter)
    {
        // TODO filter
        Collection<IP> ipbans = this.banService.getIpBans();
        if (ipbans.isEmpty())
        {
            i18n.send(context, POSITIVE, "There are no IPs banned on this server!");
            return;
        }
        if (ipbans.size() == 1)
        {
            IP next = ipbans.iterator().next();
            i18n.send(context, POSITIVE, "Only the IP {name#ip} is banned on this server", next.getAddress().getHostAddress());
            return;
        }
        i18n.send(context, POSITIVE, "The following {amount} IPs are banned from this server", ipbans.size());
        // TODO paging
        ipbans.forEach(ipban -> {
            i18n.send(context, NEUTRAL, " - {name} was banned by {text}: {text#reason}",
                      ipban.getAddress().toString(),
                      ipban.getBanSource().orElse(Component.text("Server")),
                      ipban.getReason().orElse(Component.empty()));
        });
    }

    private void banlistPlayer(CommandCause context, String filter)
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
        // TODO paging
        userBans.forEach(userban -> {
            i18n.send(context, NEUTRAL, " - {name} was banned by {text}: {text#reason}",
                      userban.getProfile().getName(),
                      userban.getBanSource().orElse(Component.text("Server")),
                      userban.getReason().orElse(Component.empty()));
        });
    }
}
