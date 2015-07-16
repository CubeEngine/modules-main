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

import java.util.Set;
import de.cubeisland.engine.butler.alias.Alias;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parameter.FixedValues;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.core.util.WorldLocation;
import de.cubeisland.engine.module.core.util.math.BlockVector3;
import de.cubeisland.engine.module.core.util.math.shape.Cuboid;
import de.cubeisland.engine.module.portals.config.Destination;
import de.cubeisland.engine.module.portals.config.PortalConfig;
import de.cubeisland.engine.service.Selector;
import de.cubeisland.engine.service.command.CommandContext;
import de.cubeisland.engine.service.command.ContainerCommand;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.service.world.ConfigWorld;
import de.cubeisland.engine.service.world.WorldManager;
import de.cubeisland.engine.reflect.Reflector;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.service.i18n.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "portals", desc = "The portal commands", alias = "mvp")
public class PortalCommands extends ContainerCommand
{
    private final Portals module;
    private Selector selector;
    private Reflector reflector;
    private WorldManager wm;

    public PortalCommands(Portals module, Selector selector, Reflector reflector, WorldManager wm)
    {
        super(module);
        this.module = module;
        this.selector = selector;
        this.reflector = reflector;
        this.wm = wm;
    }

    @Alias(value = "mvpc")
    @Command(desc = "Creates a new Portal")
    @Restricted(value = User.class, msg = "You must be ingame to do this!")
    public void create(CommandContext context, String name, @Optional Destination destination)
    {
        User sender = (User)context.getSource();
        if (!(selector.getSelection(sender) instanceof Cuboid))
        {
            context.sendTranslated(NEGATIVE, "Please select a cuboid first!");
            return;
        }
        if (module.getPortal(name) != null)
        {
            context.sendTranslated(NEGATIVE, "A portal named {input} already exists!", name);
            return;
        }
        Location p1 = selector.getFirstPoint(sender);
        Location p2 = selector.getSecondPoint(sender);
        PortalConfig config = reflector.create(PortalConfig.class);
        config.location.from = new BlockVector3(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
        config.location.to = new BlockVector3(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
        config.location.destination = new WorldLocation(sender.asPlayer().getLocation(), sender.asPlayer().getRotation());
        config.owner = sender.getUser().getName();
        config.world = new ConfigWorld(wm, (World)p1.getExtent());

        config.destination = destination;

        config.setFile(module.getPortalFile(name));
        config.save();
        Portal portal = new Portal(module, name, config);
        module.addPortal(portal);
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

    @Alias("mvpl")
    @Command(desc = "Lists the portals")
    public void list(CommandContext context, @Default World world)
    {
        Set<Portal> portals = module.getPortals(world);
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
