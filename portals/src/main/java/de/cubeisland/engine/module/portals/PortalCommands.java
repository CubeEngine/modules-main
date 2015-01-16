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
import java.util.Set;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.command.methodic.parametric.Optional;
import de.cubeisland.engine.command.parameter.FixedValues;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.module.service.Selector;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.WorldLocation;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.core.util.math.shape.Cuboid;
import de.cubeisland.engine.core.world.ConfigWorld;
import de.cubeisland.engine.module.portals.config.Destination;
import de.cubeisland.engine.module.portals.config.PortalConfig;
import org.bukkit.Location;
import org.bukkit.World;

import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;

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
    @Restricted(value = User.class, msg = "You must be ingame to do this!")
    public void create(CommandContext context, String name, @Optional Destination destination)
    {
        Selector selector = this.module.getCore().getModuleManager().getServiceManager().getServiceImplementation(Selector.class);
        User sender = (User)context.getSource();
        if (!(selector.getSelection(sender) instanceof Cuboid))
        {
            context.sendTranslated(NEGATIVE, "Please select a cuboid first!");
            return;
        }
        if (this.manager.getPortal(name) != null)
        {
            context.sendTranslated(NEGATIVE, "A portal named {input} already exists!", name);
            return;
        }
        Location p1 = selector.getFirstPoint(sender);
        Location p2 = selector.getSecondPoint(sender);
        PortalConfig config = this.module.getCore().getConfigFactory().create(PortalConfig.class);
        config.location.from = new BlockVector3(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
        config.location.to = new BlockVector3(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
        config.location.destination = new WorldLocation(sender.getLocation());
        config.owner = sender.getOfflinePlayer();
        config.world = new ConfigWorld(module.getCore().getWorldManager(), p1.getWorld());

        config.destination = destination;

        config.setFile(new File(manager.portalsDir, name + ".yml"));
        config.save();
        Portal portal = new Portal(module, manager, name, config);
        this.manager.addPortal(portal);
        sender.attachOrGet(PortalsAttachment.class, module).setPortal(portal);
        context.sendTranslated(POSITIVE, "Portal {name} created!", portal.getName());
        if (destination == null)
        {
            context.sendTranslated(POSITIVE, "Select a destination using the portal modify destination command");
        }
    }

    @Alias(value = "mvps")
    @Command(desc = "Selects an existing portal")
    @Restricted(value = User.class, msg = "You must be ingame to do this!")
    public void select(CommandContext context, Portal portal)
    {
        ((User)context.getSource()).attachOrGet(PortalsAttachment.class, module).setPortal(portal);
        context.sendTranslated(POSITIVE, "Portal selected: {name}", portal.getName());
    }

    @Alias(value ="mvpi")
    @Command(desc = "Show info about a portal")
    public void info(CommandContext context, @Default Portal portal)
    {
        portal.showInfo(context.getSource());
    }

    @Alias(value = "mvpr")
    @Command(desc = "Removes a portal permanently")
    public void remove(CommandContext context, @Default Portal portal)
    {
        portal.delete();
        context.sendTranslated(POSITIVE, "Portal {name} deleted", portal.getName());
    }

    public enum OnOff implements FixedValues
    {
        ON,OFF;

        @Override
        public String getName()
        {
            return this.name().toLowerCase();
        }
    }

    @Command(desc = "Shows debug portal information instead of teleporting")
    @Restricted(value = User.class, msg = "You must be ingame to do this!")
    public void debug(CommandContext context, @Optional OnOff onOff)
    {
        PortalsAttachment attachment = ((User)context.getSource()).attachOrGet(PortalsAttachment.class, module);
        if (onOff == null)
        {
            attachment.toggleDebug();
        }
        else
        {
            switch (onOff)
            {
                case OFF:
                    if (attachment.isDebug()) attachment.toggleDebug();
                    break;
                case ON:
                    if (!attachment.isDebug()) attachment.toggleDebug();
            }
        }
        if (attachment.isDebug())
        {
            context.sendTranslated(POSITIVE, "Portal debug mode ON!");
            return;
        }
        context.sendTranslated(POSITIVE, "Portal debug mode OFF!");
    }

    @Command(desc = "Lists the portals")
    public void list(CommandContext context, World world)
    {
        Set<Portal> portals = manager.getPortals(world);
        if (portals.isEmpty())
        {
            context.sendTranslated(POSITIVE, "There are no portals in {world}", world);
            return;
        }
        context.sendTranslated(POSITIVE, "The following portals are located in {world}", world);
        for (Portal portal : portals)
        {
            context.sendMessage(" - " + portal.getName());
        }
    }
}
