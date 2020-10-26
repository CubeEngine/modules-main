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
import java.util.List;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.service.permission.Subject;

@Singleton
@ParserFor(FileSubject.class)
public class FileSubjectParser implements ValueParser<FileSubject>, ValueCompleter
{
    private RolesPermissionService service;
    private I18n i18n;

    @Inject
    public FileSubjectParser(RolesPermissionService service, I18n i18n)
    {
        this.service = service;
        this.i18n = i18n;
    }

    @Override
    public List<String> complete(CommandContext context, String currentInput)
    {
        ArrayList<String> result = new ArrayList<>();
        for (Subject subject : service.getGroupSubjects().getLoadedSubjects())
        {
            String name = subject.getIdentifier();
            if (name.startsWith(currentInput))
            {
                result.add(name);
            }
        }
        return result;
    }

    @Override
    public Optional<? extends FileSubject> getValue(Key<? super FileSubject> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        String token = reader.parseString();
        if (service.getGroupSubjects().hasSubject(token).join())
        {
            return Optional.of((FileSubject) service.getGroupSubjects().loadSubject(token).join());
        }
        throw reader.createException(i18n.translate(context.getCause(), "Could not find the role: {input#role}", token));
    }
}
