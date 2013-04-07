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
package de.cubeisland.cubeengine.basics.command.general;

import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.basics.Basics;
import de.cubeisland.cubeengine.basics.storage.BasicUser;

public class MuteListener implements Listener
{
    private final Basics basics;

    public MuteListener(Basics basics)
    {
        this.basics = basics;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event)
    {
        if (!event.getMessage().startsWith("/"))
        {
            // muted?
            User sender = this.basics.getCore().getUserManager().getExactUser(event.getPlayer());
            if (sender != null)
            {
                BasicUser bUser = this.basics.getBasicUserManager().getBasicUser(sender);
                if (bUser.muted != null && System.currentTimeMillis() < bUser.muted.getTime())
                {
                    event.setCancelled(true);
                    sender.sendTranslated("&cYou try to speak but nothing happens!");
                }
            }
            // ignored?
            ArrayList<Player> ignore = new ArrayList<Player>();
            for (Player player : event.getRecipients())
            {
                User user = this.basics.getCore().getUserManager().getExactUser(player);
                if (this.basics.getIgnoreListManager().checkIgnore(user, sender))
                {
                    ignore.add(player);
                }
            }
            event.getRecipients().removeAll(ignore);
        }
    }
}
