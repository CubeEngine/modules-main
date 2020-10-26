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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DefaultParameterProvider;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.util.Tristate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
@ParserFor(Tristate.class)
public class TristateParser implements DefaultParameterProvider<Tristate>, ValueParser<Tristate>, ValueCompleter
{
    private I18n i18n;

    @Inject
    public TristateParser(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Override
    public Tristate apply(CommandCause commandCause)
    {
        return Tristate.TRUE;
    }

    @Override
    public List<String> complete(CommandContext context, String currentInput)
    {
        final ArrayList<String> list = new ArrayList<>();
        for (Tristate value : Tristate.values())
        {
            if (value.name().toLowerCase().startsWith(currentInput.toLowerCase()))
            {
                list.add(value.name());
            }
        }
        return list;
    }

    @Override
    public Optional<? extends Tristate> getValue(Key<? super Tristate> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        final String token = reader.parseString();
        try
        {
            return Optional.of(Tristate.valueOf(token.toUpperCase()));
        }
        catch (IllegalArgumentException e)
        {
            throw reader.createException(i18n.translate(context.getCause(), "Invalid Tristate value"));
        }
    }
}
