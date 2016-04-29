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
package org.cubeengine.module.vanillaplus.improvement;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.difficulty.Difficulty;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class DifficultyCommand
{
    private I18n i18n;

    public DifficultyCommand(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Changes the difficulty level of the server")
    public void difficulty(CommandSource context, @Optional Difficulty difficulty,
                           @Default @Named({"world", "w", "in"}) World world)
    {
        if (difficulty != null)
        {
            world.getProperties().setDifficulty(difficulty);
            i18n.sendTranslated(context, POSITIVE, "The difficulty has been set to {input}!", difficulty.getTranslation());
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "Current difficulty level: {input}", world.getDifficulty().getName());
        if (world.getProperties().isHardcore())
        {
            i18n.sendTranslated(context, POSITIVE, "The world {world} has the hardcore mode enabled.", world);
        }
    }
}
