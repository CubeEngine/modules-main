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
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;

import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;

public class TpPointCommand extends ContainerCommand
{
    protected InviteManager iManager;

    public TpPointCommand(Travel module)
    {
        super(module);
        iManager = module.getInviteManager();
    }

    protected void showList(CommandSender context, User user, Set<? extends TeleportPoint> points)
    {
        for (TeleportPoint point : points)
        {
            if (point.isPublic())
            {
                if (user != null && point.isOwnedBy(user))
                {
                    context.sendTranslated(NEUTRAL, "  {tppoint} ({text:public})", point);
                }
                else
                {
                    context.sendTranslated(NEUTRAL, "  {user}:{tppoint} ({text:public})", point.getOwnerName(), point);
                }
            }
            else
            {
                if (user != null && point.isOwnedBy(user))
                {
                    context.sendTranslated(NEUTRAL, "  {tppoint} ({text:private})", point);
                }
                else
                {
                    context.sendTranslated(NEUTRAL, "  {user}:{tppoint} ({text:private})", point.getOwnerName(), point);
                }
            }
        }
    }
}
