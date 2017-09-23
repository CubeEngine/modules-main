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

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newDirectoryStream;
import static org.cubeengine.libcube.service.filesystem.FileExtensionFilter.YAML;

import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.roles.config.RoleConfig;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class FileBasedCollection extends BaseSubjectCollection
{
    private final Path modulePath;
    private final boolean preload;
    private RolesPermissionService service;
    private Reflector reflector;
    private Map<UUID, FileSubject> subjectByUUID = new ConcurrentHashMap<>();

    private Queue<Subject> unloadQueue = new ConcurrentLinkedDeque<>(); // TODO do unload

    public FileBasedCollection(Path modulePath, RolesPermissionService service, Reflector reflector, String identifier)
    {
        this(modulePath, service, reflector, identifier, false);
    }

    public FileBasedCollection(Path modulePath, RolesPermissionService service, Reflector reflector, String identifier, boolean preload)
    {
        super(service, identifier);
        this.service = service;
        this.reflector = reflector;
        this.modulePath = modulePath;
        this.preload = preload;
    }

    @Override
    protected boolean isValid(String identifier)
    {
        return identifier.matches("[a-z][a-z0-9]+");
    }

    private void loadAll()
    {
        try
        {
            createDirectories(modulePath.resolve(this.getIdentifier()));
            for (Path configFile : newDirectoryStream(modulePath.resolve(this.getIdentifier()), YAML))
            {
                RoleConfig config = reflector.create(RoleConfig.class);
                config.setFile(configFile.toFile());
                config.reload(true); // and re-save
                addSubject(service, config);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private void clearAll()
    {
        this.subjects.clear();
        this.subjectByUUID.clear();
    }

    private FileSubject addSubject(RolesPermissionService service, RoleConfig config)
    {
        FileSubject subject = new FileSubject(service, this, config);
        subjects.put(subject.getIdentifier(), subject);
        subjectByUUID.put(subject.getSubjectData().getConfig().identifier, subject);
        return subject;
    }

    @Override
    protected Subject loadSubject0(String identifier)
    {
        Optional<Subject> result = getSubject(identifier);
        if (result.isPresent())
        {
            return result.get();
        }

        Iterator<Subject> it = this.unloadQueue.iterator();
        while (it.hasNext())
        {
            Subject next = it.next();
            if (next.getIdentifier().equals(identifier))
            {
                it.remove();
                this.subjects.put(identifier, next);
                this.subjectByUUID.put(((FileSubject) next).getSubjectData().getConfig().identifier, ((FileSubject) next));
                return next;
            }
        }

        Path file = getFile(identifier);
        if (Files.isRegularFile(file))
        {
            RoleConfig loaded = reflector.load(RoleConfig.class, file.toFile());
            return addSubject(service, loaded);
        }
        else
        {
            RoleConfig config = reflector.create(RoleConfig.class);
            config.identifier = UUID.randomUUID();
            config.roleName = identifier;
            config.setFile(file.toFile());
            return addSubject(service, config);
        }
    }

    @Override
    public CompletableFuture<Set<String>> getAllIdentifiers()
    {
        return CompletableFuture.supplyAsync(this::getAllIds);
    }

    private Set<String> getAllIds()
    {
        Set<String> set = new HashSet<>();
        try
        {
            for (Path configFile : newDirectoryStream(modulePath.resolve(this.getIdentifier()), YAML))
            {
                set.add(StringUtils.stripFileExtension(configFile.getFileName().toString()));
            }
        }
        catch (IOException ignored)
        {
        }
        return set;
    }

    private Path getFile(String identifier)
    {
        return this.modulePath.resolve(this.getIdentifier()).resolve(identifier + YAML.getExtention());
    }

    @Override
    public CompletableFuture<Boolean> hasSubject(String identifier)
    {
        return CompletableFuture.supplyAsync(() -> hasSubject0(identifier));
    }

    private boolean hasSubject0(String identifier)
    {
        return this.subjects.containsKey(identifier) || Files.isRegularFile(getFile(identifier));
    }

    public boolean rename(FileSubject role, String newName)
    {
        if (hasSubject0(newName))
        {
            return false;
        }

        if (!role.getSubjectData().getConfig().getFile().delete())
        {
            throw new IllegalStateException();
        }
        setRoleName(role, newName);

        subjects.values().stream()
                .filter(subject -> subject instanceof FileSubject)
                .map(FileSubject.class::cast)
                .filter(subject -> subject.getParents().contains(newSubjectReference(role.getIdentifier())))
                .forEach(subject -> subject.getSubjectData()
                .save(CompletableFuture.completedFuture(true)));

        return true;
    }

    public boolean delete(FileSubject r, boolean force)
    {
        // TODO maybe async this whole thing

        SubjectReference ref = newSubjectReference(r.getIdentifier());
        // remove role from
        for (Subject subject : subjects.values())
        {
            if (subject.isChildOf(ref))
            {
                if (!force)
                {
                    return false;
                }
            }
        }

        for (Subject userSubject : service.getUserSubjects().subjects.values())
        {
            if (userSubject.isChildOf(r.getActiveContexts(), ref))
            {
                if (!force)
                {
                    return false; // prevent deletion when still in use
                }
            }
        }

        subjects.remove(r.getIdentifier());
        subjectByUUID.remove(r.getSubjectData().getConfig().identifier);

        r.getSubjectData().delete(); // delete file

        for (SubjectCollection collection : service.getLoadedCollections().values())
        {
            if (collection instanceof FileBasedCollection)
            {
                ((FileBasedCollection) collection).reload();
            }
        }
        return true;
    }

    public FileBasedCollection reload()
    {
        clearAll();
        if (preload)
        {
            loadAll();
        }
        return this;
    }

    public void setRoleName(FileSubject subject, String name)
    {
        RoleConfig config = subject.getSubjectData().getConfig();
        subjects.remove(subject.getIdentifier());
        subjects.put(name, subject);
        config.roleName = name;
        config.setFile(getFile(name).toFile());
        subject.getSubjectData().save(CompletableFuture.completedFuture(true));
    }

    public Optional<FileSubject> getByUUID(UUID uuid)
    {
        return Optional.ofNullable(subjectByUUID.get(uuid));
    }

    public FileSubject getByInternalIdentifier(String internalId, String owner)
    {
        try
        {
            UUID uuid = UUID.fromString(internalId);
            Optional<FileSubject> byUUID = getByUUID(uuid);
            if (byUUID.isPresent())
            {
                return byUUID.get();
            }
            System.out.print("Could not find Role for UUID: " + uuid + " and therefor removed it from " + owner + "\n");
            // TODO message in logger
        }
        catch (IllegalArgumentException ignored)
        {
            Optional<Subject> subject = this.service.getGroupSubjects().getSubject(internalId);
            if (subject.isPresent())
            {
                return ((FileSubject) subject.get());
            }
            System.out.print("Could not find Role for Identifier: " + internalId + " and therefor removed it from " + owner + "\n");
            // TODO message in logger
        }
        return null;
    }

    @Override
    public void suggestUnload(String identifier)
    {
        Subject unload = this.subjects.remove(identifier);
        this.unloadQueue.add(unload);
    }
}
