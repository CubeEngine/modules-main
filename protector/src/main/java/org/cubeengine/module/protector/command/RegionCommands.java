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
package org.cubeengine.module.protector.command;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

@Command(name = "region", desc = "Manages the regions")
public class RegionCommands extends ContainerCommand
{
    private Selector selector;
    private RegionManager manager;
    private I18n i18n;

    public RegionCommands(CommandManager base, Selector selector, RegionManager manager, I18n i18n)
    {
        super(base, Protector.class);
        this.selector = selector;
        this.manager = manager;
        this.i18n = i18n;
    }

    @Command(desc = "Defines a new Region")
    public void define(Player context, String name)
    {
        Shape shape = selector.getSelection(context);
        if (shape == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "Nothing selected!");
            return;
        }
        World world = selector.getFirstPoint(context).getExtent();
        if (manager.hasRegion(world, name))
        {
            i18n.sendTranslated(context, NEGATIVE, "There is already a Region named {name}", name);
            return;
        }
        Region region = manager.newRegion(world, shape.getBoundingCuboid(), name);
        manager.setActiveRegion(context, region);
        i18n.sendTranslated(context, POSITIVE, "Region {name} created!", region.getName());
    }

    @Command(desc = "Redefines an existing Region")
    public void redefine(Player context, @Default Region region)
    {
        Shape shape = selector.getSelection(context);
        if (shape == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "Nothing selected!");
            return;
        }
        World world = selector.getFirstPoint(context).getExtent();
        if (!region.getWorld().equals(world))
        {
            i18n.sendTranslated(context, NEGATIVE, "This region is in another world!");
            return;
        }
        manager.changeRegion(region, shape.getBoundingCuboid());
        i18n.sendTranslated(context, POSITIVE, "Region {name} updated!", region.getName());
    }

    @Command(desc = "Selects a Region")
    public void select(CommandSource context, Region region)
    {
        manager.setActiveRegion(context, region);
        if (context instanceof Player)
        {
            selector.setFirstPoint(((Player) context), new Location<>(region.getWorld(), region.getCuboid().getMinimumPoint()));
            selector.setSecondPoint(((Player) context), new Location<>(region.getWorld(), region.getCuboid().getMaximumPoint()));
        }
        i18n.sendTranslated(context, POSITIVE, "Region {name} selected!", region.getName());
    }

    public void list(CommandSource context, @Optional String match)
    {

    }

    public void priority(CommandSource context, int priority, @Default Region region)
    {

    }

    public void info(CommandSource context, @Default Region region)
    {

    }

    public void show(CommandSource context)
    {

    }

    public void parent(CommandSource context, Region parent, @Default Region region)
    {

    }

    public void teleport(CommandSource context, @Default Region region)
    {

    }

    public void redstonedefine(CommandSource context, String name)
    {

    }
}
