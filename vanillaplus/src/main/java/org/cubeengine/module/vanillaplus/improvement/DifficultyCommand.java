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
package org.cubeengine.module.vanillaplus.improvement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.server.ServerWorld;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
public class DifficultyCommand
{
    private I18n i18n;

    @Inject
    public DifficultyCommand(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Changes the difficulty level of the server")
    public void difficulty(CommandCause context, @Option Difficulty difficulty,
                           @Default @Named({"world", "w", "in"}) ServerWorld world)
    {
        if (difficulty != null)
        {
            world.properties().setDifficulty(difficulty);
            i18n.send(context, POSITIVE, "The difficulty has been set to {input}!", difficulty.asComponent());
            return;
        }
        i18n.send(context, POSITIVE, "Current difficulty level: {input}", world.difficulty().asComponent());
        if (world.properties().hardcore())
        {
            i18n.send(context, POSITIVE, "The world {world} has the hardcore mode enabled.", world);
        }
    }
}
