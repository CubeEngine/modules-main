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
package org.cubeengine.module.travel.home;

import org.cubeengine.module.travel.Travel;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.manipulator.mutable.entity.SneakingData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;

import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.event.Order.EARLY;

public class HomeListener
{
    private final Travel module;
    private I18n i18n;
    private final HomeManager homeManager;

    public HomeListener(Travel module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
        this.homeManager = module.getHomeManager();
    }

    @Listener(order = EARLY)
    public void rightClickBed(InteractBlockEvent.Secondary event, @First Player player)
    {
        if (event.getTargetBlock().getState().getType() != BlockTypes.BED)
        {
            return;
        }
        if (player.get(SneakingData.class).isPresent())
        {
            if (homeManager.has(player, "home"))
            {
                Home home = homeManager.findOne(player, "home");
                if (player.getLocation().equals(home.getTransform()))
                {
                    return;
                }
                home.setLocation(player.getTransform());
                home.update();
                i18n.sendTranslated(player, POSITIVE, "Your home has been set!");
            }
            else
            {
                if (this.homeManager.getCount(player) == this.module.getConfig().homes.max)
                {
                    i18n.sendTranslated(player, CRITICAL, "You have reached your maximum number of homes!");
                    i18n.sendTranslated(player, NEGATIVE, "You have to delete a home to make a new one");
                    return;
                }
                homeManager.create(player, "home", player.getTransform(), false);
                i18n.sendTranslated(player, POSITIVE, "Your home has been created!");
            }
            event.setCancelled(true);
        }
    }
}
