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
package org.cubeengine.module.travel;

import java.util.Set;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;

import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;

public class TpPointCommand extends ContainerCommand
{
    protected InviteManager iManager;
    private I18n i18n;

    public TpPointCommand(Travel module, I18n i18n)
    {
        super(module);
        this.i18n = i18n;
        iManager = module.getInviteManager();
    }

    protected void showList(CommandSource context, Player user, Set<? extends TeleportPoint> points)
    {
        for (TeleportPoint point : points)
        {
            if (point.isPublic())
            {
                if (user != null && point.isOwnedBy(user))
                {
                    i18n.sendTranslated(context, NEUTRAL, "  {tppoint} ({text:public})", point);
                }
                else
                {
                    i18n.sendTranslated(context, NEUTRAL, "  {user}:{tppoint} ({text:public})", point.getOwnerName(), point);
                }
            }
            else
            {
                if (user != null && point.isOwnedBy(user))
                {
                    i18n.sendTranslated(context, NEUTRAL, "  {tppoint} ({text:private})", point);
                }
                else
                {
                    i18n.sendTranslated(context, NEUTRAL, "  {user}:{tppoint} ({text:private})", point.getOwnerName(), point);
                }
            }
        }
    }
}
