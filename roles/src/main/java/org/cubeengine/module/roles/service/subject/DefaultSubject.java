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
package org.cubeengine.module.roles.service.subject;

import org.cubeengine.module.roles.service.RolesPermissionService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;

import java.util.Optional;

public class DefaultSubject extends BaseSubject<SubjectData>
{
    private String id;

    public DefaultSubject(String id, SubjectCollection collection, RolesPermissionService service, SubjectData data)
    {
        super(collection, service, data);
        this.id = id;
    }

    @Override
    public String getIdentifier()
    {
        return id;
    }

    @Override
    public Optional<CommandSource> getCommandSource()
    {
        return Optional.empty();
    }
}
