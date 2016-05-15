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
package org.cubeengine.module.roles.service.collection;

import java.util.UUID;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.UserSubject;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

public class UserCollection extends BaseSubjectCollection<UserSubject>
{
    private RolesPermissionService service;

    public UserCollection(RolesPermissionService service)
    {
        super(PermissionService.SUBJECTS_USER);
        this.service = service;
    }

    @Override
    protected UserSubject createSubject(String identifier)
    {
        try
        {
            return new UserSubject(service, UUID.fromString(identifier));
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Provided identifier must be a uuid, was " + identifier);
        }
    }

    @Override
    public Iterable<Subject> getAllSubjects()
    {
        // TODO get from all offline users once they can have custom data
        return super.getAllSubjects();
    }

    public void reload()
    {
        for (UserSubject subject : subjects.values())
        {
            subject.reload();
        }
    }
}
