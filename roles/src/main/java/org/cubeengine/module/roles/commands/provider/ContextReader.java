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
package org.cubeengine.module.roles.commands.provider;

import java.util.List;
import java.util.Optional;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.DefaultValue;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.world.World;

import static java.util.stream.Collectors.toList;

public class ContextReader implements ArgumentReader<Context>, Completer, DefaultValue<Context>
{
    private RolesPermissionService service;
    private Game game;

    public ContextReader(RolesPermissionService service, Game game)
    {
        this.service = service;
        this.game = game;
    }

    @Override
    public Context read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String token = invocation.consume(1);
        String checkToken = token.toLowerCase();
        if ("global".equalsIgnoreCase(token))
        {
            return new Context("global", "");
        }
        if (token.contains("|"))
        {
            String[] parts = token.split("\\|", 2);
            if (!parts[0].equals("world"))
            {
                return new Context(parts[0], parts[1]);
            }
            if (!isValidWorld(parts[1]))
            {
                throw new ReaderException("Unknown context: {}", token);
                // TODO look in mirrors for other contexts
            }
            checkToken = parts[1];
        }
        if (isValidWorld(checkToken)) // try for world
        {
            return new Context("world", checkToken);
        }
        throw new ReaderException("Unknown context: {}", token);
    }

    private boolean isValidWorld(String token)
    {
        return game.getServer().getWorld(token).isPresent();
    }

    @Override
    public Context getDefault(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof Player)
        {
            return new Context("world", ((Player)invocation.getCommandSource()).getWorld().getName());
        }
        throw new ReaderException("You have to provide a context");
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        String token = invocation.currentToken();
        List<String> list = game.getServer().getWorlds().stream()
                .map(World::getName)
                .filter(n -> n.toLowerCase().startsWith(token.toLowerCase()))
                .collect(toList());
        if ("global".startsWith(token.toLowerCase()))
        {
            list.add("global");
        }

        if (token.equals("world") || token.toLowerCase().startsWith("world|"))
        {
            String subToken = token.equals("world") ? "" : token.substring(6);
            list.addAll(game.getServer().getWorlds().stream()
                    .map(World::getName)
                    .filter(n -> n.toLowerCase().startsWith(subToken.toLowerCase()))
                    .map(n -> "world|" + n)
                    .collect(toList()));
        }
        // TODO last ctx from mirror
        return list;
    }
}
