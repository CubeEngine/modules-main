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
package de.cubeisland.engine.module.portals.config;

import java.util.Random;
import com.google.common.base.Optional;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.parameter.reader.ArgumentReader;
import de.cubeisland.engine.butler.parameter.reader.ReaderException;
import de.cubeisland.engine.module.portals.Portal;
import de.cubeisland.engine.module.portals.Portals;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.service.world.WorldManager;
import org.spongepowered.api.world.World;

public class DestinationReader implements ArgumentReader<Destination>
{
    private final Portals module;
    private final WorldManager wm;
    private final Random random = new Random();

    public DestinationReader(Portals module, WorldManager wm)
    {
        this.module = module;
        this.wm = wm;
    }

    @Override
    public Destination read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String token = invocation.consume(1);
        if ("here".equalsIgnoreCase(token))
        {
            if ((invocation.getCommandSource() instanceof User))
            {
                return new Destination(wm, ((User)invocation.getCommandSource()).asPlayer().getLocation(), ((User)invocation.getCommandSource()).asPlayer().getRotation());
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
            Optional<World> world = wm.getWorld(token);
            if (!world.isPresent())
            {
                throw new ReaderException("World {input} not found!", token);
            }
            return new Destination(wm, world.get());
        }
    }
}
