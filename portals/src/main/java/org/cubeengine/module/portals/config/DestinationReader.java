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
package org.cubeengine.module.portals.config;

import java.util.Optional;
import java.util.Random;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.module.portals.Portal;
import org.cubeengine.module.portals.Portals;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

public class DestinationReader implements ArgumentReader<Destination>
{
    private final Portals module;
    private I18n i18n;
    private final Random random = new Random();

    public DestinationReader(Portals module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Override
    public Destination read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String token = invocation.consume(1);
        if ("here".equalsIgnoreCase(token))
        {
            if ((invocation.getCommandSource() instanceof Player))
            {
                return new Destination(((Player)invocation.getCommandSource()).getLocation(), ((Player)invocation.getCommandSource()).getRotation(), i18n);
            }
            throw new ReaderException(
                "The Portal Agency will bring you your portal for just {text:$ 1337} within {input#amount} weeks",
                String.valueOf(random.nextInt(51) + 1));
        }
        else if (token.startsWith("p:")) // portal dest
        {
            Portal destPortal = module.getPortal(token.substring(2));
            if (destPortal == null)
            {
                throw new ReaderException("Portal {input} not found!", token.substring(2));
            }
            return new Destination(destPortal);
        }
        else // world
        {
            Optional<World> world = Sponge.getServer().getWorld(token);
            if (!world.isPresent())
            {
                throw new ReaderException("World {input} not found!", token);
            }
            return new Destination(world.get());
        }
    }
}
