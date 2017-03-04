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
package org.cubeengine.module.protector;

import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.command.RegionCommands;
import org.cubeengine.module.protector.command.SettingsCommands;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.service.permission.PermissionService;

import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;

@ModuleInfo(name = "Protector", description = "Protects your worlds")
public class Protector extends Module
{
    @Inject private Path modulePath;
    @Inject private Reflector reflector;
    @Inject private PermissionManager pm;
    @Inject private I18n i18n;
    @Inject private PermissionService ps;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private Selector selector;

    private RegionManager manager;

    @Enable
    public void onEnable()
    {
        manager = new RegionManager(modulePath, reflector);
        ps.registerContextCalculator(new RegionContextCalculator(manager));
        RegionCommands regionCmd = new RegionCommands(cm, selector, manager, i18n);
        cm.addCommand(regionCmd);
        SettingsCommands settingsCmd = new SettingsCommands(manager, i18n, ps, pm, em, cm);
        regionCmd.addCommand(settingsCmd);
        em.registerListener(Protector.class, this);
    }

    @Listener
    public void onServerStarted(GameStartingServerEvent event)
    {
        getProvided(Log.class).info("Loading Regions...");
        manager.reload();
        getProvided(Log.class).info("{} Regions loaded", manager.getRegionCount());
    }
}
