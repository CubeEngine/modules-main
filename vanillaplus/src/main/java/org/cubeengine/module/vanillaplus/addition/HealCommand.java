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
package org.cubeengine.module.vanillaplus.addition;

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

public class HealCommand
{
    private final UserManager um;
    private CommandManager cm;
    private BanService banService;
    private Game game;
    private final Basics module;

    public HealCommand(Basics basics, UserManager um, EventManager em, TaskManager taskManager, CommandManager cm,
                       BanService banService, Game game)
    {
        this.module = basics;
        this.um = um;
        this.cm = cm;
        this.banService = banService;
        this.game = game;

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








}
