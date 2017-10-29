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
import java.util.concurrent.ExecutionException;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.argument.Completer;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.service.permission.Subject;

public class RoleParser implements ArgumentParser<FileSubject>, Completer
{
    private RolesPermissionService service;

    public RoleParser(RolesPermissionService service)
    {
        this.service = service;
    }

    @Override
    public FileSubject parse(Class type, CommandInvocation invocation) throws ParserException
    {
        String token = invocation.consume(1);
        if (service.getGroupSubjects().hasSubject(token.toLowerCase()).join())
        {
            return ((FileSubject) service.getGroupSubjects().loadSubject(token.toLowerCase()).join());
        }
        throw new ParserException("Could not find the role: {input#role}", token);
    }

    @Override
    public List<String> suggest(Class type, CommandInvocation invocation)
    {
        ArrayList<String> result = new ArrayList<>();
        String token = invocation.currentToken().toLowerCase();
        for (Subject subject : service.getGroupSubjects().getLoadedSubjects())
        {
            String name = subject.getIdentifier();
            if (name.startsWith(token))
            {
                result.add(name);
            }
        }
        return result;
    }
}
