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
package org.cubeengine.module.travel.home;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.module.travel.config.Home;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.List;

public class HomeCompleter implements Completer
{
    private HomeManager manager;

    public HomeCompleter(HomeManager manager)
    {
        this.manager = manager;
    }

    @Override
    public List<String> suggest(Class type, CommandInvocation invocation)
    {
        List<String> list = new ArrayList<>();
        if (invocation.getCommandSource() instanceof Player)
        {
            String token = invocation.currentToken();
            for (Home home : manager.list(((Player) invocation.getCommandSource()), true, true))
            {
                if (home.name.startsWith(token))
                {
                    list.add(home.name);
                }
            }
        }
        return list;
    }
}
