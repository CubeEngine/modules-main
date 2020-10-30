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
package org.cubeengine.module.travel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.travel.config.TravelConfig;
import org.cubeengine.module.travel.home.HomeCommand;
import org.cubeengine.module.travel.warp.WarpCommand;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;

@Singleton
@Module(dependencies = @Dependency(value = "cubeengine-zoned", optional = true))
public class Travel
{
    @ModuleConfig private TravelConfig config;

    @Inject private I18n i18n;

    @ModuleCommand private HomeCommand homeCommand;
    @ModuleCommand private WarpCommand warpCommand;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        i18n.getCompositor().registerFormatter(new TpPointFormatter(i18n));
    }

    public TravelConfig getConfig()
    {
        return this.config;
    }
}
