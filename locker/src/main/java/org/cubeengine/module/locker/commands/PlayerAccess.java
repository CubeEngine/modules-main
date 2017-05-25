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
package org.cubeengine.module.locker.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.libcube.util.StringUtils;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;

import static java.util.stream.Collectors.toList;

public class PlayerAccess
{
    public final User user;
    public final boolean admin;
    public final boolean add;

    public PlayerAccess(User user, boolean admin, boolean add)
    {
        this.user = user;
        this.admin = admin;
        this.add = add;
    }

    public static class PlayerAccessReader implements ArgumentReader<PlayerAccess>, Completer
    {
        @Override
        public PlayerAccess read(Class clazz, CommandInvocation invocation) throws ReaderException
        {
            String token = invocation.currentToken();
            boolean admin = token.startsWith("@");
            boolean add = !token.startsWith("-");

            token = admin || !add ? token.substring(1) : token;

            User user = Sponge.getServer().getPlayer(token)
                              .map(User.class::cast)
                              .orElse(Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(token)
                            .orElse(null));

            if (user == null)
            {
                throw new IllegalArgumentException("User not found");
            }
            invocation.consume(1);

            return new PlayerAccess(user, admin, add);
        }

        @Override
        public List<String> getSuggestions(CommandInvocation invocation)
        {
            List<String> list = new ArrayList<>();
            String[] parts = StringUtils.explode(",", invocation.currentToken(), true);

            String token = parts[parts.length - 1];

            if (token.isEmpty())
            {
                list.add(invocation.currentToken() + "-");
                list.add(invocation.currentToken() + "@");
            }
            else
            {
                String join = String.join(",", Arrays.copyOfRange(parts, 0, parts.length - 1));

                String prefix = "";
                if (token.startsWith("-") || token.startsWith("@"))
                {
                    prefix = token.substring(0, 1);
                    token = token.substring(1);
                }

                final String name = token;
                if (!join.isEmpty())
                {
                    prefix = "," + prefix;
                }
                final String pre = prefix;
                list.addAll(Sponge.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(p -> p.startsWith(name))
                        .map(p -> join + pre + p)
                        .collect(toList()));
            }

            return list;
        }
    }
}
