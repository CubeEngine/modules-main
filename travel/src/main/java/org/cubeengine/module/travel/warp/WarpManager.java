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

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.cubeengine.module.travel.config.Warp;
import org.cubeengine.module.travel.config.WarpConfig;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.config.WorldTransform;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.World;

public class WarpManager
{
    private WarpConfig config;

    public WarpManager(WarpConfig config)
    {
        this.config = config;
    }

    public Warp create(Player owner, String name, Transform<World> transform)
    {
        if (this.has(name))
        {
            throw new IllegalArgumentException("Tried to create duplicate warp!");
        }

        Warp warp = new Warp();
        warp.name = name;
        warp.owner = owner.getUniqueId();
        warp.transform = new WorldTransform(transform.getLocation(), transform.getRotation());
        warp.world = new ConfigWorld(transform.getExtent());

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
        return config.warps.stream().filter(warp -> warp.name.equals(name)).findFirst();
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
                           .filter(warp -> user == null || warp.owner.equals(user.getUniqueId()))
                           .collect(Collectors.toSet());
    }

    public void massDelete(Predicate<Warp> predicate)
    {
        config.warps = config.warps.stream().filter(predicate).collect(Collectors.toList());
        save();
    }
    
}
