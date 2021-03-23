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
import java.util.Set;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.Pair;
import org.cubeengine.module.portals.command.PortalCommands;
import org.cubeengine.module.portals.config.Destination;
import org.cubeengine.module.portals.config.DestinationConverter;
import org.cubeengine.module.portals.config.PortalConfig;
import org.cubeengine.module.zoned.Zoned;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.Server;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import static java.util.stream.Collectors.toSet;
import static org.cubeengine.libcube.service.filesystem.FileExtensionFilter.YAML;

@Singleton
@Module(dependencies = @Dependency("cubeengine-zoned"))
public class Portals
{
    @Inject private Reflector reflector;
    @Inject private TaskManager tm;
    @Inject private Logger logger;
    private Path path;
    @Inject private I18n i18n;
    @Inject private ModuleManager mm;
    @Inject private FileManager fm;

    private Path portalsDir;
    private final Map<String, Portal> portals = new HashMap<>();
    private final Map<Portal, List<UUID>> entitesInPortals = new HashMap<>();

    private final Map<UUID, PortalsAttachment> attachments = new HashMap<>();

    @ModuleConfig private PortalsConfig config;
    @ModuleCommand private PortalCommands portalCommands;
    @ModuleListener private PortalListener listener;

    private Zoned zoned;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event) throws IOException
    {
        this.path = fm.getModulePath(Portals.class);
        this.reflector.getDefaultConverterManager().registerConverter(new DestinationConverter(), Destination.class);

        this.portalsDir = Files.createDirectories(path.resolve("portals"));
        this.loadPortals();
        tm.runTimer(this::checkForEntitiesInPortals, Ticks.of(5), Ticks.of(5));
        tm.runTimer(this::spawnPortalParticles, Ticks.of(20), Ticks.of(20));
        this.zoned = (Zoned) mm.getModule(Zoned.class);
    }

    private void spawnPortalParticles(ScheduledTask task)
    {
        for (Portal portal : portals.values())
        {
            if (portal.config.particles)
            {
                final AABB aabb = portal.config.getAABB();
                final Vector3d size = aabb.size();
                final ParticleEffect effect = ParticleEffect.builder()
                                                            .type(ParticleTypes.PORTAL)
                                                            .quantity((int)(10 * size.getX() * size.getY() * size.getZ()))
                                                            .offset(size.div(4))
                                                            .build();
                portal.getWorld().spawnParticles(effect, aabb.center());
            }
        }
    }

    public Pair<Integer, Vector3i> getRandomDestinationSetting(ServerWorld world)
    {
        Vector3i pos = world.properties().spawnPosition();
        Integer radius = Math.min((int)world.properties().worldBorder().diameter() / 2, 60 * 16);
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
    }

    public Portal getPortal(String name)
    {
        return this.portals.get(name.toLowerCase());
    }

    public void addPortal(Portal portal)
    {
        this.portals.put(portal.getName().toLowerCase(), portal);
    }

    public void removePortal(Portal portal)
    {
        this.portals.remove(portal.getName().toLowerCase());
    }

    public Collection<Portal> getPortals()
    {
        return portals.values();
    }

    public Set<Portal> getPortals(World world)
    {
        return getPortals().stream().filter(portal -> portal.getWorld().equals(world)).collect(toSet());
    }

    private void checkForEntitiesInPortals(ScheduledTask task)
    {
        this.portals.values().stream()
                    .filter(portal -> portal.config.teleportNonPlayers)
                    .forEach(portal -> {
                        final AABB aabb = portal.getAABB();
                        final Collection<? extends Entity> nowInPortal = portal.getWorld().entities(aabb, entity -> !(entity instanceof ServerPlayer));
                        final List<UUID> wasInPortal = entitesInPortals.computeIfAbsent(portal, k -> new ArrayList<>());
                        nowInPortal.forEach(entity -> {
                            if (!wasInPortal.remove(entity.uniqueId()))
                            {
                                portal.teleport(entity);
                            }
                        });
                    });
    }

    public File getPortalFile(String name)
    {
        return portalsDir.resolve(name + ".yml").toFile();
    }

    public List<UUID> getEntitiesInPortal(Portal portal)
    {
        return this.entitesInPortals.computeIfAbsent(portal, p -> new ArrayList<>());
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

    public Zoned getZoned()
    {
        return this.zoned;
    }
}
