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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.util.Tristate;

@ParserFor(Tristate.class)
public class TristateParser implements ValueParser<Tristate>, ValueCompleter
{

    @Override
    public List<CommandCompletion> complete(CommandContext context, String currentInput)
    {
        String token = currentInput.toLowerCase();
        List<CommandCompletion> list = new ArrayList<>();
        if ("allow".startsWith(token))
        {
            list.add(CommandCompletion.of("allow"));
        }
        if ("deny".startsWith(token))
        {
            list.add(CommandCompletion.of("deny"));
        }
        if ("reset".startsWith(token))
        {
            list.add(CommandCompletion.of("reset"));
        }
        return list;
    }

    @Override
    public Optional<? extends Tristate> parseValue(Key<? super Tristate> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        String token = reader.parseString();
        switch (token.toLowerCase())
        {
            case "allow":
                return Optional.of(Tristate.TRUE);
            case "deny":
                return Optional.of(Tristate.FALSE);
            case "reset":
                return Optional.of(Tristate.UNDEFINED);
        }
        throw reader.createException(Component.text("Unknown token " + token + " use allow/deny/reset"));
    }

}
