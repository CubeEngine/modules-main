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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.completer.Completer;
import de.cubeisland.engine.module.service.permission.PermissionManager;

import static java.util.stream.Collectors.toList;

public class PermissionCompleter implements Completer
{
    private PermissionManager pm;

    public PermissionCompleter(PermissionManager pm)
    {
        this.pm = pm;
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        Set<String> result = new HashSet<>();
        String token = invocation.consume(1);
        for (String permission : pm.getPermissions().keySet().stream().filter(p -> p.startsWith(token)).collect(toList()))
        {
            String substring = permission.substring(token.length());
            int i = substring.indexOf(".");
            if (i == -1)
            {
                result.add(permission);
            }
            else
            {
                result.add(permission.substring(0, token.length() + i));
            }
        }
        ArrayList<String> list = new ArrayList<>(result);
        Collections.sort(list);
        return list;
    }
}
