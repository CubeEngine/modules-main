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
package org.cubeengine.module.teleport;

import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.teleport.command.MovementCommands;
import org.cubeengine.module.teleport.command.SpawnCommands;
import org.cubeengine.module.teleport.command.TeleportCommands;
import org.cubeengine.module.teleport.command.TeleportRequestCommands;
import org.cubeengine.processor.Module;

/**
 * /setworldspawn 	Sets the world spawn.
 * /spawnpoint 	Sets the spawn point for a player.
 * /tp 	Teleports entities.
 */
@Singleton
@Module
public class Teleport
{
    // TODO make override of vanilla-commands optional

    @ModuleConfig private TeleportConfiguration config;

    @ModuleCommand private MovementCommands movementCommands;
    @ModuleCommand private SpawnCommands spawnCommands;
    @ModuleCommand private TeleportCommands teleportCommands;
    @ModuleCommand private TeleportRequestCommands teleportRequestCommands;

    @ModuleListener private TeleportListener teleportListener;

    public TeleportConfiguration getConfig()
    {
        return config;
    }
}
