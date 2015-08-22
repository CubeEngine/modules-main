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
package de.cubeisland.engine.module.basics.command.general;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.TimeUtil;
import de.cubeisland.engine.module.core.util.math.BlockVector3;
import de.cubeisland.engine.service.ban.UserBan;
import de.cubeisland.engine.service.command.CommandManager;
import de.cubeisland.engine.service.command.CommandSender;
import de.cubeisland.engine.service.task.TaskManager;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.service.user.UserList;
import de.cubeisland.engine.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.world.Location;

import static de.cubeisland.engine.service.i18n.formatter.MessageType.*;
import static java.text.DateFormat.SHORT;
import static java.text.DateFormat.getAvailableLocales;
import static org.spongepowered.api.entity.player.gamemode.GameModes.CREATIVE;

public class PlayerCommands
{
    private final UserManager um;
    private CommandManager cm;
    private BanService banService;
    private Game game;
    private final Basics module;

    public PlayerCommands(Basics basics, UserManager um, EventManager em, TaskManager taskManager, CommandManager cm, BanService banService, Game game)
    {
        this.module = basics;
        this.um = um;
        this.cm = cm;
        this.banService = banService;
        this.game = game;

    }

    @Command(desc = "Refills your hunger bar")
    public void feed(CommandSender context, @Optional UserList players)
    {
        if (players == null)
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "Don't feed the troll!");
                return;
            }
            User sender = (User)context;
            sender.asPlayer().offer(Keys.FOOD_LEVEL, 20);
            sender.asPlayer().offer(Keys.SATURATION, 20.0);
            sender.asPlayer().offer(Keys.EXHAUSTION, 0.0);
            context.sendTranslated(POSITIVE, "You are now fed!");
            return;
        }
        if (!context.hasPermission(module.perms().COMMAND_FEED_OTHER))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to feed other players!");
            return;
        }
        List<User> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
            }
            context.sendTranslated(POSITIVE, "You made everyone fat!");
            this.um.broadcastStatus(ChatFormat.BRIGHT_GREEN + "shared food with everyone.", context);
            // TODO MessageType separate for translate Messages and messages from external input e.g. /me
        }
        else
        {
            context.sendTranslated(POSITIVE, "Fed {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!players.isAll())
            {
                user.sendTranslated(POSITIVE, "You got fed by {user}!", context);
            }
            user.asPlayer().offer(Keys.FOOD_LEVEL, 20);
            user.asPlayer().offer(Keys.SATURATION, 20.0);
            user.asPlayer().offer(Keys.EXHAUSTION, 0.0);
        }
    }

    @Command(desc = "Empties the hunger bar")
    public void starve(CommandSender context, @Optional UserList players)
    {
        if (players == null)
        {
            if (!(context instanceof User))
            {
                context.sendMessage(Texts.of("\n\n\n\n\n\n\n\n\n\n\n\n\n"));
                context.sendTranslated(NEGATIVE, "I'll give you only one line to eat!");
                return;
            }
            User sender = (User)context;
            sender.asPlayer().offer(Keys.FOOD_LEVEL, 0);
            sender.asPlayer().offer(Keys.SATURATION, 0.0);
            sender.asPlayer().offer(Keys.EXHAUSTION, 4.0);
            context.sendTranslated(NEGATIVE, "You are now starving!");
            return;
        }
        if (!context.hasPermission(module.perms().COMMAND_STARVE_OTHER))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to let other players starve!");
            return;
        }
        List<User> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
                return;
            }
            context.sendTranslated(NEUTRAL, "You let everyone starve to death!");
            this.um.broadcastStatus(ChatFormat.YELLOW + "took away all food.", context);
        }
        else
        {
            context.sendTranslated(POSITIVE, "Starved {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!players.isAll())
            {
                user.sendTranslated(NEUTRAL, "You are suddenly starving!");
            }
            user.asPlayer().offer(Keys.FOOD_LEVEL, 0);
            user.asPlayer().offer(Keys.SATURATION, 0.0);
            user.asPlayer().offer(Keys.EXHAUSTION, 4.0);
        }
    }

    @Command(desc = "Heals a player")
    public void heal(CommandSender context, @Optional UserList players)
    {
        if (players == null)
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "Only time can heal your wounds!");
                return;
            }
            User sender = (User)context;
            sender.asPlayer().offer(Keys.HEALTH, sender.asPlayer().get(Keys.MAX_HEALTH).get());
            sender.sendTranslated(POSITIVE, "You are now healed!");
            return;
        }
        if (!context.hasPermission(module.perms().COMMAND_HEAL_OTHER))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to heal other players!");
            return;
        }
        List<User> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
                return;
            }
            context.sendTranslated(POSITIVE, "You healed everyone!");
            this.um.broadcastStatus(ChatFormat.BRIGHT_GREEN + "healed every player.", context);
        }
        else
        {
            context.sendTranslated(POSITIVE, "Healed {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!players.isAll())
            {
                user.sendTranslated(POSITIVE, "You got healed by {sender}!", context);
            }
            user.asPlayer().offer(Keys.HEALTH, user.asPlayer().get(Keys.MAX_HEALTH).get());
        }
    }

    private static final long SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000;

    @Command(desc = "Shows when given player was online the last time")
    public void seen(CommandSender context, Player player)
    {
        if (player.isOnline())
        {
            context.sendTranslated(NEUTRAL, "{user} is currently online!", player);
            return;
        }

        Date lastPlayed = player.get(Keys.FIRST_DATE_PLAYED).get();
        if (System.currentTimeMillis() - lastPlayed.getTime() <= SEVEN_DAYS) // If less than 7 days show timeframe instead of date
        {
            context.sendTranslated(NEUTRAL, "{user} was last seen {input#date}.", player, TimeUtil.format(context.getLocale(), new Date(lastPlayed.getTime())));
            return;
        }
        Date date = new Date(lastPlayed.getTime());
        DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale());
        context.sendTranslated(NEUTRAL, "{user} is offline since {input#time}", player, format.format(date));
    }




    @Command(desc = "Displays informations from a player!")
    public void whois(CommandSender context, User player)
    {
        if (player.asPlayer().isOnline())
        {
            context.sendTranslated(NEUTRAL, "Nickname: {user}", player);
        }
        else
        {
            context.sendTranslated(NEUTRAL, "Nickname: {user} ({text:offline})", player);
        }
        if (player.hasPlayedBefore() || player.isOnline())
        {
            context.sendTranslated(NEUTRAL, "Life: {decimal:0}/{decimal#max:0}", player.getHealth(), player.getMaxHealth());
            context.sendTranslated(NEUTRAL, "Hunger: {integer#foodlvl:0}/{text:20} ({integer#saturation}/{integer#foodlvl:0})", player.getFoodLevel(), (int)player.getSaturation(), player.getFoodLevel());
            context.sendTranslated(NEUTRAL, "Level: {integer#level} + {integer#percent}%", player.getLevel(), (int)(player.getExpPerecent() * 100));
            Location loc = player.getLocation();
            if (loc != null)
            {
                context.sendTranslated(NEUTRAL, "Position: {vector} in {world}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getExtent());
            }
            if (player.getAddress() != null)
            {
                context.sendTranslated(NEUTRAL, "IP: {input#ip}", player.getAddress().getAddress().getHostAddress());
            }
            if (player.getGameMode() != null)
            {
                context.sendTranslated(NEUTRAL, "Gamemode: {input#gamemode}", player.getGameMode().toString());
            }
            if (player.getAllowFlight())
            {
                context.sendTranslated(NEUTRAL, "Flymode: {text:true:color=BRIGHT_GREEN} {input#flying}", player.isFlying() ? "flying" : "not flying");
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Flymode: {text:false:color=RED}");
            }
            if (player.isOp())
            {
                context.sendTranslated(NEUTRAL, "OP: {text:true:color=BRIGHT_GREEN}");
            }
            Timestamp muted = module.getBasicsUser(player.asPlayer()).getEntity().getValue(TABLE_BASIC_USER.MUTED);
            if (muted != null && muted.getTime() > System.currentTimeMillis())
            {
                context.sendTranslated(NEUTRAL, "Muted until {input#time}", DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale()).format(muted));
            }
            if (player.getGameMode() != CREATIVE)
            {
                context.sendTranslated(NEUTRAL, "GodMode: {input#godmode}", player.isInvulnerable() ? ChatFormat.BRIGHT_GREEN + "true" : ChatFormat.RED + "false");
            }
            if (player.get(BasicsAttachment.class).isAfk())
            {
                context.sendTranslated(NEUTRAL, "AFK: {text:true:color=BRIGHT_GREEN}");
            }
            DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SHORT, SHORT, Locale.ENGLISH);
            context.sendTranslated(NEUTRAL, "First played: {input#date}", dateFormat.format(player.getFirstPlayed()));
        }
        if (banManager.isUserBanned(player.getUniqueId()))
        {
            UserBan ban = banManager.getUserBan(player.getUniqueId());
            String expires;
            DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale());
            if (ban.getExpires() != null)
            {
                expires = format.format(ban.getExpires());
            }
            else
            {
                expires = context.getTranslation(NONE, "for ever").toString();
            }
            context.sendTranslated(NEUTRAL, "Banned by {user} on {input#date}: {input#reason} ({input#expire})", ban.getSource(), format.format(ban.getCreated()), ban.getReason(), expires);
        }
    }

    @Command(desc = "Toggles the god-mode!")
    public void god(CommandSender context, @Default User player)
    {
        boolean other = false;
        if (!context.equals(player))
        {
            if (!module.perms().COMMAND_GOD_OTHER.isAuthorized(context))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to god others!");
                return;
            }
            other = true;
        }

        Integer invTime = player.asPlayer().get(Keys.INVULNERABILITY).or(0);
        if (invTime > 0)
        {
            player.asPlayer().remove(Keys.INVULNERABILITY);
            if (!other)
            {
                context.sendTranslated(NEUTRAL, "You are no longer invincible!");
                return;
            }
            player.sendTranslated(NEUTRAL, "You are no longer invincible!");
            context.sendTranslated(NEUTRAL, "{user} is no longer invincible!", player);
            return;
        }
        player.asPlayer().offer(Keys.INVULNERABILITY, Integer.MAX_VALUE);
        if (!other)
        {
            context.sendTranslated(POSITIVE, "You are now invincible!");
            return;
        }
        player.sendTranslated(POSITIVE, "You are now invincible!");
        context.sendTranslated(POSITIVE, "{user} is now invincible!", player);
    }

    @Command(desc = "Changes your walkspeed.")
    public void walkspeed(CommandSender context, Double speed, @Default User player)
    {
        boolean other = false;
        if (!context.equals(player))
        {
            if (!context.hasPermission(module.perms().COMMAND_WALKSPEED_OTHER))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to change the walk speed of an other player!");
                return;
            }
            other = true;
        }
        if (!player.getUser().isOnline())
        {
            context.sendTranslated(NEGATIVE, "{user} is offline!", player.getName());
            return;
        }
        if (speed >= 0 && speed <= 10)
        {
            player.asPlayer().offer(Keys.WALKING_SPEED, speed / 10.0);
            player.sendTranslated(POSITIVE, "You can now walk at {decimal:2}!", speed);
            return;
        }
        player.asPlayer().offer(Keys.WALKING_SPEED, 0.2);
        if (speed != null && speed > 9000)
        {
            player.sendTranslated(NEGATIVE, "It's over 9000!");
        }
        player.sendTranslated(NEUTRAL, "Walk speed has to be a Number between {text:0} and {text:10}!");
    }

    @Command(desc = "Lets you fly away")
    public void fly(CommandSender context, @Optional Float flyspeed, @Default @Named("player") User player)
    {
        // new cmd system does not provide a way for defaultProvider to give custom messages
        //context.sendTranslated(NEUTRAL, "{text:ProTip}: If your server flies away it will go offline.");
        //context.sendTranslated(NEUTRAL, "So... Stopping the Server in {text:3..:color=RED}");

        // PermissionChecks
        if (!context.equals(player) && !context.hasPermission(module.perms().COMMAND_FLY_OTHER))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to change the fly mode of other player!");
            return;
        }
        //I Believe I Can Fly ...
        if (flyspeed != null)
        {
            if (flyspeed >= 0 && flyspeed <= 10)
            {
                player.asPlayer().offer(Keys.FLYING_SPEED, flyspeed / 10f);
                player.sendTranslated(POSITIVE, "You can now fly at {decimal#speed:2}!", flyspeed);
                if (!player.equals(context))
                {
                    context.sendTranslated(POSITIVE, "{player} can now fly at {decimal#speed:2}!", player, flyspeed);
                }
            }
            else
            {
                if (flyspeed > 9000)
                {
                    context.sendTranslated(NEUTRAL, "It's over 9000!");
                }
                context.sendTranslated(NEGATIVE, "FlySpeed has to be a Number between {text:0} and {text:10}!");
            }
            player.asPlayer().offer(Keys.CAN_FLY, true);
            player.asPlayer().offer(Keys.FLYING, true);
            return;
        }
        player.asPlayer().offer(Keys.CAN_FLY, !player.asPlayer().getValue(Keys.CAN_FLY));
        if (player.asPlayer().getValue(Keys.CAN_FLY))
        {
            player.asPlayer().offer(FLYING_SPEED, 0.1);
            player.sendTranslated(POSITIVE, "You can now fly!");
            if (!player.equals(context))
            {
                context.sendTranslated(POSITIVE, "{player} can now fly!", player);
            }
            return;
        }
        player.sendTranslated(NEUTRAL, "You cannot fly anymore!");
        if (!player.equals(context))
        {
            context.sendTranslated(POSITIVE, "{player} cannot fly anymore!", player);
        }
    }



}
