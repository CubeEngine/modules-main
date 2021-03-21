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
package org.cubeengine.module.travel.config;

import java.util.UUID;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.reflect.Section;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileManager;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.server.ServerWorld;

public class TeleportPoint implements Section
{
    public String name;

    public UUID owner;
    public ConfigWorld world;
    public Transform transform;

    public String welcomeMsg;

    public void setTransform(ServerWorld world, Transform transform)
    {
        this.transform = transform;
        this.world = new ConfigWorld(world);
    }

    public GameProfile getOwner()
    {
        final GameProfileManager manager = Sponge.server().gameProfileManager();
        return manager.basicProfile(this.owner).join();
    }

    public boolean isOwner(User user)
    {
        return this.owner.equals(user.uniqueId());
    }

    public boolean isOwner(CommandCause cause)
    {
        return cause.audience() instanceof ServerPlayer && this.owner.equals(((ServerPlayer)cause.audience()).uniqueId());
    }
}
