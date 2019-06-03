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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.logscribe.Log;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZoneManager
{
    private Map<String, ZoneConfig> zones = new HashMap<>();
    private Zoned module;
    private I18n i18n;
    private Reflector reflector;
    private Log logger;

    public ZoneManager(Zoned module, I18n i18n, Reflector reflector, Log logger)
    {
        this.module = module;
        this.i18n = i18n;
        this.reflector = reflector;
        this.logger = logger;
        this.loadZones();
    }

    private void loadZones()
    {
        this.zones.clear();
        Path zonesDir = this.module.getPath().resolve("zones");
        try {
            Files.createDirectories(zonesDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (Stream<Path> files = Files.walk(zonesDir)){
            files.filter(f -> !Files.isDirectory(f))
                 .filter(f -> f.toString().endsWith("yml"))
                 .forEach(file -> {
                     ZoneConfig cfg = reflector.load(ZoneConfig.class, file.toFile());
                     this.zones.put(cfg.name, cfg);
                 });
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Loaded {} zones", this.zones.size());
    }

    public Collection<ZoneConfig> getZones(String match, World world)
    {
        return zones.values().stream().filter(z -> world == null || z.world.getWorld() == world)
                // TODO fuzzy match
                .filter(z -> match == null || z.name.equalsIgnoreCase(match)).collect(Collectors.toList());
    }


    public void delete(CommandSource context, ZoneConfig zone)
    {
        try
        {
            Files.delete(zone.getFile().toPath());
            this.zones.remove(zone.name);
            i18n.send(context, POSITIVE, "The region {name} was deleted.", zone.toString());
        }
        catch (IOException e)
        {
            i18n.send(context, CRITICAL, "Could not delete file.");
        }
    }

    public void define(CommandSource cmdSource, String name, ZoneConfig cfg, boolean overwrite)
    {
        Path dir = module.getPath().resolve("zones").resolve(cfg.world.getName());
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
        cfg.save(file.toFile());
        zones.put(name, cfg);
        i18n.send(cmdSource, POSITIVE, "Saved zone as {name}", name);
    }

    public ZoneConfig getZone(String token)
    {
        return this.zones.get(token);
    }

    public List<ZoneConfig> getZonesAt(Location<World> location)
    {
        return null;
    }
}
