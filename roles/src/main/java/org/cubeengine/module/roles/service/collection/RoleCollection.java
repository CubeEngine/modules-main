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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.module.roles.config.RoleConfig;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.cubeengine.module.roles.service.subject.UserSubject;
import org.spongepowered.api.service.permission.Subject;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newDirectoryStream;
import static org.cubeengine.libcube.service.filesystem.FileExtensionFilter.YAML;

public class RoleCollection extends BaseSubjectCollection<RoleSubject>
{
    private final Path modulePath;
    private RolesPermissionService service;
    private Reflector reflector;
    private Map<UUID, RoleSubject> subjectByUUID = new ConcurrentHashMap<>();

    public RoleCollection(Path modulePath, RolesPermissionService service, Reflector reflector, String identifier)
    {
        super(service, identifier);
        this.service = service;
        this.reflector = reflector;
        this.modulePath = modulePath;
    }

    private void loadRoles()
    {
        this.subjects.clear();
        this.subjectByUUID.clear();
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

    private RoleSubject addSubject(RolesPermissionService service, RoleConfig config)
    {
        RoleSubject subject = new RoleSubject(service, this, config);
        subjects.put(subject.getIdentifier(), subject);
        subjectByUUID.put(subject.getSubjectData().getConfig().identifier, subject);
        return subject;
    }

    @Override
    protected RoleSubject createSubject(String identifier)
    {
        for (RoleSubject role : subjects.values())
        {
            if (role.getIdentifier().equalsIgnoreCase(identifier))
            {
                return role;
            }
        }
        try
        {
            UUID uuid = UUID.fromString(identifier);
            RoleConfig config = reflector.create(RoleConfig.class);
            config.identifier = uuid;
            config.roleName = identifier;
            return addSubject(service, config);
        }
        catch (IllegalArgumentException e)
        {
            RoleConfig config = reflector.create(RoleConfig.class);
            config.identifier = UUID.randomUUID();
            config.roleName = identifier;
            config.setFile(modulePath.resolve(this.getIdentifier()).resolve(identifier + YAML.getExtention()).toFile());
            return addSubject(service, config);
        }
    }

    @Override
    public boolean hasRegistered(String identifier)
    {
        return super.hasRegistered(identifier) || subjects.values().stream().map(RoleSubject::getIdentifier).anyMatch(name -> name.equalsIgnoreCase(identifier));
    }

    @Override
    public Iterable<Subject> getAllSubjects()
    {
        return new ArrayList<>(subjects.values());
    }

    public boolean rename(RoleSubject role, String newName)
    {
        if (hasRegistered(newName))
        {
            return false;
        }

        if (!role.getSubjectData().getConfig().getFile().delete())
        {
            throw new IllegalStateException();
        }
        setRoleName(role, newName);
        subjects.values().stream().filter(subject -> subject.getParents().contains(subject)).forEach(subject -> subject.getSubjectData().save(true));
        return true;
    }

    public boolean delete(RoleSubject r, boolean force)
    {
        // TODO maybe async this whole thing

        // remove role from files
        for (RoleSubject roleSubject : subjects.values())
        {
            if (roleSubject.isChildOf(r))
            {
                if (!force)
                {
                    return false; // prevent deletion when still in use
                }
                roleSubject.getSubjectData().removeParent(r.getActiveContexts(), r);
            }
        }

        for (UserSubject userSubject : service.getUserSubjects().subjects.values())
        {
            if (userSubject.isChildOf(r.getActiveContexts(), r))
            {
                if (!force)
                {
                    return false; // prevent deletion when still in use
                }
                userSubject.getSubjectData().removeParent(r.getActiveContexts(), r);
            }
        }

        subjects.remove(r.getIdentifier());
        subjectByUUID.remove(r.getSubjectData().getConfig().identifier);

        r.getSubjectData().delete(); // delete file

        // TODO maybe force reload ; is this needed ?
        return true;
    }

    public RoleCollection reload()
    {
        loadRoles();
        return this;
    }

    public void setRoleName(RoleSubject subject, String name)
    {
        RoleConfig config = subject.getSubjectData().getConfig();
        subjects.remove(subject.getIdentifier());
        subjects.put(name, subject);
        config.roleName = name;
        config.setFile(modulePath.resolve(this.getIdentifier()).resolve(name + YAML.getExtention()).toFile());
        subject.getSubjectData().save(true);
    }

    public Optional<RoleSubject> getByUUID(UUID uuid)
    {
        return Optional.ofNullable(subjectByUUID.get(uuid));
    }

    public RoleSubject getByInternalIdentifier(String internalId, String owner)
    {
        try
        {
            UUID uuid = UUID.fromString(internalId);
            Optional<RoleSubject> byUUID = getByUUID(uuid);
            if (byUUID.isPresent())
            {
                return byUUID.get();
            }
            System.out.print("Could not find Role for UUID: " + uuid + " and therefor removed it from " + owner + "\n");
            // TODO message in logger
        }
        catch (IllegalArgumentException ignored)
        {
            if (hasRegistered(internalId))
            {
                return get(internalId);
            }
            System.out.print("Could not find Role for Identifier: " + internalId + " and therefor removed it from " + owner + "\n");
            // TODO message in logger
        }
        return null;
    }
}
