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
package de.cubeisland.engine.module.roles.commands.provider;

import java.util.List;
import com.google.common.base.Optional;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.completer.Completer;
import de.cubeisland.engine.butler.parameter.reader.ArgumentReader;
import de.cubeisland.engine.butler.parameter.reader.DefaultValue;
import de.cubeisland.engine.butler.parameter.reader.ReaderException;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.world.World;

import static java.util.stream.Collectors.toList;

public class ContextReader implements ArgumentReader<Context>, Completer, DefaultValue<Context>
{
    private WorldManager wm;

    public ContextReader(RolesPermissionService service, WorldManager wm)
    {
        this.wm = wm;
    }

    @Override
    public Context read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String token = invocation.consume(1);
        if (token.contains("|"))
        {
            // TODO implement me
            // then look in mirrors for other contexts
        }
        else // world or global
        {
            if ("global".equalsIgnoreCase(token))
            {
                return new Context("global", "");
            }
            Optional<World> world = wm.getWorld(token);
            if (world.isPresent())
            {
                return new Context("world", token.toLowerCase());
            }
        }
        throw new ReaderException("Unknown context: {}", token);
    }

    @Override
    public Context getDefault(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof User)
        {
            return new Context("world", ((User)invocation.getCommandSource()).getWorld().getName());
        }
        throw new ReaderException("You have to provide a context");
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        String token = invocation.currentToken();
        List<String> list = wm.getWorlds().stream().map(World::getName).filter(n -> n.toLowerCase().startsWith(
            token.toLowerCase())).collect(toList());
        list.addAll(wm.getWorlds().stream()
                      .filter(world -> world.getName().startsWith(token))
                      .map(World::getName)
                      .collect(toList()));
        if ("global".startsWith(token.toLowerCase()))
        {
            list.add("global");
        }

        // TODO last ctx from mirror
        // TODO show world|<...> only if starting with world|
        return list;
    }
}
