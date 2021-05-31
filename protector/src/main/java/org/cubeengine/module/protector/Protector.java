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

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.protector.command.RegionCommands;
import org.cubeengine.module.protector.listener.BlockSettingsListener;
import org.cubeengine.module.protector.listener.DamageSettingsListener;
import org.cubeengine.module.protector.listener.InteractSettingsListener;
import org.cubeengine.module.protector.listener.MoveSettingsListener;
import org.cubeengine.module.protector.listener.SettingsListener;
import org.cubeengine.module.protector.listener.SpawnSettingsListener;
import org.cubeengine.module.protector.region.RegionFormatter;
import org.cubeengine.module.zoned.event.ZoneEvent;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.service.context.ContextService;
import org.spongepowered.api.service.permission.PermissionService;

// TODO fill/empty bucket (in hand)
// TNT can be ignited ( but no world change )
// TNT/Creeper does no damage to player?

@Module
public class Protector
{
    @Inject private Logger logger;
    @Inject private I18n i18n;
    @InjectService private ContextService ps;
    @Inject private ModuleManager mm;
    @ModuleCommand private RegionCommands regionCommands;

    @ModuleListener private BlockSettingsListener blockSettingsListener;
    @ModuleListener private DamageSettingsListener damageSettingsListener;
    @ModuleListener private InteractSettingsListener interactSettingsListener;
    @ModuleListener private MoveSettingsListener moveSettingsListener;
    @ModuleListener private SettingsListener settingsListener;
    @ModuleListener private SpawnSettingsListener spawnSettingsListener;

    @Inject private RegionManager manager;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        ps.registerContextCalculator(new RegionContextCalculator(manager));
        i18n.getCompositor().registerFormatter(new RegionFormatter());

        manager.reload();
        logger.info("{} Regions loaded", manager.getRegionCount());
    }

    @Listener
    public void onChangeZone(ZoneEvent event)
    {
        manager.reload();
    }
}
