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

import java.nio.file.Path;
import javax.inject.Inject;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Maybe;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.travel.config.Home;
import org.cubeengine.module.travel.config.Warp;
import org.cubeengine.module.travel.home.HomeCompleter;
import org.cubeengine.module.travel.warp.WarpCompleter;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.module.travel.config.HomeConfig;
import org.cubeengine.module.travel.config.TravelConfig;
import org.cubeengine.module.travel.config.WarpConfig;
import org.cubeengine.module.travel.home.HomeCommand;
import org.cubeengine.module.travel.home.HomeManager;
import org.cubeengine.module.travel.warp.WarpCommand;
import org.cubeengine.module.travel.warp.WarpManager;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;

@ModuleInfo(name = "Travel", description = "Travel anywhere")
public class Travel extends Module
{
    @ModuleConfig private TravelConfig config;
    @Inject private TravelPerm permissions;

    private HomeManager homeManager;
    private WarpManager warpManager;

    @Inject private Log logger;
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private Maybe<Selector> selector;
    @Inject private I18n i18n;
    @Inject private Reflector reflector;

    @Enable
    public void onEnable()
    {
        i18n.getCompositor().registerFormatter(new TpPointFormatter(i18n));

        this.homeManager = new HomeManager(this, i18n, reflector.load(HomeConfig.class, getProvided(Path.class).resolve("homes.yml").toFile()));
        this.cm.getProviderManager().register(this, new HomeCompleter(homeManager), Home.class);
        this.em.registerListener(Travel.class, this.homeManager);
        this.warpManager = new WarpManager(reflector.load(WarpConfig.class, getProvided(Path.class).resolve("warps.yml").toFile()));
        this.cm.getProviderManager().register(this, new WarpCompleter(warpManager), Warp.class);

        cm.addCommand(new HomeCommand(cm, this, selector, i18n));
        cm.addCommand(new WarpCommand(cm, this, i18n));

        cm.getProviderManager().getExceptionHandler().addHandler(new TravelExceptionHandler(i18n));
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
