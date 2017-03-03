package org.cubeengine.module.protector;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.libcube.service.filesystem.FileExtensionFilter;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RegionManager
{
    public Map<UUID, Map<String, Region>> byName = new HashMap<>();
    public Map<UUID, Map<Vector2i, List<Region>>> byChunk = new HashMap<>();

    public RegionManager(Path modulePath, Reflector reflector) throws IOException
    {
        Path regionsPath = modulePath.resolve("region");
        Files.createDirectories(regionsPath);
        for (Path worldPath : Files.newDirectoryStream(regionsPath))
        {
            if (Files.isDirectory(worldPath))
            {
                for (Path configPath : Files.newDirectoryStream(worldPath, FileExtensionFilter.YAML))
                {
                    RegionConfig config = reflector.load(RegionConfig.class, configPath.toFile());
                    Region region = new Region(config);
                    byName.computeIfAbsent(config.world.getUniqueId(), k -> new HashMap<>()).put(config.name, region);

                    Vector3d max = region.getCuboid().getMaximumPoint();
                    Vector3d min = region.getCuboid().getMinimumPoint();
                    Map<Vector2i, List<Region>> chunkMap = byChunk.computeIfAbsent(config.world.getUniqueId(), k -> new HashMap<>());
                    for (Vector2i chunkLoc : getChunks(min.toInt(), max.toInt()))
                    {
                        chunkMap.computeIfAbsent(chunkLoc, k -> new ArrayList<>()).add(region);
                    }
                }
            }
        }
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
        List<Region> list = byChunk.getOrDefault(loc.getExtent().getUniqueId(), Collections.emptyMap())
                                   .getOrDefault(new Vector2i(chunkX, chunkZ), Collections.emptyList());
        list.sort(Comparator.comparingInt(Region::getPriority));
        return list;
    }
}
