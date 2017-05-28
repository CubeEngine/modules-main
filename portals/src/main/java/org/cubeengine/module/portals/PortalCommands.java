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

import java.util.Set;

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parameter.FixedValues;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.portals.config.Destination;
import org.cubeengine.module.portals.config.PortalConfig;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.config.WorldTransform;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "portals", desc = "The portal commands", alias = "mvp")
public class PortalCommands extends ContainerCommand
{
    private final Portals module;
    private Selector selector;
    private Reflector reflector;
    private I18n i18n;

    public PortalCommands(CommandManager base, Portals module, Selector selector, Reflector reflector, I18n i18n)
    {
        super(base, Portals.class);
        this.module = module;
        this.selector = selector;
        this.reflector = reflector;
        this.i18n = i18n;
    }

    @Alias(value = "mvpc")
    @Command(desc = "Creates a new Portal")
    @Restricted(value = Player.class, msg = "You must be ingame to do this!")
    public void create(Player context, String name, @Optional Destination destination)
    {
        if (!(selector.getSelection(context) instanceof Cuboid))
        {
            i18n.send(context, NEGATIVE, "Please select a cuboid first!");
            return;
        }
        if (module.getPortal(name) != null)
        {
            i18n.send(context, NEGATIVE, "A portal named {input} already exists!", name);
            return;
        }
        Location p1 = selector.getFirstPoint(context);
        Location p2 = selector.getSecondPoint(context);
        PortalConfig config = reflector.create(PortalConfig.class);
        config.location.from = new Vector3i(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
        config.location.to = new Vector3i(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
        config.location.destination = new WorldTransform(context.getLocation(), context.getRotation());
        config.owner = context.getName();
        config.world = new ConfigWorld((World)p1.getExtent());

        config.destination = destination;

        config.setFile(module.getPortalFile(name));
        config.save();
        Portal portal = new Portal(module, name, config, i18n);
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
    @Restricted(value = Player.class, msg = "You must be ingame to do this!")
    public void select(Player context, Portal portal)
    {
        module.getPortalsAttachment(context.getUniqueId()).setPortal(portal);
        i18n.send(context, POSITIVE, "Portal selected: {name}", portal.getName());
    }

    @Alias(value ="mvpi")
    @Command(desc = "Show info about a portal")
    public void info(CommandSource context, @Default Portal portal)
    {
        portal.showInfo(context);
    }

    @Alias(value = "mvpr")
    @Command(desc = "Removes a portal permanently")
    public void remove(CommandSource context, @Default Portal portal)
    {
        portal.delete();
        i18n.send(context, POSITIVE, "Portal {name} deleted", portal.getName());
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
    @Restricted(value = Player.class, msg = "You must be ingame to do this!")
    public void debug(Player context, @Optional OnOff onOff)
    {
        PortalsAttachment attachment = module.getPortalsAttachment(context.getUniqueId());
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
            i18n.send(context, POSITIVE, "Portal debug mode ON!");
            return;
        }
        i18n.send(context, POSITIVE, "Portal debug mode OFF!");
    }

    @Alias("mvpl")
    @Command(desc = "Lists the portals")
    public void list(CommandSource context, @Default World world)
    {
        Set<Portal> portals = module.getPortals(world);
        if (portals.isEmpty())
        {
            i18n.send(context, POSITIVE, "There are no portals in {world}", world);
            return;
        }
        i18n.send(context, POSITIVE, "The following portals are located in {world}", world);
        for (Portal portal : portals)
        {
            context.sendMessage(Text.of(" - ", portal.getName()));
        }
    }
}
