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
package de.cubeisland.engine.module.travel.home;

import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.service.user.UserManager;
import de.cubeisland.engine.service.world.WorldManager;
import de.cubeisland.engine.module.travel.Travel;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.manipulator.entity.SneakingData;
import org.spongepowered.api.entity.EntityInteractionTypes;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerInteractBlockEvent;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static org.spongepowered.api.event.Order.EARLY;

public class HomeListener
{
    private final Travel module;
    private UserManager um;
    private WorldManager wm;
    private final HomeManager homeManager;

    public HomeListener(Travel module, UserManager um, WorldManager wm)
    {
        this.module = module;
        this.um = um;
        this.wm = wm;
        this.homeManager = module.getHomeManager();
    }

    @Subscribe(order = EARLY)
    public void rightClickBed(PlayerInteractBlockEvent event)
    {
        if (event.getInteractionType() != EntityInteractionTypes.USE || event.getBlock().getBlockType() != BlockTypes.BED)
        {
            return;
        }
        Player player = event.getUser();
        User user = um.getExactUser(player.getUniqueId());
        if (player.getData(SneakingData.class).isPresent())
        {
            if (homeManager.has(user, "home"))
            {
                Home home = homeManager.findOne(user, "home");
                if (player.getLocation().equals(home.getLocation()))
                {
                    return;
                }
                home.setLocation(player.getLocation(), player.getRotation(), wm);
                home.update();
                user.sendTranslated(POSITIVE, "Your home has been set!");
            }
            else
            {
                if (this.homeManager.getCount(user) == this.module.getConfig().homes.max)
                {
                    user.sendTranslated(CRITICAL, "You have reached your maximum number of homes!");
                    user.sendTranslated(NEGATIVE, "You have to delete a home to make a new one");
                    return;
                }
                homeManager.create(user, "home", player.getLocation(), player.getRotation(), false);
                user.sendTranslated(POSITIVE, "Your home has been created!");
            }
            event.setCancelled(true);
        }
    }
}
