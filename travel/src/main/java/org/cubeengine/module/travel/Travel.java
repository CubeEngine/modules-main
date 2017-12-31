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

import org.cubeengine.logscribe.Log;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.travel.config.Home;
import org.cubeengine.module.travel.config.HomeConfig;
import org.cubeengine.module.travel.config.TravelConfig;
import org.cubeengine.module.travel.config.Warp;
import org.cubeengine.module.travel.config.WarpConfig;
import org.cubeengine.module.travel.home.HomeCommand;
import org.cubeengine.module.travel.home.HomeCompleter;
import org.cubeengine.module.travel.home.HomeManager;
import org.cubeengine.module.travel.warp.WarpCommand;
import org.cubeengine.module.travel.warp.WarpCompleter;
import org.cubeengine.module.travel.warp.WarpManager;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
public class Travel extends CubeEngineModule
{
    @ModuleConfig private TravelConfig config;
    @Inject private TravelPerm permissions;

    private HomeManager homeManager;
    private WarpManager warpManager;

    private Log logger;
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @com.google.inject.Inject(optional = true) private Selector selector;
    @Inject private I18n i18n;
    @Inject private Reflector reflector;
    @Inject private ModuleManager mm;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        this.logger = mm.getLoggerFor(Travel.class);
        i18n.getCompositor().registerFormatter(new TpPointFormatter(i18n));

        this.homeManager = new HomeManager(this, i18n, reflector.load(HomeConfig.class, mm.getPathFor(Travel.class).resolve("homes.yml").toFile()));
        this.cm.getProviders().register(this, new HomeCompleter(homeManager), Home.class);
        this.em.registerListener(Travel.class, this.homeManager);
        this.warpManager = new WarpManager(reflector.load(WarpConfig.class, mm.getPathFor(Travel.class).resolve("warps.yml").toFile()));
        this.cm.getProviders().register(this, new WarpCompleter(warpManager), Warp.class);

        cm.addCommand(new HomeCommand(cm, this, selector, i18n));
        cm.addCommand(new WarpCommand(cm, this, i18n));

        cm.getProviders().getExceptionHandler().addHandler(new TravelExceptionHandler(i18n));
    }

    public TravelConfig getConfig()
    {
        return this.config;
    }

    public HomeManager getHomeManager()
    {
        return homeManager;
    }

    public WarpManager getWarpManager()
    {
        return warpManager;
    }

    public TravelPerm getPermissions()
    {
        return permissions;
    }

    public Log getLog()
    {
        return logger;
    }
}
