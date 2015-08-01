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
package de.cubeisland.engine.module.travel;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.Disable;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Maybe;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.service.filesystem.FileManager;
import de.cubeisland.engine.service.i18n.I18n;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.service.Selector;
import de.cubeisland.engine.service.command.CommandManager;
import de.cubeisland.engine.module.core.util.Profiler;
import de.cubeisland.engine.service.database.Database;
import de.cubeisland.engine.service.permission.PermissionManager;
import de.cubeisland.engine.service.user.UserManager;
import de.cubeisland.engine.service.world.WorldManager;
import de.cubeisland.engine.module.travel.home.HomeCommand;
import de.cubeisland.engine.module.travel.home.HomeListener;
import de.cubeisland.engine.module.travel.home.HomeManager;
import de.cubeisland.engine.module.travel.storage.TableInvite;
import de.cubeisland.engine.module.travel.storage.TableTeleportPoint;
import de.cubeisland.engine.module.travel.warp.WarpCommand;
import de.cubeisland.engine.module.travel.warp.WarpManager;

@ModuleInfo(name = "Travel", description = "Travel anywhere")
public class Travel extends Module
{
    private TravelConfig config;

    private InviteManager inviteManager;
    private HomeManager homeManager;
    private WarpManager warpManager;

    private TravelPerm permissions;

    @Inject private FileManager fm;
    @Inject private Database db;
    @Inject private Log logger;
    @Inject private UserManager um;
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private PermissionManager pm;
    @Inject private WorldManager wm;
    @Inject private Maybe<Selector> selector;
    @Inject private I18n i18n;

    @Enable
    public void onEnable()
    {
        this.config = fm.loadConfig(this, TravelConfig.class);
        db.registerTable(TableTeleportPoint.class);
        db.registerTable(TableInvite.class);

        i18n.getCompositor().registerFormatter(new TpPointFormatter(i18n));

        Profiler.startProfiling("travelEnable");
        logger.trace("Loading TeleportPoints...");
        this.inviteManager = new InviteManager(db, this, um);
        this.homeManager = new HomeManager(this, this.inviteManager, db, pm, wm, um);
        this.homeManager.load();
        this.warpManager = new WarpManager(this, this.inviteManager, db, pm, wm, um);
        this.warpManager.load();
        logger.trace("Loaded TeleportPoints in {} ms", Profiler.endProfiling("travelEnable", TimeUnit.MILLISECONDS));

        HomeCommand homeCmd = new HomeCommand(this, selector, i18n, um, wm);
        cm.addCommand(homeCmd);
        WarpCommand warpCmd = new WarpCommand(this, um, wm);
        cm.addCommand(warpCmd);
        em.registerListener(this, new HomeListener(this, um, wm));

        this.permissions = new TravelPerm(this);
    }

    @Disable
    public void onDisable()
    {
        cm.removeCommands(this);
        em.removeListeners(this);
        pm.cleanup(this);
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
