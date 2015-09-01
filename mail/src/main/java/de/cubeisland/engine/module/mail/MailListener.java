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
package de.cubeisland.engine.module.mail;

import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;

import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.event.Order.POST;

public class MailListener
{
    private MailModule module;
    private UserManager um;

    public MailListener(MailModule module, UserManager um)
    {
        this.module = module;
        this.um = um;
    }

    @Subscribe(order = POST)
    public void onAfterJoin(PlayerJoinEvent event)
    {
        User user = um.getExactUser(event.getUser().getUniqueId());
        MailAttachment mails = user.attachOrGet(MailAttachment.class, module);
        int amount = mails.countMail();
        if (amount > 0)
        {
            user.sendTranslatedN(POSITIVE, amount, "You have a new mail!", "You have {amount} of mail!", amount);
            user.sendTranslated(NEUTRAL, "Use {text:/mail read} to display them.");
        }
    }
}
