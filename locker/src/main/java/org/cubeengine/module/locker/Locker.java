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
package org.cubeengine.module.locker;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cubeengine.converter.ConverterManager;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.libcube.service.command.ModuleCommand;
import org.cubeengine.libcube.service.event.ModuleListener;
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
import org.cubeengine.libcube.service.database.ModuleTables;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.PluginContainer;

// TODO protect lines of redstone

@Singleton
@Module
@ModuleTables({TableLocks.class, TableLockLocations.class, TableAccessList.class})
public class Locker extends CubeEngineModule
{
    private LockerConfig config;
    @Inject private TaskManager tm;
    @Inject private LockManager manager;
    @ModuleCommand private LockerCommands lockerCmd;
    @ModuleCommand(LockerCommands.class) private LockerCreateCommands lockerCreateCmds;
    @ModuleCommand(LockerCommands.class) private LockerAdminCommands lockerAdminCmds;
    @Inject private LockerPerm perms;
    @ModuleListener private LockerListener listener;
    @ModuleListener private LockerBlockListener blockListener;
    private PluginContainer plugin;
    private Log logger;
    @Inject private InventoryGuardFactory igf;
    @Inject private Reflector reflector;
    @Inject private EntityMatcher entityMatcher;
    @Inject private MaterialMatcher mm;
    @Inject private CommandManager cm;
    @Inject private FileManager fm;

    @Inject
    public Locker(ModuleManager momu)
    {
        this.logger = momu.getLoggerFor(Locker.class);
        this.plugin = momu.getPlugin(Locker.class).get();
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent event)
    {
        DataRegistration<LockerData, ImmutableLockerData> dr =
                DataRegistration.<LockerData, ImmutableLockerData>builder()
                        .dataClass(LockerData.class).immutableClass(ImmutableLockerData.class)
                        .builder(new LockerDataBuilder()).manipulatorId("locker")
                        .dataName("CubeEngine Locker Data")
                        .buildAndRegister(plugin);

        LockerData.LOCK_ID.getQuery();

        Sponge.getDataManager().registerLegacyManipulatorIds(LockerData.class.getName(), dr);


        ConverterManager cManager = reflector.getDefaultConverterManager();
        cManager.registerConverter(new BlockLockerConfigConverter(logger, mm), BlockLockConfig.class);
        cManager.registerConverter(new EntityLockerConfigConverter(logger, entityMatcher), EntityLockConfig.class);

        cm.getProviders().register(this, new PlayerAccess.PlayerAccessParser(), PlayerAccess.class);

        this.config = fm.loadConfig(this, LockerConfig.class);
    }

    @Listener
    public void onDisable(GameStoppingEvent event)
    {
        this.manager.saveAll();
    }

    public LockerConfig getConfig()
    {
        return this.config;
    }

    public TaskManager getTaskManager()
    {
        return tm;
    }

    public LockerPerm perms()
    {
        return perms;
    }

    public PluginContainer getPlugin()
    {
        return plugin;
    }

    public Log getLogger()
    {
        return this.logger;
    }

    public InventoryGuardFactory getInventoryGuardFactory() {
        return igf;
    }

    public LockManager getManager()
    {
        return manager;
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

