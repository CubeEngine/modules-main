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
package org.cubeengine.module.roles.service.collection;

import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.BasicSubject;
import org.spongepowered.api.service.permission.Subject;

public class BasicSubjectCollection extends BaseSubjectCollection<Subject>
{
    public BasicSubjectCollection(RolesPermissionService service, String identifier)
    {
        super(service, identifier);
    }

    @Override
    protected Subject createSubject(String identifier)
    {
        return new BasicSubject(identifier, this, service);
    }

    @Override
    public Subject getDefaults() {
        return service.getDefaults();
    }
}
