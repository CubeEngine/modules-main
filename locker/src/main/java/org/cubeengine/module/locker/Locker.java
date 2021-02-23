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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.locker.config.BlockLockConfig;
import org.cubeengine.module.locker.config.BlockLockConfig.BlockLockerConfigConverter;
import org.cubeengine.module.locker.config.EntityLockConfig;
import org.cubeengine.module.locker.config.EntityLockConfig.EntityLockerConfigConverter;
import org.cubeengine.module.locker.config.LockerConfig;
import org.cubeengine.module.locker.data.LockerData;
import org.cubeengine.module.locker.data.LockerItems;
import org.cubeengine.module.locker.data.LockerManager;
import org.cubeengine.module.locker.listener.LockerAutoProtectListener;
import org.cubeengine.module.locker.listener.LockerBookListener;
import org.cubeengine.module.locker.listener.LockerLockedListener;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.recipe.RecipeRegistration;

// TODO hoppers and protection
@Singleton
@Module
public class Locker
{
    @ModuleConfig private LockerConfig config;
    @Inject private LockerManager manager;
    @ModuleListener private LockerAutoProtectListener autoProtectListener;
    @ModuleListener private LockerBookListener bookListener;
    @ModuleListener private LockerLockedListener lockedListener;

    @Inject
    public Locker(Reflector reflector, ModuleManager mm)
    {
        ConverterManager cManager = reflector.getDefaultConverterManager();
        final Log logger = mm.getLoggerFor(Locker.class);
        cManager.registerConverter(new BlockLockerConfigConverter(logger), BlockLockConfig.class);
        cManager.registerConverter(new EntityLockerConfigConverter(logger), EntityLockConfig.class);
    }

    @Listener
    public void onRegisterRecipe(RegisterDataPackValueEvent<RecipeRegistration>event)
    {
        LockerItems.registerRecipes(event);
    }

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        LockerData.register(event);
    }


    public LockerConfig getConfig()
    {
        return this.config;
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

