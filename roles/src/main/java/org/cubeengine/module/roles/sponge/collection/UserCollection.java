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
package org.cubeengine.module.roles.sponge.collection;

import java.util.*;
import java.util.stream.Collectors;

import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.subject.UserSubject;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;

import static java.util.stream.Collectors.toList;

public class UserCollection extends BaseSubjectCollection<UserSubject>
{
    private Map<Context, Context> assignedMirrors;
    private Map<Context, Context> directMirrors;

    private RolesPermissionService service;
    private Game game;

    public UserCollection(RolesPermissionService service, Game game)
    {
        super(PermissionService.SUBJECTS_USER);
        this.service = service;
        this.game = game;
        loadMirrors();
        // TODO add missing selfreferencing mirrors
        // TODO use mirrors when resolving context
    }

    private void loadMirrors()
    {
        assignedMirrors = readMirrors(service.getConfig().mirrors.assigned);
        directMirrors = readMirrors(service.getConfig().mirrors.direct);
    }

    @Override
    protected UserSubject createSubject(String identifier)
    {
        try
        {
            return new UserSubject(game, service, UUID.fromString(identifier));
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Provided identifier must be a uuid, was " + identifier);
        }
    }

    @Override
    public boolean hasRegistered(String identifier)
    {
        // TODO DB-Lookup ?
        return super.hasRegistered(identifier) || false;
    }

    @Override
    public Iterable<Subject> getAllSubjects()
    {
        Iterable<Subject> allSubjects = super.getAllSubjects();
        // TODO lazy DB-Lookup
        return null;
    }

    public void reload()
    {
        loadMirrors();
        this.subjects.clear();
    }

    public Context getAssignMirror(Context context)
    {
        return assignedMirrors.getOrDefault(context, context);
    }

    public Context getDirectMirror(Context context)
    {
        return directMirrors.getOrDefault(context, context);
    }
}
