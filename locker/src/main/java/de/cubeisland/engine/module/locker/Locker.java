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
package de.cubeisland.engine.module.locker;

import javax.inject.Inject;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.service.filesystem.FileManager;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.core.util.matcher.EntityMatcher;
import de.cubeisland.engine.module.core.util.matcher.MaterialMatcher;
import de.cubeisland.engine.module.core.util.matcher.StringMatcher;
import de.cubeisland.engine.module.locker.BlockLockerConfiguration.BlockLockerConfigConverter;
import de.cubeisland.engine.module.locker.EntityLockerConfiguration.EntityLockerConfigConverter;
import de.cubeisland.engine.module.locker.commands.LockerAdminCommands;
import de.cubeisland.engine.module.locker.commands.LockerCommands;
import de.cubeisland.engine.module.locker.commands.LockerCreateCommands;
import de.cubeisland.engine.module.locker.storage.LockManager;
import de.cubeisland.engine.module.locker.storage.TableAccessList;
import de.cubeisland.engine.module.locker.storage.TableLockLocations;
import de.cubeisland.engine.module.locker.storage.TableLocks;
import de.cubeisland.engine.service.command.CommandManager;
import de.cubeisland.engine.service.database.Database;
import de.cubeisland.engine.service.task.TaskManager;
import de.cubeisland.engine.service.user.UserManager;
import de.cubeisland.engine.reflect.Reflector;
import org.spongepowered.api.Game;

public class Locker extends Module
{
    private LockerConfig config;
    private LockManager manager;
    private LockerListener listener;


    public LockerPerm perms()
    {
        return perms;
    }

    private LockerPerm perms;

    @Inject private Reflector reflector;
    @Inject private FileManager fm;
    @Inject private Database db;
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private UserManager um;
    @Inject private MaterialMatcher mm;
    @Inject private Log logger;
    @Inject private StringMatcher sm;
    @Inject private EntityMatcher entityMatcher;
    @Inject private Game game;
    @Inject private TaskManager tm;


    @Enable
    public void onEnable()
    {
        ConverterManager cManager = reflector.getDefaultConverterManager();
        cManager.registerConverter(new BlockLockerConfigConverter(logger, mm), BlockLockerConfiguration.class);
        cManager.registerConverter(new EntityLockerConfigConverter(logger, entityMatcher), EntityLockerConfiguration.class);
        this.config = fm.loadConfig(this, LockerConfig.class);
        db.registerTable(TableLocks.class);
        db.registerTable(TableLockLocations.class);
        db.registerTable(TableAccessList.class);
        manager = new LockManager(this, em, sm);
        LockerCommands lockerCmd = new LockerCommands(this, manager, um);
        cm.addCommand(lockerCmd);
        lockerCmd.addCommand(new LockerCreateCommands(this, manager));
        lockerCmd.addCommand(new LockerAdminCommands(this, manager));
        perms = new LockerPerm(this, lockerCmd);
        listener = new LockerListener(this, manager, um);
        em.registerListener(this, listener);
    }

    @Disable
    public void onDisable()
    {
        em.removeListeners(this);
        cm.removeCommands(this);
        this.manager.saveAll();
    }

    public void reload()
    {
        // TODO add possibility in modularity
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

    public UserManager getUserManager()
    {
        return um;
    }

    public TaskManager getTaskManager()
    {
        return tm;
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

