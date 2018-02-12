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
package org.cubeengine.module.protector;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.protector.command.RegionCommands;
import org.cubeengine.module.protector.command.SettingsCommands;
import org.cubeengine.module.protector.region.RegionFormatter;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.service.permission.PermissionService;

import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Singleton;

// TODO fill/empty bucket (in hand)
// TNT can be ignited ( but no world change )
// TNT/Creeper does no damage to player?

@Singleton
@Module
public class Protector extends CubeEngineModule
{
    private Path modulePath;
    @Inject private Reflector reflector;
    @Inject private PermissionManager pm;
    @Inject private I18n i18n;
    @InjectService private PermissionService ps;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @InjectService private Selector selector;
    @Inject private ModuleManager mm;
    @Inject private TaskManager tm;
    private Log logger;

    private RegionManager manager;

    @Listener
    public void onEnable(GamePostInitializationEvent event)
    {
        this.logger = mm.getLoggerFor(Protector.class);
        this.modulePath = mm.getPathFor(Protector.class);
        manager = new RegionManager(modulePath, reflector, logger);
        ps.registerContextCalculator(new RegionContextCalculator(manager));
        RegionCommands regionCmd = new RegionCommands(cm, selector, manager, i18n, tm);
        i18n.getCompositor().registerFormatter(new RegionFormatter());
        cm.addCommand(regionCmd);
        SettingsCommands settingsCmd = new SettingsCommands(manager, i18n, ps, pm, em, cm);
        regionCmd.addCommand(settingsCmd);
    }

    @Listener
    public void onServerStarted(GameStartedServerEvent event)
    {
        manager.reload();
        mm.getLoggerFor(Protector.class).info("{} Regions loaded", manager.getRegionCount());
    }
}
