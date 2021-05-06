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
package org.cubeengine.module.roles.commands.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.module.roles.RolesUtil;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;

import static java.util.stream.Collectors.toList;

@Singleton
public class PermissionCompleter implements ValueCompleter
{
    @Override
    public List<CommandCompletion> complete(CommandContext context, String currentInput)
    {
        Set<CommandCompletion> result = new HashSet<>();
        for (String permission : RolesUtil.allPermissions.stream().filter(p -> p.startsWith(currentInput)).collect(toList()))
        {
            String substring = permission.substring(currentInput.length());
            int i = substring.indexOf(".");
            if (i == -1)
            {
                result.add(CommandCompletion.of(permission));
            }
            else
            {
                result.add(CommandCompletion.of(permission.substring(0, currentInput.length() + i)));
            }
        }
        List<CommandCompletion> list = new ArrayList<>(result);
//        Collections.sort(list);
        return list;
    }

}
