package org.cubeengine.module.roles.service.subject;

import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;

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


}
