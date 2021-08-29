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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.WorldType;
import org.spongepowered.api.world.WorldTypes;
import org.spongepowered.api.world.generation.ChunkGenerator;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Command(name = "modify", desc = "Worlds modify commands")
public class WorldsModifyCommands extends DispatcherCommand
{
    private I18n i18n;

    @Inject
    public WorldsModifyCommands(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Sets the autoload behaviour")
    public void autoload(CommandCause context, ServerWorldProperties world, @Option Boolean set)
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

    @Command(desc = "Sets whether features generate")
    public void generateFeatures(CommandCause context, ServerWorldProperties world, @Option Boolean set)
    {
        if (set == null)
        {
            set = !world.worldGenerationConfig().generateFeatures();
        }
        world.worldGenerationConfig().setGenerateFeatures(set);
        if (set)
        {
            i18n.send(context, POSITIVE, "{world} will now generate structures", world);
            return;
        }
        i18n.send(context, POSITIVE, "{world} will no longer generate structures", world);
    }

}
