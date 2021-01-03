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
package org.cubeengine.module.portals.command;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.portals.Portal;
import org.cubeengine.module.portals.Portals;
import org.cubeengine.module.portals.PortalsAttachment;
import org.cubeengine.module.portals.config.Destination;
import org.cubeengine.module.portals.config.PortalConfig;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Command(name = "portals", desc = "The portal commands", alias = "mvp")
@Using({DestinationParser.class, PortalParser.class})
public class PortalCommands extends DispatcherCommand
{
    private final Portals module;
    private Reflector reflector;
    private I18n i18n;

    @Inject
    public PortalCommands(Portals module, Reflector reflector, I18n i18n, PortalModifyCommand modifyCommand)
    {
        super(Portals.class, modifyCommand);
        this.module = module;
        this.reflector = reflector;
        this.i18n = i18n;
    }

    @Alias(value = "mvpc")
    @Command(desc = "Creates a new Portal")
    @Restricted(msg = "You must be ingame to do this!")
    public void create(ServerPlayer context, String name, @Option Destination destination)
    {
        final ZoneConfig activeZone = module.getZoned().getActiveZone(context);
        if (activeZone == null || !(activeZone.shape instanceof Cuboid))
        {
            i18n.send(context, NEGATIVE, "Please select a cuboid first!");
            return;
        }
        if (module.getPortal(name) != null)
        {
            i18n.send(context, NEGATIVE, "A portal named {input} already exists!", name);
            return;
        }
        final ServerWorld world = activeZone.world.getWorld();
        final ServerLocation p1 = world.getLocation(((Cuboid)activeZone.shape).getMinimumPoint());
        final ServerLocation p2 = world.getLocation(((Cuboid)activeZone.shape).getMaximumPoint());
        final PortalConfig config = reflector.create(PortalConfig.class);
        config.location.from = new Vector3i(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
        config.location.to = new Vector3i(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
        config.location.destination = Transform.of(context.getLocation().getPosition(), context.getRotation());
        config.owner = context.getName();
        config.world = new ConfigWorld(p1.getWorld());

        config.destination = destination;

        config.setFile(module.getPortalFile(name));
        config.save();
        final Portal portal = new Portal(module, name, config, i18n);
        module.addPortal(portal);

        module.getPortalsAttachment(context.getUniqueId()).setPortal(portal);
        i18n.send(context, POSITIVE, "Portal {name} created!", portal.getName());
        if (destination == null)
        {
            i18n.send(context, POSITIVE, "Select a destination using the portal modify destination command");
        }
    }

    @Alias(value = "mvps")
    @Command(desc = "Selects an existing portal")
    @Restricted(msg = "You must be ingame to do this!")
    public void select(ServerPlayer context, Portal portal)
    {
        module.getPortalsAttachment(context.getUniqueId()).setPortal(portal);
        i18n.send(context, POSITIVE, "Portal selected: {name}", portal.getName());
    }

    @Alias(value ="mvpi")
    @Command(desc = "Show info about a portal")
    public void info(CommandCause context, @Default Portal portal)
    {
        portal.showInfo(context.getAudience());
    }

    @Alias(value = "mvpr")
    @Command(desc = "Removes a portal permanently")
    public void remove(CommandCause context, @Default Portal portal)
    {
        portal.delete();
        i18n.send(context, POSITIVE, "Portal {name} deleted", portal.getName());
    }

    @Command(desc = "Shows debug portal information instead of teleporting")
    @Restricted(msg = "You must be ingame to do this!")
    public void debug(ServerPlayer context, @Option Boolean isDebug)
    {
        final PortalsAttachment attachment = module.getPortalsAttachment(context.getUniqueId());
        if (isDebug == null)
        {
            attachment.toggleDebug();
        }
        else
        {
            if (isDebug)
            {
                if (!attachment.isDebug()) attachment.toggleDebug();
            }
            else
            {
                if (attachment.isDebug()) attachment.toggleDebug();
            }
        }
        if (attachment.isDebug())
        {
            i18n.send(context, POSITIVE, "Portal debug mode ON!");
            return;
        }
        i18n.send(context, POSITIVE, "Portal debug mode OFF!");
    }

    @Alias("mvpl")
    @Command(desc = "Lists the portals")
    public void list(CommandCause context, @Default ServerWorld world)
    {
        final Set<Portal> portals = module.getPortals(world);
        if (portals.isEmpty())
        {
            i18n.send(context, POSITIVE, "There are no portals in {world}", world);
            return;
        }
        PaginationList.Builder builder = PaginationList.builder().title(i18n.translate(context, POSITIVE, "The following portals are located in {world}", world))
            .contents(portals.stream().sorted(Comparator.comparing(Portal::getName)).map(p -> Component.text(" - " + p.getName())).collect(Collectors.toList()));
        builder.sendTo(context.getAudience());
    }
}
