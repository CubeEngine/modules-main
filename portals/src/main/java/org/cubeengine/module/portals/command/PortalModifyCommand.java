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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.portals.Portal;
import org.cubeengine.module.portals.Portals;
import org.cubeengine.module.portals.config.Destination;
import org.cubeengine.module.portals.config.RandomDestination;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Alias("mvpm")
@Command(name = "modify", desc = "modifies a portal")
@Using({PortalParser.class, DestinationParser.class})
public class PortalModifyCommand extends DispatcherCommand
{
    private Portals module;
    private I18n i18n;

    @Inject
    public PortalModifyCommand(Portals module, I18n i18n)
    {
        super(Portals.class);
        this.module = module;
        this.i18n = i18n;
    }

    @Command(desc = "Changes the owner of a portal")
    public void owner(CommandCause context, User owner, @Default Portal portal)
    {
        portal.config.owner = owner.getName();
        portal.config.save();
        i18n.send(context, POSITIVE, "{user} is now the owner of {name#portal}!", owner, portal.getName());
    }

    @Alias(value = "mvpd")
    @Command(alias = "dest", desc = "changes the destination of the selected portal")
    public void destination(CommandCause context, @Label("here|<world>|p:<portal>") Destination destination, @Default Portal portal)
    {
        portal.config.destination = destination;
        portal.config.save();
        i18n.send(context, POSITIVE, "Portal destination set!");
    }

    @Alias(value = "mvprd")
    @Command(alias = "randdest", desc = "Changes the destination of the selected portal to a random position each time")
    public void randomDestination(CommandCause context, ServerWorld world, @Default Portal portal)
    {
        this.destination(context, new RandomDestination(world), portal);
    }

    @Command(desc = "Changes a portals location")
    @Restricted(msg = "You have to be ingame to do this!")
    public void location(ServerPlayer context, @Default Portal portal)
    {
        final ZoneConfig activeZone = module.getZoned().getActiveZone(context);
        if (activeZone == null || !(activeZone.shape instanceof Cuboid))
        {
            i18n.send(context, NEGATIVE, "Please select a cuboid first!");
            return;
        }
        this.module.removePortal(portal);
        final ServerWorld world = activeZone.world.getWorld();
        final ServerLocation p1 = world.getLocation(((Cuboid)activeZone.shape).getMinimumPoint());
        final ServerLocation p2 = world.getLocation(((Cuboid)activeZone.shape).getMaximumPoint());
        portal.config.world = new ConfigWorld(p1.getWorld());
        portal.config.location.from = new Vector3i(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
        portal.config.location.to = new Vector3i(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
        portal.config.save();
        this.module.addPortal(portal);
        i18n.send(context, POSITIVE, "Portal {name} updated to your current selection!", portal.getName());
    }

    @Command(desc = "Modifies the location where a player exits when teleporting a portal")
    @Restricted(msg = "You have to be ingame to do this!")
    public void exit(ServerPlayer context, @Default Portal portal)
    {
        final ServerLocation location = context.getServerLocation();
        if (!portal.config.world.getWorld().getKey().equals(location.getWorldKey()))
        {
            // TODO range check? range in config
            i18n.send(context, NEGATIVE, "A portals exit cannot be in an other world than its location!");
            return;
        }
        portal.config.location.destination = Transform.of(location.getPosition(), context.getRotation());
        portal.config.save();
        i18n.send(context, POSITIVE, "The portal exit of portal {name} was set to your current location!", portal.getName());
    }

    @Command(desc = "Toggles safe teleportation for this portal")
    public void togglesafe(CommandCause context, @Default Portal portal)
    {
        portal.config.safeTeleport = !portal.config.safeTeleport;
        portal.config.save();
        if (portal.config.safeTeleport)
        {
            i18n.send(context, POSITIVE, "The portal {name} will not teleport to an unsafe destination", portal.getName());
            return;
        }
        i18n.send(context, POSITIVE, "The portal {name} will also teleport to an unsafe destination", portal.getName());
    }

    @Command(desc = "Toggles whether entities can teleport with this portal")
    public void entity(CommandCause context, @Default Portal portal)
    {
        portal.config.teleportNonPlayers = !portal.config.teleportNonPlayers;
        portal.config.save();
        if (portal.config.teleportNonPlayers)
        {
            i18n.send(context, POSITIVE, "The portal {name} will teleport entities too", portal.getName());
            return;
        }
        i18n.send(context, POSITIVE, "The portal {name} will only teleport players", portal.getName());
    }
}
