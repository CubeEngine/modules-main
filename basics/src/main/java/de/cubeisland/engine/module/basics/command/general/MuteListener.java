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
import java.util.ArrayList;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.storage.BasicsUserEntity;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerChatEvent;

import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;

public class MuteListener
{
    private final Basics basics;
    private final IgnoreCommands ignore;
    private UserManager um;

    public MuteListener(Basics basics, IgnoreCommands ignore, UserManager um)
    {
        this.basics = basics;
        this.ignore = ignore;
        this.um = um;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event)
    {
        // muted?
        User sender = um.getExactUser(event.getUser().getUniqueId());
        if (sender != null)
        {
            BasicsUserEntity bUser = basics.getBasicsUser(event.getUser()).getEntity();
            Timestamp muted = bUser.getValue(TABLE_BASIC_USER.MUTED);
            if (muted != null && System.currentTimeMillis() < muted.getTime())
            {
                event.setCancelled(true);
                sender.sendTranslated(NEGATIVE, "You try to speak but nothing happens!");
            }
        }
        // ignored?
        ArrayList<Player> ignore = new ArrayList<>();
        for (Player player : event.getRecipients())
        {
            User user = um.getExactUser(player.getUniqueId());
            if (this.ignore.checkIgnored(user, sender))
            {
                ignore.add(player);
            }
        }
        event.getRecipients().removeAll(ignore);
    }
}
