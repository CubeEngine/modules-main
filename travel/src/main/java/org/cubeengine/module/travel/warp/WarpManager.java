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
package org.cubeengine.module.travel.warp;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.config.Warp;
import org.cubeengine.module.travel.config.WarpConfig;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.server.ServerWorld;

@Singleton
public class WarpManager
{
    private WarpConfig config;

    @Inject
    public WarpManager(Reflector reflector, FileManager fm)
    {
        final File confFile = fm.getModulePath(Travel.class)
                .resolve("warps.yml")
                .toFile();
        this.config = reflector.load(WarpConfig.class, confFile);
    }

    public Warp create(User owner, String name, ServerWorld world, Transform transform)
    {
        if (this.has(name))
        {
            throw new IllegalArgumentException("Tried to create duplicate warp!");
        }

        Warp warp = new Warp();
        warp.name = name;
        warp.owner = owner.uniqueId();
        warp.transform = transform;
        warp.world = new ConfigWorld(world);

        config.warps.add(warp);
        config.save();
        return warp;
    }

    public void delete(Warp warp)
    {
        config.warps.remove(warp);
        config.save();
    }

    public boolean has(String name)
    {
        return get(name).isPresent();
    }

    public Optional<Warp> get(String name)
    {
        return config.warps.stream().filter(warp -> warp.name.equalsIgnoreCase(name)).findFirst();
    }

    public long getCount()
    {
        return config.warps.size();
    }

    public void save()
    {
        config.save();
    }

    public boolean rename(Warp point, String name)
    {
        if (has(name))
        {
            return false;
        }

        point.name = name;
        save();
        return true;
    }

    public Set<Warp> list(@Nullable User user)
    {
        return config.warps.stream()
                           .filter(warp -> user == null || warp.owner.equals(user.uniqueId()))
                           .collect(Collectors.toSet());
    }

    public void massDelete(Predicate<Warp> predicate)
    {
        config.warps = config.warps.stream().filter(predicate).collect(Collectors.toList());
        save();
    }
    
}
