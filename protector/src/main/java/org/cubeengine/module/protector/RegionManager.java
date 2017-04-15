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
package org.cubeengine.module.protector;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.filesystem.FileExtensionFilter;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class RegionManager
{
    private final Path modulePath;
    private final Reflector reflector;
    public Map<UUID, Map<String, Region>> byName = new HashMap<>();
    public Map<UUID, Map<Vector2i, List<Region>>> byChunk = new HashMap<>();
    public Map<UUID, Region> worldRegions = new HashMap<>();
    public Region globalRegion;

    public Map<UUID, Region> activeRegion = new HashMap<>(); // playerUUID -> Region

    public RegionManager(Path modulePath, Reflector reflector)
    {
        this.modulePath = modulePath;
        this.reflector = reflector;
        Path path = modulePath.resolve("region");
        try
        {
            Files.createDirectories(path);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        RegionConfig config = reflector.load(RegionConfig.class, path.resolve("global.yml").toFile());
        this.globalRegion = new Region(config);
    }

    private Region loadRegion(Region region)
    {
        byName.computeIfAbsent(region.getWorld().getUniqueId(), k -> new HashMap<>()).put(region.getName().toLowerCase(), region);

        Vector3d max = region.getCuboid().getMaximumPoint();
        Vector3d min = region.getCuboid().getMinimumPoint();
        Map<Vector2i, List<Region>> chunkMap = byChunk.computeIfAbsent(region.getWorld().getUniqueId(), k -> new HashMap<>());
        for (Vector2i chunkLoc : getChunks(min.toInt(), max.toInt()))
        {
            chunkMap.computeIfAbsent(chunkLoc, k -> new ArrayList<>()).add(region);
        }
        return region;
    }

    public List<Vector2i> getChunks(Vector3i from, Vector3i to)
    {
        List<Vector2i> result = new ArrayList<>();
        int chunkXFrom = from.getX() >> 4;
        int chunkZFrom = from.getZ() >> 4;
        int chunkXTo =  to.getX() >> 4;
        int chunkZTo = to.getZ() >> 4;
        if (chunkXFrom > chunkXTo) // if from is greater swap
        {
            chunkXFrom = chunkXFrom + chunkXTo;
            chunkXTo = chunkXFrom - chunkXTo;
            chunkXFrom = chunkXFrom - chunkXTo;
        }
        if (chunkZFrom > chunkZTo) // if from is greater swap
        {
            chunkZFrom = chunkZFrom + chunkZTo;
            chunkZTo = chunkZFrom - chunkZTo;
            chunkZFrom = chunkZFrom - chunkZTo;
        }
        for (int x = chunkXFrom; x <= chunkXTo; x++)
        {
            for (int z = chunkZFrom; z <= chunkZTo; z++)
            {
                result.add(new Vector2i(x, z));
            }
        }
        return result;
    }

    public List<Region> getRegionsAt(Location<World> loc)
    {
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        List<Region> regions = new ArrayList<>();
        regions.add(globalRegion);
        regions.add(getWorldRegion(loc.getExtent().getUniqueId()));
        regions.addAll(byChunk.getOrDefault(loc.getExtent().getUniqueId(), Collections.emptyMap())
                .getOrDefault(new Vector2i(chunkX, chunkZ), Collections.emptyList())
                            .stream().filter(r -> r.contains(loc))
                .sorted(Comparator.comparingInt(Region::getPriority))
                .collect(Collectors.toList()));
        return regions;
    }

    public Region getActiveRegion(CommandSource src)
    {
        return activeRegion.get(toUUID(src));
    }

    private UUID toUUID(CommandSource src)
    {
        return src instanceof Identifiable ? ((Identifiable) src).getUniqueId() : UUID.nameUUIDFromBytes(src.getIdentifier().getBytes());
    }

    public void setActiveRegion(CommandSource src, Region region)
    {
        this.activeRegion.put(toUUID(src), region);
    }

    public Map<String, Region> getRegions(UUID world)
    {
        return this.byName.getOrDefault(world, Collections.emptyMap());
    }

    public Map<UUID, Map<String,Region>> getRegions()
    {
        return this.byName;
    }

    public Region newRegion(World world, Cuboid boundingCuboid, String name)
    {
        Path folder = modulePath.resolve("region").resolve(world.getName());
        try {
            Files.createDirectories(folder);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        RegionConfig config = reflector.load(RegionConfig.class, folder.resolve(name + FileExtensionFilter.YAML.getExtention()).toFile());
        config.name = name;
        config.world = new ConfigWorld(world);
        config.corner1 = boundingCuboid.getMinimumPoint().toInt();
        config.corner2 = boundingCuboid.getMaximumPoint().toInt();
        config.save();
        return loadRegion(new Region(config));
    }

    public void reload()
    {
        try
        {
            Path regionsPath = modulePath.resolve("region");
            Files.createDirectories(regionsPath);
            for (Path worldPath : Files.newDirectoryStream(regionsPath))
            {
                if (Files.isDirectory(worldPath))
                {
                    for (Path configPath : Files.newDirectoryStream(worldPath, FileExtensionFilter.YAML))
                    {
                        if (configPath.getFileName().toString().equals("world.yml"))
                        {
                            continue;
                        }
                        RegionConfig config = reflector.load(RegionConfig.class, configPath.toFile());
                        loadRegion(new Region(config));
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    public int getRegionCount()
    {
        return this.byName.values().stream().mapToInt(Map::size).sum();
    }

    public boolean hasRegion(World world, String name)
    {
        return getRegions(world.getUniqueId()).containsKey(name.toLowerCase());
    }

    public void changeRegion(Region region, Cuboid cuboid)
    {
        for (Map<Vector2i, List<Region>> map : this.byChunk.values())
        {
            for (Map.Entry<Vector2i, List<Region>> entry : map.entrySet())
            {
                entry.getValue().remove(region);
            }
        }
        region.setCuboid(cuboid);
        region.save();
        this.loadRegion(region);
    }

    public Region getGlobalRegion() {
        return globalRegion;
    }

    public Region getWorldRegion(UUID world)
    {
        if (!this.worldRegions.containsKey(world))
        {
            Optional<WorldProperties> prop = Sponge.getServer().getWorldProperties(world);
            Path path = modulePath.resolve("region").resolve(prop.get().getWorldName());
            try
            {
                Files.createDirectories(path);
            }
            catch (IOException e)
            {
                throw new IllegalStateException(e);
            }
            RegionConfig config = reflector.load(RegionConfig.class, path.resolve("world.yml").toFile());
            config.world = new ConfigWorld(prop.get().getWorldName());
            config.save();
            this.worldRegions.put(world, new Region(config));
        }
        return this.worldRegions.get(world);


    }
}
