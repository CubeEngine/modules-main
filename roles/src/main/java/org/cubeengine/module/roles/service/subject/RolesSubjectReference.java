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

import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RolesSubjectReference implements SubjectReference
{

    private String identifier;
    private SubjectCollection collection;

    public RolesSubjectReference(String identifier, SubjectCollection collection)
    {

        this.identifier = identifier;
        this.collection = collection;
    }

    @Override
    public String getCollectionIdentifier()
    {
        return this.collection.getIdentifier();
    }

    @Override
    public String getSubjectIdentifier()
    {
        return this.identifier;
    }

    @Override
    public CompletableFuture<Subject> resolve()
    {
        return this.collection.loadSubject(identifier);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof RolesSubjectReference))
        {
            return false;
        }
        RolesSubjectReference that = (RolesSubjectReference) o;
        return Objects.equals(identifier, that.identifier) &&
                Objects.equals(collection.getIdentifier(), that.collection.getIdentifier());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(identifier, collection.getIdentifier());
    }
}
