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

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Desc;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.portals.config.Destination;
import org.cubeengine.module.portals.config.RandomDestination;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.config.WorldTransform;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Alias("mvpm")
@Command(name = "modify", desc = "modifies a portal")
public class PortalModifyCommand extends ContainerCommand
{

    private Portals module;
    private Selector selector;
    private I18n i18n;

    public PortalModifyCommand(CommandManager base, Portals module, Selector selector, I18n i18n)
    {
        super(base, Portals.class);
        this.module = module;
        this.selector = selector;
        this.i18n = i18n;
    }

    @Command(desc = "Changes the owner of a portal")
    public void owner(CommandSource context, User owner, @Default Portal portal)
    {
        portal.config.owner = owner.getName();
        portal.config.save();
        i18n.send(context, POSITIVE, "{user} is now the owner of {name#portal}!", owner, portal.getName());
    }

    @Alias(value = "mvpd")
    @Command(alias = "dest", desc = "changes the destination of the selected portal")
    public void destination(CommandSource context,
        @Desc("A destination can be: here, <world> or p:<portal>") Destination destination,
        @Default Portal portal)
    {
        portal.config.destination = destination;
        portal.config.save();
        i18n.send(context, POSITIVE, "Portal destination set!");
    }

    @Alias(value = "mvprd")
    @Command(alias = "randdest", desc = "Changes the destination of the selected portal to a random position each time")
    public void randomDestination(CommandSource context, World world, @Default Portal portal)
    {
        this.destination(context, new RandomDestination(world), portal);
    }

    @Command(desc = "Changes a portals location")
    @Restricted(value = Player.class, msg = "You have to be ingame to do this!")
    public void location(Player context, @Default Portal portal)
    {
        if (!(selector.getSelection(context) instanceof Cuboid))
        {
            i18n.send(context, NEGATIVE, "Please select a cuboid first!");
            return;
        }
        this.module.removePortal(portal);
        Location<World> p1 = selector.getFirstPoint(context);
        Location<World> p2 = selector.getSecondPoint(context);
        portal.config.world = new ConfigWorld(p1.getExtent());
        portal.config.location.from = new Vector3i(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
        portal.config.location.to = new Vector3i(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
        portal.config.save();
        this.module.addPortal(portal);
        i18n.send(context, POSITIVE, "Portal {name} updated to your current selection!", portal.getName());
    }

    @Command(desc = "Modifies the location where a player exits when teleporting a portal")
    @Restricted(value = Player.class, msg = "You have to be ingame to do this!")
    public void exit(Player context, @Default Portal portal)
    {
        Location<World> location = context.getLocation();
        if (portal.config.world.getWorld() != location.getExtent())
        {
            // TODO range check? range in config
            i18n.send(context, NEGATIVE, "A portals exit cannot be in an other world than its location!");
            return;
        }
        portal.config.location.destination = new WorldTransform(location, context.getRotation());
        portal.config.save();
        i18n.send(context, POSITIVE, "The portal exit of portal {name} was set to your current location!", portal.getName());
    }

    @Command(desc = "Toggles safe teleportation for this portal")
    public void togglesafe(CommandSource context, @Default Portal portal)
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
    public void entity(CommandSource context, @Default Portal portal)
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
