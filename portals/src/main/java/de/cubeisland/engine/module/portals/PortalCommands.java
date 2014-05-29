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

import org.bukkit.Location;
import org.bukkit.World;

import de.cubeisland.engine.core.command.CubeContext;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.command.reflected.context.IParams;
import de.cubeisland.engine.core.command.reflected.context.NParams;
import de.cubeisland.engine.core.command.reflected.context.Named;
import de.cubeisland.engine.core.command.parameterized.completer.WorldCompleter;
import de.cubeisland.engine.core.command.reflected.Alias;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.context.Grouped;
import de.cubeisland.engine.core.command.reflected.context.Indexed;
import de.cubeisland.engine.core.module.service.Selector;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.WorldLocation;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.core.util.math.shape.Cuboid;
import de.cubeisland.engine.core.world.ConfigWorld;
import de.cubeisland.engine.module.portals.config.Destination;
import de.cubeisland.engine.module.portals.config.PortalConfig;

import static de.cubeisland.engine.core.command.Commands.aliases;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;

public class PortalCommands extends ContainerCommand
{
    private final Portals module;
    private final PortalManager manager;

    public PortalCommands(Portals module, PortalManager manager)
    {
        super(module, "portals", "The portal commands", aliases("mvp"));
        this.module = module;
        this.manager = manager;
    }

    @Alias(names = "mvpc")
    @Command(desc = "Creates a new Portal")
    @IParams(@Grouped(@Indexed(label = "name")))
    @NParams({@Named(names = "worlddest", label = "world", completer = WorldCompleter.class, type = World.class),
              @Named(names = "portaldest", label = "portal")})
    public void create(CubeContext context)
    {
        if (context.getSender() instanceof User)
        {
            Selector selector = this.getModule().getCore().getModuleManager().getServiceManager().getServiceImplementation(Selector.class);
            User sender = (User)context.getSender();
            if (selector.getSelection(sender) instanceof Cuboid)
            {
                if (this.manager.getPortal(context.getString(0)) == null)
                {
                    Location p1 = selector.getFirstPoint(sender);
                    Location p2 = selector.getSecondPoint(sender);
                    PortalConfig config = this.getModule().getCore().getConfigFactory().create(PortalConfig.class);
                    config.location.from = new BlockVector3(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
                    config.location.to = new BlockVector3(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
                    config.location.destination = new WorldLocation(sender.getLocation());
                    config.owner = sender.getOfflinePlayer();
                    config.world = new ConfigWorld(module.getCore().getWorldManager(), p1.getWorld());
                    if (context.hasNamed("worlddest"))
                    {
                        World world = context.getArg("worlddest", null);
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
                    config.setFile(new File(manager.portalsDir, context.getArg(0) + ".yml"));
                    config.save();
                    Portal portal = new Portal(module, manager, context.getString(0), config);
                    this.manager.addPortal(portal);
                    sender.attachOrGet(PortalsAttachment.class, module).setPortal(portal);
                    context.sendTranslated(POSITIVE, "Portal {name} created! Select a destination using portal modify destination command", portal.getName());
                    return;
                }
                context.sendTranslated(NEGATIVE, "A portal named {input} already exists!", context.getArg(0));
            }
            else
            {
                context.sendTranslated(NEGATIVE, "Please select a cuboid first!");
            }
            return;
        }
        context.sendTranslated(NEGATIVE, "You must be ingame to do this!");
    }

    @Alias(names = "mvps")
    @Command(desc = "Selects an existing portal")
    @IParams(@Grouped(@Indexed(label = "portal")))
    public void select(CubeContext context)
    {
        Portal portal = this.manager.getPortal(context.getString(0));
        if (portal == null)
        {
            context.sendTranslated(NEGATIVE, "Portal {input} not found!", context.getArg(0));
            return;
        }
        if (context.getSender() instanceof User)
        {
            ((User)context.getSender()).attachOrGet(PortalsAttachment.class, module).setPortal(portal);
            context.sendTranslated(POSITIVE, "Portal selected: {name}", context.getArg(0));
            return;
        }
        context.sendTranslated(NEGATIVE, "You must be ingame to do this!");
    }

    @Alias(names ="mvpi")
    @Command(desc = "Show info about a portal")
    @IParams(@Grouped(req = false, value = @Indexed(label = "portal")))
    public void info(CubeContext context)
    {
        Portal portal = null;
        if (context.hasIndexed(0))
        {
            portal = manager.getPortal(context.getString(0));
            if (portal == null)
            {
                context.sendTranslated(NEGATIVE, "Portal {input} not found!", context.getArg(0));
                return;
            }
        }
        else if (context.getSender() instanceof User)
        {
            portal = ((User)context.getSender()).attachOrGet(PortalsAttachment.class, getModule()).getPortal();
        }
        if (portal == null)
        {
            context.sendTranslated(NEGATIVE, "You need to define a portal to use!");
            context.sendMessage(context.getCommand().getUsage(context));
            return;
        }
        portal.showInfo(context.getSender());
    }

    @Alias(names = "mvpr")
    @Command(desc = "Removes a portal permanently")
    @IParams(@Grouped(@Indexed(label = "portal")))
    public void remove(CubeContext context)
    {
        Portal portal = this.manager.getPortal(context.getString(0));
        if (portal == null)
        {
            context.sendTranslated(NEGATIVE, "Portal {input} not found!", context.getArg(0));
            return;
        }
        portal.delete();
        context.sendTranslated(POSITIVE, "Portal {name} deleted", portal.getName());
    }

    @Command(desc = "Shows debug portal information instead of teleporting")
    @IParams(@Grouped(req = false, value = @Indexed(label = {"!on","!off"})))
    public void debug(CubeContext context)
    {
        if (context.getSender() instanceof User)
        {
            PortalsAttachment attachment = ((User)context.getSender()).attachOrGet(PortalsAttachment.class, module);
            if (context.hasIndexed(0))
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
}
