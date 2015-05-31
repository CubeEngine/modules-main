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
package de.cubeisland.engine.module.roles;

import java.util.List;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.completer.Completer;
import de.cubeisland.engine.butler.parameter.reader.ArgumentReader;
import de.cubeisland.engine.butler.parameter.reader.ReaderException;
import de.cubeisland.engine.module.roles.commands.ContextualRole;
import org.spongepowered.api.service.permission.context.Context;

public class ContextReader implements ArgumentReader<Context>, Completer
{
    // TODO implement me
    @Override
    public Context read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String token = invocation.currentToken();
        if (token.contains("|"))
        {
            // then look in mirrors for other contexts
        }
        else // world or global
        {
            if ("global".equalsIgnoreCase(token))
            {
                return new Context("global", "");
            }
            // TODO check if world exists
        }
        return null;
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        // first worlds
        // then global
        // last ctx from mirror
        return null;
    }
}
