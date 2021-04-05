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
package org.cubeengine.module.zoned;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.math.vector.Vector3d;

public class ShapeRenderer
{
    private static final Map<UUID, ScheduledTask> showRegionTasks = new HashMap<>();

    public static boolean toggleShowActiveZone(TaskManager tm, ServerPlayer player, Function<ServerPlayer, ZoneConfig> zoneProvider)
    {

        final ScheduledTask oldTask = showRegionTasks.remove(player.uniqueId());
        if (oldTask != null)
        {
            hideActiveZone(player);
            return false;
        }
        showActiveZone(tm, player, zoneProvider);
        return true;
    }

    public static boolean hideActiveZone(ServerPlayer player)
    {
        final ScheduledTask oldTask = showRegionTasks.remove(player.uniqueId());
        if (oldTask != null)
        {
            oldTask.cancel();
            return true;
        }
        return false;
    }

    public static boolean showActiveZone(TaskManager tm, ServerPlayer player, Function<ServerPlayer, ZoneConfig> zoneProvider)
    {
        if (showRegionTasks.containsKey(player.uniqueId()))
        {
            return false;
        }
        final ScheduledTask newTask = tm.runTimer(t -> ShapeRenderer.showActiveRegion0(player.uniqueId(), zoneProvider.apply(player)),
                                                  Ticks.of(10), Ticks.of(10));
        showRegionTasks.put(player.uniqueId(), newTask);
        return true;
    }

    public static boolean isShowingActiveZone(ServerPlayer player)
    {
        return showRegionTasks.containsKey(player.uniqueId());
    }

    private static void showActiveRegion0(UUID playerUuid, ZoneConfig zone)
    {
        final ServerPlayer player = Sponge.server().player(playerUuid).orElse(null);
        if (player == null)
        {
            ScheduledTask task = showRegionTasks.remove(playerUuid);
            if (task != null)
            {
                task.cancel();
            }
            return;
        }

        if (zone == null || !zone.isComplete())
        {
            return; // Skip no complete zone selected
        }

        Cuboid cuboid = ((Cuboid)zone.shape); // TODO other shapes
        Vector3d mmm = cuboid.getMinimumPoint().add(-0.5, -0.5, -0.5);
        Vector3d xxx = cuboid.getMaximumPoint().add(0.5, 0.5, 0.5);

        Vector3d mmx = new Vector3d(mmm.getX(), mmm.getY(), xxx.getZ());
        Vector3d mxx = new Vector3d(mmm.getX(), xxx.getY(), xxx.getZ());
        Vector3d xmm = new Vector3d(xxx.getX(), mmm.getY(), mmm.getZ());
        Vector3d xxm = new Vector3d(xxx.getX(), xxx.getY(), mmm.getZ());

        Vector3d mxm = new Vector3d(mmm.getX(), xxx.getY(), mmm.getZ());
        Vector3d xmx = new Vector3d(xxx.getX(), mmm.getY(), xxx.getZ());

        Map<Color, List<Vector3d>> particles = new HashMap<>();
        List<Vector3d> red = new ArrayList<>();
        particles.put(Color.RED, red);
        int stepX = ((int)mmm.distance(xmm)) * 5;
        linePoints(red, mmm, xmm, stepX);
        linePoints(red, mmx, xmx, stepX);
        linePoints(red, xxx, mxx, stepX);
        linePoints(red, xxm, mxm, stepX);

        List<Vector3d> green = new ArrayList<>();
        particles.put(Color.GREEN, green);
        int stepY = ((int)mmm.distance(mxm)) * 5;
        linePoints(green, mmm, mxm, stepY);
        linePoints(green, mmx, mxx, stepY);
        linePoints(green, xxx, xmx, stepY);
        linePoints(green, xxm, xmm, stepY);

        List<Vector3d> blue = new ArrayList<>();
        particles.put(Color.BLUE, blue);
        int stepZ = ((int)mmm.distance(mmx)) * 5;
        linePoints(blue, mmm, mmx, stepZ);
        linePoints(blue, mxx, mxm, stepZ);
        linePoints(blue, xxx, xxm, stepZ);
        linePoints(blue, xmx, xmm, stepZ);

        draw(player, new Vector3d(0.5, 0.5, 0.5), particles);
    }

    private static void linePoints(List<Vector3d> list, Vector3d point, Vector3d point2, int steps)
    {
        Vector3d move = point2.sub(point).div(steps);
        for (int step = 0; step < steps; step++)
        {
            point = point.add(move);
            list.add(point);
        }
    }

    private static void draw(Player player, Vector3d position, Map<Color, List<Vector3d>> particles)
    {
        for (Map.Entry<Color, List<Vector3d>> entry : particles.entrySet())
        {
            for (Vector3d vec : entry.getValue())
            {
                player.spawnParticles(ParticleEffect.builder().type(ParticleTypes.DUST).option(ParticleOptions.COLOR, entry.getKey()).build(), position.add(vec));
            }
        }
    }
}
