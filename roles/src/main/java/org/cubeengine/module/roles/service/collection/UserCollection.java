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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.cubeengine.module.roles.service.subject.UserSubject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;

public class UserCollection extends BaseSubjectCollection
{
    private RolesPermissionService service;

    public UserCollection(RolesPermissionService service)
    {
        super(service, PermissionService.SUBJECTS_USER);
        this.service = service;
    }

    @Override
    protected UserSubject loadSubject0(String identifier)
    {
        Subject result = this.subjects.get(identifier);
        if (result != null)
        {
            return ((UserSubject) result);
        }
        try
        {
            UserSubject userSubject = new UserSubject(service, UUID.fromString(identifier));
            this.subjects.put(identifier, userSubject);
            return userSubject;
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Provided identifier must be a uuid, was " + identifier);
        }
    }

    public void reload()
    {
        for (Subject subject : subjects.values())
        {
            if (subject instanceof FileSubject)
            {

            }
        }
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate()
    {
        return identifier -> {
            try
            {
                UUID.fromString(identifier);
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        };
    }

    @Override
    public CompletableFuture<Boolean> hasSubject(String identifier)
    {
        return CompletableFuture.supplyAsync(() -> {
            UserStorageService service = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
            return service.get(UUID.fromString(identifier)).isPresent();
        });
    }

    @Override
    public CompletableFuture<Set<String>> getAllIdentifiers()
    {
        return CompletableFuture.supplyAsync(() -> {
            UserStorageService service = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
            return service.getAll().stream().map(gp -> gp.getUniqueId().toString()).collect(Collectors.toSet());
        });
    }

    @Override
    public void suggestUnload(String identifier)
    {
    }
}
