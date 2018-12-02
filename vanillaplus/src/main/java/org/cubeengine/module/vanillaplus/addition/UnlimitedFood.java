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
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;

import java.util.concurrent.TimeUnit;

public class UnlimitedFood extends PermissionContainer implements Runnable
{
    private final Permission UNLIMITED_FOOD = register("effect.unlimited-food", "Grants unlimited food", null);

    public UnlimitedFood(PermissionManager pm, PluginContainer plugin)
    {
        super(pm, VanillaPlus.class);
        Task.builder().interval(1, TimeUnit.SECONDS).execute(this).submit(plugin);
    }

    @Override
    public void run()
    {
        for (Player player : Sponge.getServer().getOnlinePlayers())
        {
            if (player.hasPermission(UNLIMITED_FOOD.getId()))
            {
                player.offer(Keys.FOOD_LEVEL, player.getValue(Keys.FOOD_LEVEL).get().getMaxValue());
                player.offer(Keys.SATURATION, player.getValue(Keys.SATURATION).get().getMaxValue());
            }
        }

    }
}
