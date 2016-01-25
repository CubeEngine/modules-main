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
package org.cubeengine.module.multiverse;

import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.world.ConfigWorld;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Holds multiple parallel universes
 */
public class UniverseManager
{
    private final Multiverse module;
    private Log logger;
    private Reflector reflector;

    private Game game;

    private final Map<String, UniverseConfig> universes = new HashMap<>(); // universeName -> Universe
    private final Map<String, List<WorldProperties>> uWorlds = new HashMap<>(); // universeName -> Worlds
    private final Map<String, UniverseConfig> worlds = new HashMap<>(); // worldName -> belonging to Universe

    private Map<UUID, World> lastDeath = new HashMap<>();

    private final Path dirPlayers;
    private final Path dirUniverses;
    private final Path dirErrors;

    private final PermissionDescription universeRootPerm;

    public UniverseManager(Multiverse module, Path modulePath, Log logger, Reflector reflector, PermissionManager pm) throws IOException
    {
        this.module = module;
        this.logger = logger;
        this.reflector = reflector;
        this.universeRootPerm = pm.register(module, "universe", "", null);

        this.dirPlayers = modulePath.resolve("players"); // config for last world
        Files.createDirectories(this.dirPlayers);

        this.dirErrors = modulePath.resolve("errors");
        Files.createDirectories(dirErrors);
        this.dirUniverses = modulePath.resolve("universes");

        Collection<WorldProperties> properties = game.getServer().getAllWorldProperties();

        for (Path file : Files.newDirectoryStream(dirUniverses))
        {
            loadUniverse(properties, file);
        }

        UniverseConfig mainUConfig;
        if (universes.isEmpty())
        {
            mainUConfig = loadUniverse(properties, dirUniverses.resolve("main.yml"));
        }
        else
        {
            mainUConfig = universes.values().iterator().next();
        }

        for (WorldProperties property : properties)
        {
            if (property.isEnabled())
            {
                mainUConfig.worlds.add(new ConfigWorld(game, property.getWorldName()));
                worlds.put(property.getWorldName(), mainUConfig);
            }
        }

    }

    private UniverseConfig loadUniverse(Collection<WorldProperties> properties, Path file)
    {
        if (!Files.exists(file) && Files.isDirectory(file))
        {
            return null;
        }
        String universeName = file.getFileName().toString();
        UniverseConfig uConfig = reflector.load(UniverseConfig.class, file.toFile());
        this.universes.put(universeName, uConfig);
        List<WorldProperties> worlds = new ArrayList<>();
        this.uWorlds.put(universeName, worlds);
        for (ConfigWorld w : uConfig.worlds)
        {
            Optional<WorldProperties> world = game.getServer().getWorldProperties(w.getName());
            if (world.isPresent())
            {
                worlds.add(world.get());
                this.worlds.put(world.get().getWorldName(), uConfig);
                properties.remove(world.get());
            }
            else
            {
                logger.warn("Missing world {}", w.getName());
            }
        }
        return uConfig;
    }

    public PermissionDescription getUniverseRootPerm()
    {
        return universeRootPerm;
    }

    public UniverseConfig getUniverseFrom(World world)
    {
        return this.worlds.get(world.getName());
    }

    public UniverseConfig createUniverse(String name)
    {
        return loadUniverse(null, dirUniverses.resolve(name));
    }

    public Collection<UniverseConfig> getUniverses()
    {
        return this.universes.values();
    }

    public UniverseConfig getUniverse(String name)
    {
        return this.universes.get(name);
    }
}
