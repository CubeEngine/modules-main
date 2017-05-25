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
package org.cubeengine.module.vanillaplus.addition;

import java.util.Collection;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.parser.PlayerList;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.text.format.TextColors.GREEN;
import static org.spongepowered.api.text.format.TextFormat.NONE;

public class FoodCommands extends PermissionContainer
{

    private I18n i18n;
    private Broadcaster bc;

    public final Permission COMMAND_FEED_OTHER = register("command.feed.other", "", null);
    public final Permission COMMAND_STARVE_OTHER = register("command.starve.other", "", null);

    public FoodCommands(PermissionManager pm, I18n i18n, Broadcaster bc)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
        this.bc = bc;
    }

    @Command(desc = "Refills your hunger bar")
    public void feed(CommandSource context, @Optional PlayerList players)
    {
        if (players == null)
        {
            if (!(context instanceof Player))
            {
                i18n.sendTranslated(context, NEGATIVE, "Don't feed the troll!");
                return;
            }
            User sender = (User)context;
            sender.offer(Keys.FOOD_LEVEL, 20);
            sender.offer(Keys.SATURATION, 20.0);
            sender.offer(Keys.EXHAUSTION, 0.0);
            i18n.sendTranslated(context, POSITIVE, "You are now fed!");
            return;
        }
        if (!context.hasPermission(COMMAND_FEED_OTHER.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to feed other players!");
            return;
        }
        Collection<Player> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                i18n.sendTranslated(context, NEGATIVE, "There are no players online at the moment!");
            }
            i18n.sendTranslated(context, POSITIVE, "You made everyone fat!");
            bc.broadcastStatus(NONE.color(GREEN), "shared food with everyone.", context);
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "Fed {amount} players!", userList.size());
        }
        for (Player user : userList)
        {
            if (!players.isAll())
            {
                i18n.sendTranslated(user, POSITIVE, "You got fed by {user}!", context);
            }
            user.offer(Keys.FOOD_LEVEL, 20);
            user.offer(Keys.SATURATION, 20.0);
            user.offer(Keys.EXHAUSTION, 0.0);
        }
    }

    @Command(desc = "Empties the hunger bar")
    public void starve(CommandSource context, @Optional PlayerList players)
    {
        if (players == null)
        {
            if (!(context instanceof Player))
            {
                context.sendMessage(Text.of("\n\n\n\n\n\n\n\n\n\n\n\n\n"));
                i18n.sendTranslated(context, NEGATIVE, "I'll give you only one line to eat!");
                return;
            }
            User sender = (User)context;
            sender.offer(Keys.FOOD_LEVEL, 0);
            sender.offer(Keys.SATURATION, 0.0);
            sender.offer(Keys.EXHAUSTION, 4.0);
            i18n.sendTranslated(context, NEGATIVE, "You are now starving!");
            return;
        }
        if (!context.hasPermission(COMMAND_STARVE_OTHER.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to let other players starve!");
            return;
        }
        Collection<Player> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                i18n.sendTranslated(context, NEGATIVE, "There are no players online at the moment!");
                return;
            }
            i18n.sendTranslated(context, NEUTRAL, "You let everyone starve to death!");
            bc.broadcastStatus(ChatFormat.YELLOW + "took away all food.", context);
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "Starved {amount} players!", userList.size());
        }
        for (Player user : userList)
        {
            if (!players.isAll())
            {
                i18n.sendTranslated(user, NEUTRAL, "You are suddenly starving!");
            }
            user.offer(Keys.FOOD_LEVEL, 0);
            user.offer(Keys.SATURATION, 0.0);
            user.offer(Keys.EXHAUSTION, 4.0);
        }
    }
}
