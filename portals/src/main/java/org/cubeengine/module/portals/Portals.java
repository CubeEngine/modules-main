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
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.inject.Inject;
import com.google.common.base.Optional;
import org.cubeengine.butler.ProviderManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.core.util.LocationUtil;
import org.cubeengine.module.core.util.Pair;
import org.cubeengine.module.portals.config.Destination;
import org.cubeengine.module.portals.config.DestinationReader;
import org.cubeengine.module.portals.config.DestinationConverter;
import org.cubeengine.module.portals.config.PortalConfig;
import org.cubeengine.service.Selector;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldManager;
import de.cubeisland.engine.reflect.Reflector;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.service.filesystem.FileExtensionFilter.YAML;
import static java.util.stream.Collectors.toSet;

@ModuleInfo(name = "Portals", description = "Create and use portals")
public class Portals extends Module
{
    @Inject private Reflector reflector;
    @Inject private CommandManager cm;
    @Inject private WorldManager wm;
    @Inject private UserManager um;
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
    private Map<World, Pair<Integer, Chunk>> randomDestinationSettings = new HashMap<>();

    private Map<UUID, PortalsAttachment> attachments = new HashMap<>();

    @Enable
    public void onEnable() throws IOException
    {
        reflector.getDefaultConverterManager().registerConverter(new DestinationConverter(wm), Destination.class);
        ProviderManager rManager = cm.getProviderManager();
        rManager.register(this, new PortalReader(this), Portal.class);
        rManager.register(this, new DestinationReader(this, wm, i18n), Destination.class);

        this.portalsDir = Files.createDirectories(path.resolve("portals"));

        PortalCommands portals = new PortalCommands(this, selector, reflector, wm, i18n);
        cm.addCommand(portals);
        portals.addCommand(new PortalModifyCommand(this, selector, wm, game, i18n));

        em.registerListener(this, new PortalListener(this, um, i18n));
        this.loadPortals();
        tm.runTimer(this, this::checkForEntitiesInPortals, 5, 5);
    }

    @Disable
    public void onDisable()
    {
        em.removeListeners(this);
        cm.removeCommands(this);
    }

    public void setRandomDestinationSetting(World world, Integer radius, Chunk center)
    {
        this.randomDestinationSettings.put(world, new Pair<>(radius, center));
    }

    public Pair<Integer, Chunk> getRandomDestinationSetting(World world)
    {
        Pair<Integer, Chunk> setting = this.randomDestinationSettings.get(world);
        if (setting == null)
        {
            setting = new Pair<>(30, world.loadChunk(world.getSpawnLocation().getBlockPosition(), false).get());
        }
        return setting;
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
}
