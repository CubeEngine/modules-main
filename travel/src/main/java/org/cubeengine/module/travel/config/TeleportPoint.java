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
package org.cubeengine.module.travel.config;

import java.util.UUID;
import de.cubeisland.engine.reflect.Section;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.config.WorldTransform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.World;

public class TeleportPoint implements Section
{
    public String name;

    public UUID owner;
    public ConfigWorld world;
    public WorldTransform transform;

    public String welcomeMsg;

    public void setTransform(Transform<World> transform)
    {
        this.transform = new WorldTransform(transform.getLocation(), transform.getRotation());
        this.world = new ConfigWorld(transform.getExtent());
    }

    public User getOwner()
    {
        return Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(owner).orElse(null);
    }

    public boolean isOwner(CommandSource cmdSource)
    {
        return getOwner().getIdentifier().equals(cmdSource.getIdentifier());
    }
}
