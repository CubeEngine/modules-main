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
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.completer.WorldCompleter;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.core.module.service.Selector;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.WorldLocation;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.core.util.math.shape.Cuboid;
import de.cubeisland.engine.core.world.ConfigWorld;
import de.cubeisland.engine.module.portals.config.Destination;
import de.cubeisland.engine.module.portals.config.PortalConfig;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Command(name = "portals", desc = "The portal commands", alias = "mvp")
public class PortalCommands extends CommandContainer
{
    private final Portals module;
    private final PortalManager manager;

    public PortalCommands(Portals module, PortalManager manager)
    {
        super(module);
        this.module = module;
        this.manager = manager;
    }

    @Alias(value = "mvpc")
    @Command(desc = "Creates a new Portal")
    @Params(positional = @Param(label = "name"),
            nonpositional = {@Param(names = "worlddest", label = "world", completer = WorldCompleter.class, type = World.class),
                             @Param(names = "portaldest", label = "portal")})
    public void create(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            Selector selector = this.module.getCore().getModuleManager().getServiceManager().getServiceImplementation(Selector.class);
            User sender = (User)context.getSource();
            if (selector.getSelection(sender) instanceof Cuboid)
            {
                if (this.manager.getPortal(context.getString(0)) == null)
                {
                    Location p1 = selector.getFirstPoint(sender);
                    Location p2 = selector.getSecondPoint(sender);
                    PortalConfig config = this.module.getCore().getConfigFactory().create(PortalConfig.class);
                    config.location.from = new BlockVector3(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
                    config.location.to = new BlockVector3(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
                    config.location.destination = new WorldLocation(sender.getLocation());
                    config.owner = sender.getOfflinePlayer();
                    config.world = new ConfigWorld(module.getCore().getWorldManager(), p1.getWorld());
                    if (context.hasNamed("worlddest"))
                    {
                        World world = context.get("worlddest", null);
                        if (world == null)
                        {
                            context.sendTranslated(NEGATIVE, "World {input} not found!", context.getString("worlddest"));
                            return;
                        }
                        config.destination = new Destination(world);
                    }
                    if (context.hasNamed("portaldest"))
                    {
                        Portal portal = this.manager.getPortal(context.getString("portaldest"));
                        if (portal == null)
                        {
                            context.sendTranslated(NEGATIVE, "Portal {input} not found!", context.getString("portaldest"));
                            return;
                        }
                        config.destination = new Destination(portal);
                    }
                    config.setFile(new File(manager.portalsDir, context.get(0) + ".yml"));
                    config.save();
                    Portal portal = new Portal(module, manager, context.getString(0), config);
                    this.manager.addPortal(portal);
                    sender.attachOrGet(PortalsAttachment.class, module).setPortal(portal);
                    context.sendTranslated(POSITIVE, "Portal {name} created! Select a destination using portal modify destination command", portal.getName());
                    return;
                }
                context.sendTranslated(NEGATIVE, "A portal named {input} already exists!", context.get(0));
            }
            else
            {
                context.sendTranslated(NEGATIVE, "Please select a cuboid first!");
            }
            return;
        }
        context.sendTranslated(NEGATIVE, "You must be ingame to do this!");
    }

    @Alias(value = "mvps")
    @Command(desc = "Selects an existing portal")
    @Params(positional = @Param(label = "portal"))
    public void select(CommandContext context)
    {
        Portal portal = this.manager.getPortal(context.getString(0));
        if (portal == null)
        {
            context.sendTranslated(NEGATIVE, "Portal {input} not found!", context.get(0));
            return;
        }
        if (context.getSource() instanceof User)
        {
            ((User)context.getSource()).attachOrGet(PortalsAttachment.class, module).setPortal(portal);
            context.sendTranslated(POSITIVE, "Portal selected: {name}", context.get(0));
            return;
        }
        context.sendTranslated(NEGATIVE, "You must be ingame to do this!");
    }

    @Alias(value ="mvpi")
    @Command(desc = "Show info about a portal")
    @Params(positional = @Param(req = false, label = "portal"))
    public void info(CommandContext context)
    {
        Portal portal = null;
        if (context.hasPositional(0))
        {
            portal = manager.getPortal(context.getString(0));
            if (portal == null)
            {
                context.sendTranslated(NEGATIVE, "Portal {input} not found!", context.get(0));
                return;
            }
        }
        else if (context.getSource() instanceof User)
        {
            portal = ((User)context.getSource()).attachOrGet(PortalsAttachment.class, module).getPortal();
        }
        if (portal == null)
        {
            context.sendTranslated(NEGATIVE, "You need to define a portal to use!");
            return;
        }
        portal.showInfo(context.getSource());
    }

    @Alias(value = "mvpr")
    @Command(desc = "Removes a portal permanently")
    @Params(positional = @Param(label = "portal"))
    public void remove(CommandContext context)
    {
        Portal portal = this.manager.getPortal(context.getString(0));
        if (portal == null)
        {
            context.sendTranslated(NEGATIVE, "Portal {input} not found!", context.get(0));
            return;
        }
        portal.delete();
        context.sendTranslated(POSITIVE, "Portal {name} deleted", portal.getName());
    }

    @Command(desc = "Shows debug portal information instead of teleporting")
    @Params(positional = @Param(req = false, names = {"on","off"}))
    public void debug(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            PortalsAttachment attachment = ((User)context.getSource()).attachOrGet(PortalsAttachment.class, module);
            if (context.hasPositional(0))
            {
                if ("on".equalsIgnoreCase(context.getString(0)))
                {
                    if (!attachment.isDebug())
                    {
                        attachment.toggleDebug();
                    }
                }
                else if ("off".equalsIgnoreCase(context.getString(0)))
                {
                    if (attachment.isDebug())
                    {
                        attachment.toggleDebug();
                    }
                    context.sendTranslated(NEUTRAL, "[Portals] Debug {text:OFF}");
                }
            }
            else
            {
                attachment.toggleDebug();
            }
            if (attachment.isDebug())
            {
                context.sendTranslated(NEUTRAL, "[Portals] Debug {text:ON}");
            }
            else
            {
                context.sendTranslated(POSITIVE, "Toggled debug mode!");
            }
            return;
        }
        context.sendTranslated(NEGATIVE, "You must be ingame to do this!");
    }

    @Command(desc = "Lists the portals")
    @Params(positional = @Param(label = "world", type = World.class))
    public void list(CommandContext context)
    {
        Set<Portal> portals = new HashSet<>();
        for (Portal portal : manager.getPortals())
        {
            if (portal.getWorld().equals(context.get(0)))
            {
                portals.add(portal);
            }
        }
        if (portals.isEmpty())
        {
            context.sendTranslated(POSITIVE, "There are no portals in {world}", context.get(0));
            return;
        }
        context.sendTranslated(POSITIVE, "The following portals are located in {world}", context.get(0));
        for (Portal portal : portals)
        {
            context.sendMessage(" - " + portal.getName());
        }
    }
}
