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

import org.cubeengine.butler.parameter.TooFewArgumentsException;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.difficulty.Difficulty;

public class DifficultyCommand
{

    @Command(desc = "Changes the difficulty level of the server")
    public void difficulty(CommandSender context, @Optional Difficulty difficulty, @Named({"world", "w", "in"}) World world)
    {
        if (world == null)
        {
            if (context instanceof User)
            {
                world = ((User)context).getWorld();
            }
            else
            {
                context.sendTranslated(NEGATIVE, "You have to specify a world");
                throw new TooFewArgumentsException();
            }
        }
        if (difficulty != null)
        {
            world.getProperties().setDifficulty(difficulty); // TODO is this saved?
            context.sendTranslated(POSITIVE, "The difficulty has been successfully set!");
            return;
        }
        context.sendTranslated(POSITIVE, "Current difficulty level: {input}", world.getDifficulty().getName());
        if (world.getProperties().isHardcore())
        {
            context.sendTranslated(POSITIVE, "The world {world} has the hardcore mode enabled.", world);
        }
    }
}
