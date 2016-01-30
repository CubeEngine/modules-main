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
package org.cubeengine.module.roles.commands.provider;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.subject.RoleSubject;
import org.spongepowered.api.service.permission.Subject;

import java.util.ArrayList;
import java.util.List;

public class RoleReader implements ArgumentReader<RoleSubject>, Completer
{
    private RolesPermissionService service;

    public RoleReader(RolesPermissionService service)
    {
        this.service = service;
    }

    @Override
    public RoleSubject read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String token = invocation.consume(1);
        if (service.getGroupSubjects().hasRegistered("role:" + token))
        {
            return service.getGroupSubjects().get("role:" + token);
        }
        throw new ReaderException("Could not find the role: {input#role}", token);
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        ArrayList<String> result = new ArrayList<>();
        String token = invocation.currentToken().toLowerCase();
        for (Subject subject : service.getGroupSubjects().getAllSubjects())
        {
            if (subject.getIdentifier().startsWith("role:" + token))
            {
                result.add(subject.getIdentifier().substring(5));
            }
        }
        return result;
    }
}
