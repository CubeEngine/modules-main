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
package org.cubeengine.module.travel;

import org.cubeengine.butler.CommandBase;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.exception.PriorityExceptionHandler;
import org.cubeengine.module.travel.config.*;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

public class TravelExceptionHandler implements PriorityExceptionHandler
{
    private I18n i18n;

    public TravelExceptionHandler(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Override
    public boolean handleException(Throwable e, CommandBase command, CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof Player && e instanceof MissingWorldException)
        {
            Player player = (Player)invocation.getCommandSource();
            org.cubeengine.module.travel.config.TeleportPoint point = ((MissingWorldException)e).getPoint();
            if (point.owner.equals(player.getUniqueId()))
            {
                if (point instanceof Home)
                {
                    i18n.sendTranslated(player, NEGATIVE, "Your home {name} is in a world that no longer exists!", point.name);
                }
                else
                {
                    i18n.sendTranslated(player, NEGATIVE, "Your warp {name} is in a world that no longer exists!", point.name);
                }
            }
            else
            {
                String owner = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(point.owner).map(User::getName).orElse(null);
                if (point instanceof Home)
                {
                    i18n.sendTranslated(player, NEGATIVE, "The home {name} of {user} is in a world that no longer exists!", point.name, owner);
                }
                else
                {
                    i18n.sendTranslated(player, NEGATIVE, "The warp {name} of {user} is in a world that no longer exists!", point.name, owner);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int priority()
    {
        return 1;
    }
}
