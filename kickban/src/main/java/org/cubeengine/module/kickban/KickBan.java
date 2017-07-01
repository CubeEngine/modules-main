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
package org.cubeengine.module.kickban;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.command.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Overrides and improves Vanilla Kick and Ban Commands:
 *
 * /ban 	Adds player to banlist.
 * /ban-ip 	Adds IP address to banlist.
 * /banlist Displays banlist.
 * /kick 	Kicks a player off a server.
 * /pardon 	Removes entries from the banlist.
 */
@Singleton
@Module(id = "kickban", name = "KickBan", version = "1.0.0",
        description = "Kick and Ban players",
        dependencies = @Dependency("cubeengine-core"),
        url = "http://cubeengine.org",
        authors = {"Anselm 'Faithcaio' Brehme", "Phillip Schichtel"})
public class KickBan extends CubeEngineModule
{
    @Inject private KickBanPerms perms;
    @ModuleConfig private KickBanConfig config;
    @ModuleCommand private KickBanCommands kickBanCommands;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        kickBanCommands.init();
    }

    public KickBanPerms perms()
    {
        return perms;
    }

    public KickBanConfig getConfiguration()
    {
        return config;
    }
}
