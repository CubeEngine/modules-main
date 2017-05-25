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
import org.cubeengine.libcube.service.command.readers.PlayerList;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.PermissionDescription;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class HealCommand extends PermissionContainer
{
    private I18n i18n;
    private Broadcaster bc;

    public HealCommand(PermissionManager pm, I18n i18n, Broadcaster bc)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
        this.bc = bc;
    }

    public final Permission COMMAND_HEAL_OTHER = register("command.heal.other", "", null);

    @Command(desc = "Heals a player")
    public void heal(CommandSource context, @Optional PlayerList players)
    {
        if (players == null)
        {
            if (!(context instanceof Player))
            {
                i18n.sendTranslated(context, NEGATIVE, "Only time can heal your wounds!");
                return;
            }
            Player sender = (Player)context;
            sender.offer(Keys.HEALTH, sender.get(Keys.MAX_HEALTH).get());
            i18n.sendTranslated(sender, POSITIVE, "You are now healed!");
            return;
        }
        if (!context.hasPermission(COMMAND_HEAL_OTHER.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to heal other players!");
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
            i18n.sendTranslated(context, POSITIVE, "You healed everyone!");
            bc.broadcastStatus(ChatFormat.BRIGHT_GREEN + "healed every player.", context);
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "Healed {amount} players!", userList.size());
        }
        for (Player user : userList)
        {
            if (!players.isAll())
            {
                i18n.sendTranslated(user, POSITIVE, "You got healed by {sender}!", context);
            }
            user.offer(Keys.HEALTH, user.get(Keys.MAX_HEALTH).get());
        }
    }
}
