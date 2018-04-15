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
package org.cubeengine.module.protector.command.parser;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.Completer;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.List;

public class TristateParser implements ArgumentParser<Tristate>, Completer
{
    @Override
    public Tristate parse(Class aClass, CommandInvocation commandInvocation) throws ParserException
    {
        String token = commandInvocation.consume(1);
        switch (token.toLowerCase())
        {
            case "allow":
                return Tristate.TRUE;
            case "deny":
                return Tristate.FALSE;
            case "reset":
                return Tristate.UNDEFINED;
        }
        throw new ParserException("Unknown token " + token + " use allow/deny/reset");
    }

    @Override
    public List<String> suggest(Class aClass, CommandInvocation commandInvocation) {
        String token = commandInvocation.currentToken().toLowerCase();
        List<String> list = new ArrayList<>();
        if ("allow".startsWith(token))
        {
            list.add("allow");
        }
        if ("deny".startsWith(token))
        {
            list.add("deny");
        }
        if ("reset".startsWith(token))
        {
            list.add("reset");
        }
        return list;
    }
}
