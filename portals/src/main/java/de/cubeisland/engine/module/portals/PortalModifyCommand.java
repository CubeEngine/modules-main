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

import org.bukkit.Location;
import org.bukkit.World;

import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.command.methodic.parametric.Desc;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.module.service.Selector;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.WorldLocation;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.core.util.math.shape.Cuboid;
import de.cubeisland.engine.module.portals.config.Destination;
import de.cubeisland.engine.module.portals.config.RandomDestination;

import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;

@Alias("mvpm")
@Command(name = "modify", desc = "modifies a portal")
public class PortalModifyCommand extends CommandContainer
{
    private Portals module;
    private final PortalManager manager;

    public PortalModifyCommand(Portals module, PortalManager manager)
    {
        super(module);
        this.module = module;
        this.manager = manager;
    }

    @Command(desc = "Changes the owner of a portal")
    public void owner(CommandContext context, User owner, @Default Portal portal)
    {
        portal.config.owner = owner.getOfflinePlayer();
        portal.config.save();
        context.sendTranslated(POSITIVE, "{user} is now the owner of {name#portal}!", owner, portal.getName());
    }

    @Alias(value = "mvpd")
    @Command(alias = "dest", desc = "changes the destination of the selected portal")
    public void destination(CommandContext context,
        @Desc("A destination can be: here, <world> or p:<portal>") Destination destination,
        @Default Portal portal)
    {
        portal.config.destination = destination;
        portal.config.save();
        context.sendTranslated(POSITIVE, "Portal destination set!");
    }

    @Alias(value = "mvprd")
    @Command(alias = "randdest", desc = "Changes the destination of the selected portal to a random position each time")
    public void randomDestination(CommandContext context, World world, @Default Portal portal)
    {
        this.destination(context, new RandomDestination(world), portal);
    }

    @Command(desc = "Changes a portals location")
    @Restricted(value = User.class, msg = "You have to be ingame to do this!")
    public void location(CommandContext context, @Default Portal portal)
    {
        User sender = (User)context.getSource();
        Selector selector = this.module.getCore().getModuleManager().getServiceManager().getServiceImplementation(Selector.class);
        if (!(selector.getSelection(sender) instanceof Cuboid))
        {
            context.sendTranslated(NEGATIVE, "Please select a cuboid first!");
            return;
        }
        Location p1 = selector.getFirstPoint(sender);
        Location p2 = selector.getSecondPoint(sender);
        portal.config.location.from = new BlockVector3(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
        portal.config.location.to = new BlockVector3(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
        portal.config.save();
        context.sendTranslated(POSITIVE, "Portal {name} updated to your current selection!", portal.getName());
    }

    @Command(desc = "Modifies the location where a player exits when teleporting a portal")
    @Restricted(value = User.class, msg = "You have to be ingame to do this!")
    public void exit(CommandContext context, @Default Portal portal)
    {
        User sender = (User)context.getSource();
        Location location = sender.getLocation();
        if (portal.config.world.getWorld() != location.getWorld())
        {
            // TODO range check? range in config
            context.sendTranslated(NEGATIVE, "A portals exit cannot be in an other world than its location!");
            return;
        }
        portal.config.location.destination = new WorldLocation(location);
        portal.config.save();
        context.sendTranslated(POSITIVE, "The portal exit of portal {name} was set to your current location!", portal.getName());
    }

    @Command(desc = "Toggles safe teleportation for this portal")
    public void togglesafe(CommandContext context, @Default Portal portal)
    {
        portal.config.safeTeleport = !portal.config.safeTeleport;
        portal.config.save();
        if (portal.config.safeTeleport)
        {
            context.sendTranslated(POSITIVE, "The portal {name} will not teleport to an unsafe destination", portal.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "The portal {name} will also teleport to an unsafe destination", portal.getName());
    }

    @Command(desc = "Toggles whether entities can teleport with this portal")
    public void entity(CommandContext context, @Default Portal portal)
    {
        portal.config.teleportNonPlayers = !portal.config.teleportNonPlayers;
        portal.config.save();
        if (portal.config.teleportNonPlayers)
        {
            context.sendTranslated(POSITIVE, "The portal {name} will teleport entities too", portal.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "The portal {name} will only teleport players", portal.getName());
    }
}
