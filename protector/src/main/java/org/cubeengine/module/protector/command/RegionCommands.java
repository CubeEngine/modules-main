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

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.command.CommandSource;

@Command(name = "region", desc = "Manages the regions")
public class RegionCommands extends ContainerCommand
{

    private Selector selector;

    public RegionCommands(CommandManager base, Selector selector)
    {
        super(base, Protector.class);
        this.selector = selector;
    }

    public void define(CommandSource context, String name)
    {

    }

    public void redefine(CommandSource context, @Default Region region)
    {

    }

    public void select(CommandSource context, Region region)
    {

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
