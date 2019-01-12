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
package org.cubeengine.module.protector;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.filesystem.FileExtensionFilter;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.cubeengine.module.zoned.ZoneConfig;
import org.cubeengine.module.zoned.Zoned;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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
    private Log logger;
    private Zoned zonedModule;
    private Map<UUID, Map<String, Region>> byName = new HashMap<>();
    private Map<UUID, Map<Vector2i, List<Region>>> byChunk = new HashMap<>();

    private Map<UUID, Region> worldRegions = new HashMap<>();
    private Region globalRegion;

    private Map<UUID, Region> activeRegion = new HashMap<>(); // playerUUID -> Region

    public RegionManager(Path modulePath, Reflector reflector, Log logger, Zoned zonedModule)
    {
        this.modulePath = modulePath;
        this.reflector = reflector;
        this.logger = logger;
        this.zonedModule = zonedModule;
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
        this.globalRegion = new Region(null, config, this);
    }

    private Region loadRegion(Region region)
    {
        World world = region.getWorld();
        if (world == null) {
            logger.warn("Region {} was not loaded: Could not find world {}.", region.getName(), region.getWorldName());
            return region;
        }
        byName.computeIfAbsent(world.getUniqueId(), k -> new HashMap<>()).put(region.getName().toLowerCase(), region);

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

    private Map<UUID, Map<Vector3i, List<Region>>> regionCache = new HashMap<>();

    public List<Region> getRegionsAt(Location<World> loc)
    {
        Map<Vector3i, List<Region>> cache = regionCache.computeIfAbsent(loc.getExtent().getUniqueId(), k -> new HashMap<>());
        return cache.computeIfAbsent(loc.getBlockPosition(), v -> getRegions(loc.getExtent(), loc.getBlockPosition()));
    }

    public List<Region> getRegionsAt(World world, Vector3i pos) {
        Map<Vector3i, List<Region>> cache = regionCache.computeIfAbsent(world.getUniqueId(), k -> new HashMap<>());
        return cache.computeIfAbsent(pos, v -> getRegions(world, pos));
    }

    private List<Region> getRegions(World world, Vector3i pos)
    {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        List<Region> regions = new ArrayList<>();
        regions.add(globalRegion);
        regions.add(getWorldRegion(world.getUniqueId()));
        regions.addAll(byChunk.getOrDefault(world.getUniqueId(), Collections.emptyMap())
                .getOrDefault(new Vector2i(chunkX, chunkZ), Collections.emptyList())
                            .stream().filter(r -> r.contains(pos.toDouble()))
                .collect(Collectors.toList()));
        regions.sort(Comparator.comparingInt(Region::getPriority).reversed());
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

    public void reload()
    {
        try
        {
            this.byChunk.clear();
            this.byName.clear();
            this.markDirty();
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
                        ZoneConfig zone = zonedModule.getManager().getZone(config.name);
                        if (zone == null)
                        {
                            logger.info("Converting old region to zone format");
                            zone = reflector.create(ZoneConfig.class);
                            zone.world = config.world;
                            zone.shape = new Cuboid(config.corner1.toDouble(), config.corner2.toDouble().sub(config.corner1.toDouble()));
                            zonedModule.getManager().define(Sponge.getServer().getConsole(), config.name, zone, false);
                        }
                        loadRegion(new Region(zone, config, this));
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
            this.worldRegions.put(world, new Region(null, config, this));
        }
        return this.worldRegions.get(world);


    }

    public Collection<Region> getWorldRegions()
    {
        return worldRegions.values();
    }

    public void markDirty()
    {
        this.regionCache.clear();
    }

    public boolean deleteRegion(Region region)
    {
        if (region.getConfig().getFile().delete())
        {
            this.reload();
            return true;
        }
        return false;
    }

    public Region newRegion(ZoneConfig zone)
    {
        RegionConfig config = reflector.create(RegionConfig.class);
        config.name = zone.name;
        config.world = zone.world;
        Region region = this.loadRegion(new Region(zone, config, this));
        Path dir = modulePath.resolve("region").resolve(config.world.getName());
        try
        {
            Files.createDirectories(dir);
        }
        catch (IOException e)
        {
            throw new IllegalStateException();
        }
        config.setFile(dir.resolve(config.name + ".yml").toFile());
        config.save();
        return region;
    }
}
