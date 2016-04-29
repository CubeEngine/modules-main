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
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;

public class StopCommand
{
    private VanillaPlus module;

    public StopCommand(VanillaPlus module)
    {
        this.module = module;
    }

    @Command(alias = {"shutdown", "killserver", "quit"}, desc = "Shuts down the server")
    public void stop(CommandSource context, @Optional @Greed(INFINITE) String message)
    {
        if (message == null || message.isEmpty())
        {
            message = module.getConfig().improve.commandStopDefaultMessage;
        }
        message = ChatFormat.parseFormats(message);

        Sponge.getServer().shutdown(ChatFormat.fromLegacy(message, '&'));
    }
}
