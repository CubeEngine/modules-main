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
package org.cubeengine.module.basics.command;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.basics.Basics;
import org.cubeengine.module.basics.BasicsAttachment;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.core.util.TimeUtil;
import org.cubeengine.module.core.util.math.BlockVector3;
import de.cubeisland.engine.service.ban.UserBan;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserList;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.world.Location;

import static java.text.DateFormat.SHORT;
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





}
