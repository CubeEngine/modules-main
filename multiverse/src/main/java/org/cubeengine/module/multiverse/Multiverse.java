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
package org.cubeengine.module.multiverse;


import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.multiverse.player.MultiverseData;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * Group Worlds into Universes
 * <p>
 * Mass configurate worlds (in that universe)
 * <p>
 * Separate PlayerData(Inventories) for universes /
 * Block plugin teleports of entites with inventories / alternative save entity inventory and reload later? using custom data
 * <p>
 * ContextProvider for universe (allowing permissions per universe)
 * <p>
 * permissions for universe access
 *
 * wait for Sponge Inventory or better Data impl of setRawData(..)
 * save all data i want in custom manipulator (Map:World->PlayerData)
 */
@Singleton
@Module
public class Multiverse
{
    public static final String UNKNOWN_UNIVERSE_NAME = "unknown";
    @Inject private Log log;
    @InjectService private PermissionService ps;

    @ModuleConfig private MultiverseConfig config;
    @ModuleCommand private MultiverseCommands multiverseCommands;
    @ModuleListener private MultiverseListener listener;

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        MultiverseData.register(event);
    }


    @Listener
    public void onPostInit(StartedEngineEvent<Server> event)
    {
        ps.registerContextCalculator(new MultiverseContextCalculator(this));
    }

    public MultiverseConfig getConfig()
    {
        return config;
    }

    public String getUniverse(ServerWorld world)
    {
        for (Entry<String, Set<ConfigWorld>> entry : config.universes.entrySet())
        {
            for (ConfigWorld cWorld : entry.getValue())
            {
                if (world.key().asString().equals(cWorld.getName()))
                {
                    return entry.getKey();
                }
            }
        }

        if (config.autoDetectUnivserse)
        {
            final String worldKeyValue = world.key().value();
            if (worldKeyValue.contains("_"))
            {
                String name = worldKeyValue.substring(0, worldKeyValue.indexOf("_"));
                return saveInUniverse(new ConfigWorld(world), name);
            }
        }

        return saveInUniverse(new ConfigWorld(world), UNKNOWN_UNIVERSE_NAME);
    }

    public String saveInUniverse(ConfigWorld cWorld, String name)
    {
        Set<ConfigWorld> set = config.universes.computeIfAbsent(name, k -> new HashSet<>());
        set.add(cWorld);
        log.info("Added {} to the universe {}", cWorld.getName(), name);
        config.save();
        return name;
    }

    public void setUniverse(ConfigWorld cWorld, String universe)
    {
        config.universes.values().forEach(set -> set.remove(cWorld));
        Set<ConfigWorld> set = config.universes.computeIfAbsent(universe, k -> new HashSet<>());
        set.add(cWorld);
        config.save();
    }

    public Log getLogger()
    {
        return this.log;
    }
}
