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
package org.cubeengine.module.locker;

import javax.inject.Inject;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.module.locker.commands.LockerAdminCommands;
import org.cubeengine.module.locker.commands.LockerCommands;
import org.cubeengine.module.locker.commands.LockerCreateCommands;
import org.cubeengine.module.locker.commands.PlayerAccess;
import org.cubeengine.module.locker.config.BlockLockConfig;
import org.cubeengine.module.locker.config.BlockLockConfig.BlockLockerConfigConverter;
import org.cubeengine.module.locker.config.EntityLockConfig;
import org.cubeengine.module.locker.config.EntityLockConfig.EntityLockerConfigConverter;
import org.cubeengine.module.locker.data.ImmutableLockerData;
import org.cubeengine.module.locker.data.LockerData;
import org.cubeengine.module.locker.data.LockerDataBuilder;
import org.cubeengine.module.locker.storage.LockManager;
import org.cubeengine.module.locker.storage.TableAccessList;
import org.cubeengine.module.locker.storage.TableLockLocations;
import org.cubeengine.module.locker.storage.TableLocks;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.database.ModuleTables;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Game;

// TODO protect lines of redstone

@ModuleInfo(name = "Locker", description = "Puts a Lock on your stuff")
@ModuleTables({TableLocks.class, TableLockLocations.class, TableAccessList.class})
public class Locker extends Module
{
    @ModuleConfig private LockerConfig config;
    private LockerPerm perms;

    private LockManager manager;
    private Log logger;

    @Inject private Database db;
    @Inject private CommandManager cm;
    @Inject private EventManager em;

    @Inject private StringMatcher sm;

    @Inject private Game game;
    @Inject private TaskManager tm;
    @Inject private I18n i18n;

    @Inject
    public Locker(Game game, Reflector reflector, EntityMatcher entityMatcher, Log logger, MaterialMatcher mm)
    {
        this.logger = logger;
        game.getDataManager().register(LockerData.class, ImmutableLockerData.class,
                                                    new LockerDataBuilder(game.getRegistry().getValueFactory()));

        ConverterManager cManager = reflector.getDefaultConverterManager();
        cManager.registerConverter(new BlockLockerConfigConverter(logger, mm), BlockLockConfig.class);
        cManager.registerConverter(new EntityLockerConfigConverter(logger, entityMatcher), EntityLockConfig.class);
    }


    @Enable
    public void onEnable()
    {
        cm.getProviderManager().register(this, new PlayerAccess.PlayerAccessReader(game), PlayerAccess.class);

        manager = new LockManager(this, em, sm, db, tm, i18n, game);
        LockerCommands lockerCmd = new LockerCommands(this, manager, i18n, sm);
        cm.addCommand(lockerCmd);
        lockerCmd.addCommand(new LockerCreateCommands(this, manager, i18n));
        lockerCmd.addCommand(new LockerAdminCommands(this, manager, i18n));
        perms = new LockerPerm(this, lockerCmd);
        em.registerListener(this, new LockerListener(this, manager, i18n));
        em.registerListener(this, new LockerBlockListener(this, manager, i18n, game));
    }

    @Disable
    public void onDisable()
    {
        this.manager.saveAll();
    }

    public void reload()
    {
        // TODO add possibility in modularity
        // full reload is impossible because of Sponge not allowing registration of DataBuilders after bootup
        /*
        this.onDisable();
        this.config = this.loadConfig(LockerConfig.class);
        Database db = this.getCore().getDB();
        db.registerTable(TableLocks.class);
        db.registerTable(TableLockLocations.class);
        db.registerTable(TableAccessList.class);
        manager = new LockManager(this);
        LockerCommands lockerCmd = new LockerCommands(this, manager);
        this.getCore().getCommandManager().addCommand(lockerCmd);
        lockerCmd.addCommand(new LockerCreateCommands(this, manager));
        lockerCmd.addCommand(new LockerAdminCommands(this, manager));
        listener = new LockerListener(this, manager);
        */
    }

    public LockerConfig getConfig()
    {
        return this.config;
    }

    public Game getGame()
    {
        return game;
    }

    public TaskManager getTaskManager()
    {
        return tm;
    }

    public LockerPerm perms()
    {
        return perms;
    }


    /*
    Features:
      lock leashknot / or fence from leashing on it
    lock beacon effects?
    golden masterKey for Admin/Mod to open all chests (explode in hand if no perm)
    masterKeys to open all chests of one user
    multiKeys to open multiple chests
    buttons to door-protection to open door for x-sec = autoclose time BUT deny redstone so ONLY that button can open the door/doubledoor
    implement usage of separate access-level for in & out in containers
    name a protection
     */

}

