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


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Maybe;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.multiverse.player.ImmutableMultiverseData;
import org.cubeengine.module.multiverse.player.MultiverseData;
import org.cubeengine.module.multiverse.player.MultiverseDataBuilder;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.world.World;

/**
 * Group Worlds into Universes
 * <p>
 * Mass konfigurate worlds (in that universe)
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
@ModuleInfo(name = "Multiverse", description = "Group worlds into universes")
public class Multiverse extends Module
{
    public static final String UNKNOWN = "unknown";
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private I18n i18n;
    @Inject private Log log;
    @Inject private PluginContainer plugin;

    @ModuleConfig private MultiverseConfig config;

    @Inject
    public Multiverse(PluginContainer plugin)
    {
        DataRegistration<MultiverseData, ImmutableMultiverseData> dr = DataRegistration.<MultiverseData, ImmutableMultiverseData>builder()
                .dataClass(MultiverseData.class).immutableClass(ImmutableMultiverseData.class)
                .builder(new MultiverseDataBuilder()).manipulatorId("multiverse")
                .dataName("CubeEngine Multiverse Data")
                .buildAndRegister(plugin);

        Sponge.getDataManager().registerLegacyManipulatorIds(MultiverseData.class.getName(), dr);
    }

    @Enable
    @Inject
    public void onEnable(Maybe<PermissionService> ps) throws IOException
    {
        cm.addCommand(new MultiverseCommands(cm, this, i18n));
        em.registerListener(Multiverse.class, new MultiverseListener(this));
        ps.onAvailable(s -> s.registerContextCalculator(new MultiverseContextCalculator(this)));
    }

    public MultiverseConfig getConfig()
    {
        return config;
    }

    public String getUniverse(World world)
    {
        for (Entry<String, Set<ConfigWorld>> entry : config.universes.entrySet())
        {
            for (ConfigWorld cWorld : entry.getValue())
            {
                if (world.equals(cWorld.getWorld()))
                {
                    return entry.getKey();
                }
            }
        }

        if (config.autoDetectUnivserse)
        {
            if (world.getName().contains("_"))
            {
                String name = world.getName().substring(0, world.getName().indexOf("_"));
                Set<ConfigWorld> list = config.universes.get(name);
                if (list == null)
                {
                    list = new HashSet<>();
                    config.universes.put(name, list);
                }
                list.add(new ConfigWorld(world));
                log.info("Added {} to the universe {}", world.getName(), name);
                config.save();
                return name;
            }
        }

        Set<ConfigWorld> list = config.universes.get(UNKNOWN);
        if (list == null)
        {
            list = new HashSet<>();
            config.universes.put(UNKNOWN, list);
        }
        list.add(new ConfigWorld(world));
        log.info("Added {} to the universe {}", world.getName(), UNKNOWN);
        config.save();
        return UNKNOWN;
    }

    public void setUniverse(World world, String universe)
    {
        ConfigWorld cWorld = new ConfigWorld(world);
        config.universes.values().forEach(set -> set.remove(cWorld));
        Set<ConfigWorld> set = config.universes.get(universe);
        if (set == null)
        {
            set = new HashSet<>();
            config.universes.put(universe, set);
        }
        set.add(cWorld);
        config.save();
    }
}
