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

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command_old.reflected.Alias;
import de.cubeisland.engine.core.module.service.Selector;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.WorldLocation;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.core.util.math.shape.Cuboid;
import de.cubeisland.engine.module.portals.config.Destination;
import de.cubeisland.engine.module.portals.config.RandomDestination;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Command(name = "modify", desc = "modifies a portal", alias = "mvpm")
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
    @Params(positional = {@Param(label = "owner", type = User.class),
                          @Param(req = false, label = "portal")})
    public void owner(CommandContext context)
    {
        User user = context.get(0);
        Portal portal = getPortal(context, 1);
        if (portal == null)
        {
            return;
        }
        portal.config.owner = user.getOfflinePlayer();
        portal.config.save();
        context.sendTranslated(POSITIVE, "{user} is now the owner of {name#portal}!", user, portal.getName());
    }

    private Portal getPortal(CommandContext context, int index)
    {
        Portal portal = null;
        if (context.hasPositional(index))
        {
            portal = manager.getPortal(context.getString(index));
            if (portal == null)
            {
                context.sendTranslated(NEGATIVE, "Portal {input} not found!", context.getString(index));
                return null;
            }
        }
        else if (context.getSource() instanceof User)
        {
            portal = ((User)context.getSource()).attachOrGet(PortalsAttachment.class, module).getPortal();
        }
        if (portal == null)
        {
            context.sendTranslated(NEGATIVE, "You need to define a portal to use!");
            return null;
        }
        return portal;
    }

    @Alias(names = "mvpd")
    @Command(alias = "dest", desc = "changes the destination of the selected portal")
    @Params(positional = {@Param(label = "world", names = "here"), // TODO treat "names" on positional parameter as allowed fixed values?
                          @Param(req = false, label = "portal")})
    public void destination(CommandContext context)
    {
        Portal portal = getPortal(context, 1);
        if (portal == null)
        {
            return;
        }
        String arg0 = context.get(0);
        if ("here".equalsIgnoreCase(arg0))
        {
            if (!(context.getSource() instanceof User))
            {
                context.sendTranslated(NEUTRAL, "The Portal Agency will bring you your portal for just {text:$ 1337} within {amount} weeks", new Random().nextInt(51)+1);
                return;
            }
            portal.config.destination = new Destination(((User)context.getSource()).getLocation());
        }
        else if (arg0.startsWith("p:")) // TODO extract to separate (as its not possible to show in label) cmd /mvppd portaldestination
        {
            Portal destPortal = manager.getPortal(arg0.substring(2));
            if (destPortal == null)
            {
                context.sendTranslated(NEGATIVE, "Portal {input} not found!", arg0.substring(2));
                return;
            }
            portal.config.destination = new Destination(destPortal);
        }
        else
        {
            World world = this.module.getCore().getWorldManager().getWorld(arg0);
            if (world == null)
            {
                context.sendTranslated(NEGATIVE, "World {input} not found!", arg0);
                return;
            }
            portal.config.destination = new Destination(world);
        }
        portal.config.save();
        context.sendTranslated(POSITIVE, "Portal destination set!");
    }

    @Alias(names = "mvprd")
    @Command(alias = "randdest", desc = "Changes the destination of the selected portal to a random position each time")
    @Params(positional = {@Param(label = "world", type = World.class),
                          @Param(req = false, label = "portal")})
    public void randomDestination(CommandContext context)
    {
        Portal portal = getPortal(context, 1);
        if (portal == null)
        {
            return;
        }
        World world = context.get(0);
        portal.config.destination = new RandomDestination(world);
        portal.config.save();
    }

    @Command(desc = "Changes a portals location")
    @Params(positional = @Param(req = false, label = "portal"))
    public void location(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            User sender = (User)context.getSource();
            Selector selector = this.module.getCore().getModuleManager().getServiceManager().getServiceImplementation(Selector.class);
            if (selector.getSelection(sender) instanceof Cuboid)
            {
                Portal portal = sender.attachOrGet(PortalsAttachment.class, module).getPortal();
                if (context.hasPositional(0))
                {
                    portal = manager.getPortal(context.getString(0));
                    if (portal == null)
                    {
                        context.sendTranslated(NEGATIVE, "Portal {input} not found!", context.get(0));
                        return;
                    }
                }
                if (portal == null)
                {
                    context.sendTranslated(NEGATIVE, "You need to define a portal!");
                    return;
                }
                Location p1 = selector.getFirstPoint(sender);
                Location p2 = selector.getSecondPoint(sender);
                portal.config.location.from = new BlockVector3(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
                portal.config.location.to = new BlockVector3(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
                portal.config.save();
                context.sendTranslated(POSITIVE, "Portal {name} updated to your current selection!", portal.getName());
                return;
            }
            context.sendTranslated(NEGATIVE, "Please select a cuboid first!");
            return;
        }
        context.sendTranslated(NEGATIVE, "You have to be ingame to do this!");
    }

    @Command(desc = "Modifies the location where a player exits when teleporting a portal")
    @Params(positional = @Param(req = false, label = "portal"))
    public void exit(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            User sender = (User)context.getSource();
            Portal portal = sender.attachOrGet(PortalsAttachment.class, module).getPortal();
            if (context.hasPositional(0))
            {
                portal = manager.getPortal(context.getString(0));
                if (portal == null)
                {
                    context.sendTranslated(NEGATIVE, "Portal {input} not found!", context.get(0));
                    return;
                }
            }
            if (portal == null)
            {
                context.sendTranslated(NEGATIVE, "You need to define a portal!");
                return;
            }
            Location location = sender.getLocation();
            if (portal.config.world.getWorld() != location.getWorld())
            {
                context.sendTranslated(NEGATIVE, "A portals exit cannot be in an other world than its location!");
                return;
            }
            portal.config.location.destination = new WorldLocation(location);
            portal.config.save();
            context.sendTranslated(POSITIVE, "The portal exit of portal {name} was set to your current location!", portal.getName());
            return;
        }
        context.sendTranslated(NEGATIVE, "You have to be ingame to do this!");
    }

    @Command(desc = "Toggles safe teleportation for this portal")
    @Params(positional = @Param(req = false, label = "portal"))
    public void togglesafe(CommandContext context)
    {
        Portal portal = getPortal(context, 0);
        if (portal == null)
        {
            return;
        }
        portal.config.safeTeleport = !portal.config.safeTeleport;
        portal.config.save();
        if (portal.config.safeTeleport)
        {
            context.sendTranslated(POSITIVE, "The portal {name} will not teleport to an unsafe destination", portal.getName());
        }
        else
        {
            context.sendTranslated(POSITIVE, "The portal {name} will also teleport to an unsafe destination", portal.getName());
        }
    }

    @Command(desc = "Toggles whether entities can teleport with this portal")
    @Params(positional = @Param(req = false, label = "portal"))
    public void entity(CommandContext context)
    {
        Portal portal = getPortal(context, 0);
        if (portal == null)
        {
            return;
        }
        portal.config.teleportNonPlayers = !portal.config.teleportNonPlayers;
        portal.config.save();
        if (portal.config.teleportNonPlayers)
        {
            context.sendTranslated(POSITIVE, "The portal {name} will teleport entities too", portal.getName());
        }
        else
        {
            context.sendTranslated(POSITIVE, "The portal {name} will only teleport players", portal.getName());
        }
    }
}
