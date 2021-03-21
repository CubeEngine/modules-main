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
package org.cubeengine.module.roles.service.subject;

import java.util.Collections;
import java.util.Set;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.data.BaseSubjectData;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;

public class BasicSubject extends BaseSubject<BaseSubjectData>
{
    private final BaseSubjectData data;
    private String identifier;

    public BasicSubject(String identifier, SubjectCollection collection, RolesPermissionService service)
    {
        super(collection, service);
        this.identifier = identifier;
        this.data = new BaseSubjectData(service, this,false);
    }

    @Override
    public BaseSubjectData subjectData()
    {
        return this.data;
    }

    @Override
    public String identifier()
    {
        return identifier;
    }

//    @Override
//    public Optional<CommandSource> getCommandSource()
//    {
//        if (identifier.equals("Server"))
//        {
//            return Optional.of(Sponge.getServer().getConsole());
//        }
//        if (identifier.equals("RCON"))
//        {
//
//        }
//        return Optional.empty();
//    }

    @Override
    public Set<Context> activeContexts()
    {
        if (containingCollection().identifier().equals(PermissionService.SUBJECTS_ROLE_TEMPLATE))
        {
            return Collections.emptySet();
        }
        return super.activeContexts();
    }

    @Override
    public boolean isSubjectDataPersisted()
    {
        return false;
    }
}
