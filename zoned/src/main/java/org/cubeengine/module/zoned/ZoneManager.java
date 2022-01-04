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
package org.cubeengine.module.zoned;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.cubeengine.module.zoned.event.ZoneEvent;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
public class ZoneManager
{
    private Path path;
    private Map<String, ZoneConfig> zones = new HashMap<>();
    private I18n i18n;
    private Reflector reflector;
    private Logger logger;

    @Inject
    public ZoneManager(I18n i18n, Reflector reflector, FileManager fm, Logger logger)
    {
        this.i18n = i18n;
        this.reflector = reflector;
        this.logger = logger;
        this.path = fm.getModulePath(Zoned.class);
    }

    public void loadZones() throws IOException
    {
        this.zones.clear();
        Path zones = this.path.resolve("zones");
        Files.createDirectories(zones);
        try (Stream<Path> files = Files.walk(zones))
        {
            files.filter(f -> !Files.isDirectory(f)).filter(f -> f.toString().endsWith("yml")).forEach(file -> {
                ZoneConfig cfg = reflector.load(ZoneConfig.class, file.toFile());
                this.zones.put(cfg.name, cfg);
            });
        }
        logger.info("Loaded {} zones", this.zones.size());
    }

    public Collection<ZoneConfig> getZones(String match, ServerWorld world)
    {
        return zones.values().stream().filter(z -> world == null || z.world.getWorld() == world)
                    // TODO fuzzy match
                    .filter(z -> match == null || z.name.equalsIgnoreCase(match) || z.name.startsWith(match)).collect(Collectors.toList());
    }


    public void delete(Audience context, ZoneConfig zone)
    {
        try
        {
            Files.delete(zone.getFile().toPath());
            this.zones.remove(zone.name);
            i18n.send(context, POSITIVE, "The zone {name} was deleted.", zone.name);
        }
        catch (IOException e)
        {
            i18n.send(context, CRITICAL, "Could not delete file.");
        }
    }

    public void define(Audience cmdSource, String name, ZoneConfig cfg, boolean overwrite)
    {
        if (cfg.isComplete())
        {
            i18n.send(cmdSource, NEGATIVE, "Current selection is incomplete and cannot be saved as a zone");
            return;
        }
        Path dir = this.path.resolve("zones").resolve(cfg.world.getName());
        Path file = dir.resolve(name + ".yml");
        if (!overwrite && Files.exists(file))
        {
            i18n.send(cmdSource, NEGATIVE, "Zone is already defined. Use /redefine to update the zone");
            return;
        }
        try
        {
            Files.createDirectories(dir);
        }
        catch (IOException e)
        {
            i18n.send(cmdSource, CRITICAL, "Could not create directories");
            return;
        }
        cfg.name(name);
        cfg.setFile(file.toFile());
        cfg.save();
        zones.put(name, cfg);
        i18n.send(cmdSource, POSITIVE, "Saved zone as {name}", name);
        Sponge.eventManager().post(new ZoneEvent(cfg));
    }

    public ZoneConfig getZone(String token)
    {
        return this.zones.get(token);
    }

    public List<ZoneConfig> getZonesAt(ServerLocation location)
    {
        return null;
    }
}
