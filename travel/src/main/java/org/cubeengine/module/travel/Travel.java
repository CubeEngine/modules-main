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
package org.cubeengine.module.travel;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Maybe;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.core.util.Profiler;
import org.cubeengine.module.travel.home.HomeCommand;
import org.cubeengine.module.travel.home.HomeListener;
import org.cubeengine.module.travel.home.HomeManager;
import org.cubeengine.module.travel.storage.TableInvite;
import org.cubeengine.module.travel.storage.TableTeleportPoint;
import org.cubeengine.module.travel.warp.WarpCommand;
import org.cubeengine.module.travel.warp.WarpManager;
import org.cubeengine.service.Selector;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.database.ModuleTables;
import org.cubeengine.service.event.EventManager;
import org.cubeengine.service.filesystem.ModuleConfig;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.ModulePermissions;
import org.cubeengine.service.permission.PermissionManager;

@ModuleInfo(name = "Travel", description = "Travel anywhere")
@ModuleTables({TableTeleportPoint.class, TableInvite.class})
public class Travel extends Module
{
    @ModuleConfig private TravelConfig config;
    @ModulePermissions private TravelPerm permissions;

    private InviteManager inviteManager;
    private HomeManager homeManager;
    private WarpManager warpManager;

    @Inject private Database db;
    @Inject private Log logger;
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private PermissionManager pm;
    @Inject private Maybe<Selector> selector;
    @Inject private I18n i18n;

    @Enable
    public void onEnable()
    {
        i18n.getCompositor().registerFormatter(new TpPointFormatter(i18n));

        Profiler.startProfiling("travelEnable");
        logger.trace("Loading TeleportPoints...");
        this.inviteManager = new InviteManager(db, this);
        this.homeManager = new HomeManager(this, this.inviteManager, db, pm);
        this.homeManager.load();
        this.warpManager = new WarpManager(this, this.inviteManager, db, pm);
        this.warpManager.load();
        logger.trace("Loaded TeleportPoints in {} ms", Profiler.endProfiling("travelEnable", TimeUnit.MILLISECONDS));

        HomeCommand homeCmd = new HomeCommand(this, selector, i18n);
        cm.addCommand(homeCmd);
        WarpCommand warpCmd = new WarpCommand(this, i18n);
        cm.addCommand(warpCmd);
        em.registerListener(this, new HomeListener(this, i18n));
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

    public InviteManager getInviteManager()
    {
        return this.inviteManager;
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
