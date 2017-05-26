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
package org.cubeengine.module.portals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.inject.Inject;

import com.flowpowered.math.vector.Vector3i;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.butler.provider.Providers;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.libcube.util.LocationUtil;
import org.cubeengine.libcube.util.Pair;
import org.cubeengine.module.portals.config.Destination;
import org.cubeengine.module.portals.config.DestinationConverter;
import org.cubeengine.module.portals.config.DestinationParser;
import org.cubeengine.module.portals.config.PortalConfig;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static java.util.stream.Collectors.toSet;
import static org.cubeengine.libcube.service.filesystem.FileExtensionFilter.YAML;

@ModuleInfo(name = "Portals", description = "Create and use portals")
public class Portals extends Module
{
    @Inject private Reflector reflector;
    @Inject private CommandManager cm;
    @Inject private Selector selector;
    @Inject private EventManager em;
    @Inject private TaskManager tm;
    @Inject private Log logger;
    @Inject private Path path;
    @Inject private Game game;
    @Inject private I18n i18n;

    private Path portalsDir;
    private final Map<String, Portal> portals = new HashMap<>();
    private final Map<Long, List<Portal>> chunksWithPortals = new HashMap<>();
    private final WeakHashMap<Portal, List<Entity>> entitesInPortals = new WeakHashMap<>();

    private Map<UUID, PortalsAttachment> attachments = new HashMap<>();
    @ModuleConfig private PortalsConfig config;

    @Enable
    public void onEnable() throws IOException
    {
        reflector.getDefaultConverterManager().registerConverter(new DestinationConverter(), Destination.class);
        Providers rManager = cm.getProviders();
        rManager.register(this, new PortalParser(this), Portal.class);
        rManager.register(this, new DestinationParser(this, i18n), Destination.class);

        this.portalsDir = Files.createDirectories(path.resolve("portals"));

        PortalCommands portals = new PortalCommands(cm, this, selector, reflector, i18n);
        cm.addCommand(portals);
        portals.addCommand(new PortalModifyCommand(cm, selector, game, i18n));

        em.registerListener(Portals.class, new PortalListener(this, i18n));
        this.loadPortals();
        tm.runTimer(Portals.class, this::checkForEntitiesInPortals, 5, 5);
    }

    public Pair<Integer, Vector3i> getRandomDestinationSetting(World world)
    {
        Vector3i pos = world.getSpawnLocation().getBlockPosition();
        Integer radius = Math.min((int)world.getWorldBorder().getDiameter() / 2, 60 * 16);
        return new Pair<>(radius, pos);
    }

    private void loadPortals() throws IOException
    {
        for (Path file : Files.newDirectoryStream(this.portalsDir, YAML))
        {
            PortalConfig load = reflector.load(PortalConfig.class, file.toFile());
            String fileName = file.getFileName().toString();
            Portal portal = new Portal(this, fileName.substring(0, fileName.lastIndexOf(".yml")), load, i18n);
            try
            {
                this.addPortal(portal);
            }
            catch (Exception e)
            {
                logger.error("Could not load portal {}", portal.getName());
            }
        }
        logger.info("{} portals loaded!", this.portals.size());
        logger.debug("in {} chunks", this.chunksWithPortals.size());
    }

    public Portal getPortal(String name)
    {
        return this.portals.get(name.toLowerCase());
    }

    protected void addPortal(Portal portal)
    {
        List<Pair<Integer,Integer>> chunks = portal.getChunks();
        for (Pair<Integer, Integer> chunk : chunks)
        {
            long chunkKey = LocationUtil.getChunkKey(chunk.getLeft(), chunk.getRight());
            List<Portal> list = this.chunksWithPortals.get(chunkKey);
            if (list == null)
            {
                list = new ArrayList<>();
                this.chunksWithPortals.put(chunkKey, list);
            }
            list.add(portal);
        }

        this.portals.put(portal.getName().toLowerCase(), portal);
    }

    protected void removePortal(Portal portal)
    {
        this.portals.remove(portal.getName().toLowerCase());
        for (List<Portal> portalList : this.chunksWithPortals.values())
        {
            portalList.remove(portal);
        }
    }

    public Collection<Portal> getPortals()
    {
        return portals.values();
    }

    public Set<Portal> getPortals(World world)
    {
        return getPortals().stream().filter(portal -> portal.getWorld().equals(world)).collect(toSet());
    }

    private void checkForEntitiesInPortals()
    {
        this.portals.values().stream()
                    .filter(portal -> portal.config.teleportNonPlayers)
                    .forEach(portal -> {
                        for (Pair<Integer, Integer> chunk : portal.getChunks())
                        {
                            Optional<Chunk> loaded = portal.getWorld().getChunk(chunk.getLeft(), 0, chunk.getRight());
                            if (!loaded.isPresent())
                            {
                                return;
                            }
                            loaded.get().getEntities().stream()
                                  .filter(entity -> !(entity instanceof Player))
                                  .forEach(entity -> {
                                      List<Entity> entities = entitesInPortals.get(portal);
                                      if (portal.has(entity.getLocation()))
                                      {
                                          if (entities == null || entities.isEmpty() || !entities.contains(entity))
                                          {
                                              portal.teleport(entity);
                                          }
                                      }
                                      else if (entities != null)
                                      {
                                          entities.remove(entity);
                                      }
                                  });
                        }
                    });
    }

    public File getPortalFile(String name)
    {
        return portalsDir.resolve(name + ".yml").toFile();
    }

    public List<Portal> getPortalsInChunk(Location location)
    {
        return chunksWithPortals.get(LocationUtil.getChunkKey(location));
    }

    public List<Entity> getEntitiesInPortal(Portal portal)
    {
        List<Entity> entities = entitesInPortals.get(portal);
        if (entities == null)
        {
            entities = new ArrayList<>();
            entitesInPortals.put(portal, entities);
        }
        return entities;
    }


    public PortalsAttachment getPortalsAttachment(UUID uuid)
    {
        PortalsAttachment attachment = attachments.get(uuid);
        if (attachment == null)
        {
            attachment = new PortalsAttachment();
            attachments.put(uuid, attachment);
        }
        return attachment;
    }

    public PortalsConfig getConfig() {
        return config;
    }
}
