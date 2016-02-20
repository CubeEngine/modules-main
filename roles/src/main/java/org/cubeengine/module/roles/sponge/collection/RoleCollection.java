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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.config.RoleConfig;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.subject.RoleSubject;
import org.cubeengine.module.roles.sponge.subject.UserSubject;
import org.spongepowered.api.service.permission.Subject;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newDirectoryStream;
import static org.cubeengine.service.filesystem.FileExtensionFilter.YAML;
import static org.spongepowered.api.service.permission.PermissionService.SUBJECTS_GROUP;

public class RoleCollection extends BaseSubjectCollection<RoleSubject>
{
    private final Path modulePath;
    private Roles module;
    private RolesPermissionService service;
    private Reflector reflector;

    public RoleCollection(Roles module, RolesPermissionService service, Reflector reflector)
    {
        super(SUBJECTS_GROUP);
        this.module = module;
        this.service = service;
        this.reflector = reflector;
        this.modulePath = module.getProvided(Path.class);
    }

    private void loadRoles()
    {
        this.subjects.clear();
        try
        {
            createDirectories(modulePath.resolve("roles"));
            for (Path configFile : newDirectoryStream(modulePath.resolve("roles"), YAML))
            {
                RoleConfig config = reflector.create(RoleConfig.class);
                config.setFile(configFile.toFile());
                config.reload(true); // and re-save
                addSubject(module, service, config);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private RoleSubject addSubject(Roles module, RolesPermissionService service, RoleConfig config)
    {
        RoleSubject subject = new RoleSubject(module, service, this, config);
        subjects.put(subject.getIdentifier(), subject);
        return subject;
    }

    @Override
    protected RoleSubject createSubject(String identifier)
    {
        if (!identifier.startsWith("role:"))
        {
            throw new IllegalArgumentException("Provided identifier is not a role: " + identifier);
        }
        String name = identifier.substring(5);

        RoleConfig config = reflector.create(RoleConfig.class);
        config.roleName = name;
        config.setFile(modulePath.resolve("roles").resolve(name + YAML.getExtention()).toFile());
        config.reload();
        return addSubject(module, service, config);
    }

    @Override
    public boolean hasRegistered(String identifier)
    {
        return subjects.containsKey(identifier);
    }

    @Override
    public Iterable<Subject> getAllSubjects()
    {
        return new ArrayList<>(subjects.values());
    }

    public boolean rename(RoleSubject role, String newName)
    {
        // TODO rename
        return false;
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

        // TODO remove defaultrole status

        subjects.values().remove(r);
        r.getSubjectData().delete(); // delete file

        // TODO reload/calculate perms ; is this needed ?
        return true;
    }

    public void reload()
    {
        loadRoles();
    }
}
