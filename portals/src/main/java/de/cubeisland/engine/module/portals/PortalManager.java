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
package de.cubeisland.engine.module.portals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.core.util.LocationUtil;
import de.cubeisland.engine.module.core.util.Pair;
import de.cubeisland.engine.module.portals.config.PortalConfig;
import de.cubeisland.engine.module.service.Selector;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.service.task.TaskManager;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.world.WorldManager;
import de.cubeisland.engine.reflect.Reflector;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.EntityTeleportEvent;
import org.spongepowered.api.event.entity.player.PlayerMoveEvent;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;

public class PortalManager
{
    public final Portals module;
    private Reflector reflector;
    private Log logger;
    protected final File portalsDir;

    private final Map<String, Portal> portals = new HashMap<>();
    private final Map<Long, List<Portal>> chunksWithPortals = new HashMap<>();

    private Map<World, Pair<Integer, Chunk>> randomDestinationSettings = new HashMap<>();

    public void setRandomDestinationSetting(World world, Integer radius, Chunk center)
    {
        this.randomDestinationSettings.put(world, new Pair<>(radius, center));
    }

    public Pair<Integer, Chunk> getRandomDestinationSetting(World world)
    {
        Pair<Integer, Chunk> setting = this.randomDestinationSettings.get(world);
        if (setting == null)
        {
            setting = new Pair<>(30, world.getSpawnLocation().getChunk());
        }
        return setting;
    }

    public PortalManager(Portals module, Selector selector, Reflector reflector, WorldManager wm, EventManager em, TaskManager tm, CommandManager cm, Log logger)
    {
        this.module = module;
        this.reflector = reflector;
        this.logger = logger;
        this.portalsDir = this.module.getFolder().resolve("portals").toFile();
        this.portalsDir.mkdir();
        PortalCommands portals = new PortalCommands(this.module, this, selector, reflector, wm);
        cm.addCommand(portals);
        portals.addCommand(new PortalModifyCommand(this.module, this));

        em.registerListener(this.module, this);
        this.loadPortals();
        tm.runTimer(module, this::checkForEntitiesInPortals, 5, 5);
    }

    private void loadPortals()
    {
        for (File file : this.portalsDir.listFiles())
        {
            if (!file.isDirectory() && file.getName().endsWith(".yml"))
            {
                PortalConfig load = reflector.load(PortalConfig.class, file);
                Portal portal = new Portal(module, this, file.getName().substring(0, file.getName().lastIndexOf(".yml")), load);
                this.addPortal(portal);
            }
        }
        logger.info("{} portals loaded!", this.portals.size());
        logger.debug("in {} chunks", this.chunksWithPortals.size());
    }

    @Subscribe
    public void onTeleport(PlayerTeleportEvent event)
    {
        List<Portal> portals = this.chunksWithPortals.get(LocationUtil.getChunkKey(event.getNewLocation()));
        if (portals == null)
        {
            return;
        }
        for (Portal portal : portals)
        {
            if (portal.has(event.getNewLocation()))
            {
                User user = um.getExactUser(event.getUser().getUniqueId());
                PortalsAttachment attachment = user.attachOrGet(PortalsAttachment.class, module);
                attachment.setInPortal(true);
                if (attachment.isDebug())
                {
                    user.sendTranslated(POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Teleported into portal: {name}", portal.getName());
                }
                return;
            }
            // else ignore
        }
    }

    final WeakHashMap<Portal, List<Entity>> entitesInPortals = new WeakHashMap<>();

    private void checkForEntitiesInPortals()
    {
        for (Portal portal : this.portals.values())
        {
            if (portal.config.teleportNonPlayers)
            {
                for (Pair<Integer, Integer> chunk : portal.getChunks())
                {
                    if (portal.getWorld().isChunkLoaded(chunk.getLeft(), chunk.getRight()))
                    {
                        for (Entity entity : portal.getWorld().getChunkAt(chunk.getLeft(), chunk.getRight()).getEntities())
                        {
                            if (!(entity instanceof Player))
                            {
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
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onEntityTeleport(EntityTeleportEvent event)
    {
        List<Portal> portals = this.chunksWithPortals.get(LocationUtil.getChunkKey(event.getOldLocation()));
        if (portals == null)
        {
            return;
        }
        for (Portal portal : portals)
        {
            List<Entity> entities = this.entitesInPortals.get(portal);
            if (portal.has(event.getNewLocation()))
            {
                if (entities == null)
                {
                    entities = new ArrayList<>();
                    this.entitesInPortals.put(portal, entities);
                }
                entities.add(event.getEntity());
                return;
            }
            else if (entities != null)
            {
                entities.remove(event.getEntity());
            }
        }
    }

    @Subscribe
    public void onMove(PlayerMoveEvent event)
    {
        if (event.getOldLocation().getExtent() != event.getNewLocation().getExtent())
        {
            return;
        }
        if (event.getOldLocation().getBlockX() != event.getNewLocation().getBlockX()
         || event.getOldLocation().getBlockY() != event.getNewLocation().getBlockY()
         || event.getOldLocation().getBlockZ() != event.getNewLocation().getBlockZ())
        {
            List<Portal> portals = this.chunksWithPortals.get(LocationUtil.getChunkKey(event.getNewLocation()));
            User user = um.getExactUser(event.getUser().getUniqueId());
            PortalsAttachment attachment = user.attachOrGet(PortalsAttachment.class, module);
            if (portals != null)
            {
                for (Portal portal : portals)
                {
                    if (portal.has(event.getNewLocation()))
                    {
                        if (attachment.isDebug())
                        {
                            if (attachment.isInPortal())
                            {
                                user.sendTranslated(POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Move in portal: {name}", portal.getName());
                            }
                            else
                            {
                                user.sendTranslated(POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Entered portal: {name}", portal.getName());
                                portal.showInfo(user);
                                attachment.setInPortal(true);
                            }
                        }
                        else if (!attachment.isInPortal())
                        {
                            portal.teleport(user);
                        }
                        return;
                    }
                    // else ignore
                }
            }
            attachment.setInPortal(false);
            // else movement is not in a chunk that has a portal
        }
    }

    public Portal getPortal(String name)
    {
        return this.portals.get(name.toLowerCase());
    }

    protected void addPortal(Portal portal)
    {
        this.portals.put(portal.getName().toLowerCase(), portal);

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
        Set<Portal> result = new HashSet<>();
        for (Portal portal : getPortals())
        {
            if (portal.getWorld().equals(world))
            {
                result.add(portal);
            }
        }
        return result;
    }
}
