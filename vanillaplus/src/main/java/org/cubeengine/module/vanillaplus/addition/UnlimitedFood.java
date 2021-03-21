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

import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scheduler.ScheduledTask;

import java.util.function.Consumer;

public class UnlimitedFood extends PermissionContainer implements Consumer<ScheduledTask>
{
    private final Permission UNLIMITED_FOOD = register("effect.unlimited-food", "Grants unlimited food", null);

    public UnlimitedFood(PermissionManager pm, TaskManager tm)
    {
        super(pm, VanillaPlus.class);
    }

    @Override
    public void accept(ScheduledTask scheduledTask) {
        for (ServerPlayer player : Sponge.server().onlinePlayers())
        {
            if (UNLIMITED_FOOD.check(player))
            {
                player.offer(Keys.FOOD_LEVEL, player.get(Keys.MAX_FOOD_LEVEL).get());
                player.offer(Keys.SATURATION, player.get(Keys.MAX_SATURATION).get());
            }
        }
    }
}
