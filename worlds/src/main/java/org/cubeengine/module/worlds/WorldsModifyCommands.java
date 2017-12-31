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
package org.cubeengine.module.worlds;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.storage.WorldProperties;

import javax.inject.Inject;

@Command(name = "modify", desc = "Worlds modify commands")
public class WorldsModifyCommands extends ContainerCommand
{
    private I18n i18n;

    @Inject
    public WorldsModifyCommands(CommandManager cm, I18n i18n)
    {
        super(cm, Worlds.class);
        this.i18n = i18n;
    }

    @Command(desc = "Sets the autoload behaviour")
    public void autoload(CommandSource context, WorldProperties world, @Optional Boolean set)
    {
        if (set == null)
        {
            set = !world.loadOnStartup();
        }
        world.setLoadOnStartup(set);
        if (set)
        {
            i18n.send(context, POSITIVE, "{world} will now autoload.", world);
            return;
        }
        i18n.send(context, POSITIVE, "{world} will no longer autoload.", world);
    }

    @Command(desc = "Sets whether structors generate")
    public void generateStructure(CommandSource context, WorldProperties world, @Optional Boolean set)
    {
        if (set == null)
        {
            set = !world.usesMapFeatures();
        }
        world.setMapFeaturesEnabled(set);
        if (set)
        {
            i18n.send(context, POSITIVE, "{world} will now generate structures", world);
            return;
        }
        i18n.send(context, POSITIVE, "{world} will no longer generate structures", world);
    }
}
