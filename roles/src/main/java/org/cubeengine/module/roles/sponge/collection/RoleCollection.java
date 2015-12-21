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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.config.RoleConfig;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.subject.RoleSubject;
import org.cubeengine.module.roles.sponge.subject.UserSubject;
import de.cubeisland.engine.reflect.Reflector;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.world.storage.WorldProperties;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;
import static org.cubeengine.module.roles.sponge.subject.RoleSubject.SEPARATOR;
import static org.cubeengine.service.filesystem.FileExtensionFilter.YAML;
import static org.spongepowered.api.service.permission.PermissionService.SUBJECTS_GROUP;

public class RoleCollection extends BaseSubjectCollection<RoleSubject>
{
    private final Path modulePath;
    private final Server server;
    private Map<Context, Context> mirrors;
    private Roles module;
    private RolesPermissionService service;
    private Reflector reflector;

    public RoleCollection(Roles module, RolesPermissionService service, Reflector reflector, Game game)
    {
        super(SUBJECTS_GROUP);
        this.module = module;
        this.service = service;
        this.reflector = reflector;
        this.server = game.getServer();
        this.modulePath = module.getProvided(Path.class);
    }

    private void loadRoles()
    {
        this.subjects.clear();
        try
        {
            createDirectories(modulePath.resolve("global"));
            createDirectories(modulePath.resolve(Context.WORLD_KEY));
            for (Path ctxType : newDirectoryStream(modulePath, p -> isDirectory(p)))
            {
                String ctxTypeName = ctxType.getFileName().toString();
                for (Path ctxName : newDirectoryStream(ctxType, p -> Files.isDirectory(p) || (Files.isRegularFile(p) && p.toString().endsWith(YAML.getExtention()))))
                {
                    if (isDirectory(ctxName))
                    {
                        for (Path file : newDirectoryStream(ctxName, YAML))
                        {
                            newSubject(ctxTypeName, ctxName.getFileName().toString(), file.toFile());
                        }
                    }
                    else // YAML
                    {
                        newSubject(ctxTypeName, "", ctxName.toFile());
                    }
                }

                if (Context.WORLD_KEY.equals(ctxTypeName))
                {
                    for (WorldProperties prop : server.getAllWorldProperties())
                    {
                        String world = prop.getWorldName();
                        if (!mirrors.containsKey(readMirror(world)) && !Files.exists(ctxType.resolve(world)))
                        {
                            try
                            {
                                createDirectories(ctxType.resolve(world));
                            }
                            catch (IOException e)
                            {
                                throw new IllegalStateException(e);
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private void loadMirrors()
    {
        mirrors = readMirrors(service.getConfig().mirrors.roles);
    }

    private void newSubject(String ctxType, String ctxName, File file)
    {
        RoleConfig config = reflector.create(RoleConfig.class);
        config.setFile(file);
        config.reload();
        addSubject(module, service, config, ctxType, ctxName);
    }

    private RoleSubject addSubject(Roles module, RolesPermissionService service, RoleConfig config, String ctxType, String ctxName)
    {
        RoleSubject subject = new RoleSubject(module, service, this, config, new Context(ctxType, ctxName));
        subjects.put(subject.getIdentifier(), subject);
        mirrors.entrySet().stream()
                .filter(entry -> entry.getValue().equals(subject.getContext()))
                .forEach(entry -> subjects.put(subject.getIdentifier(entry.getKey()), subject));
        return subject;
    }

    @Override
    protected RoleSubject createSubject(String identifier)
    {
        if (!identifier.startsWith("role:"))
        {
            // must be <ctxType>|<ctxName>|<roleName>
            throw new IllegalArgumentException("Provided identifier is not a role: " + identifier);
        }
        String name = identifier.substring(5);
        String[] split = name.split("\\|");
        String ctxType = split[0];
        String ctxName = "";
        if (split.length == 3)
        {
            ctxName = split[1];
            name = split[2];
        }
        else if (split.length == 2)
        {
            name = split[1];
        }
        else
        {
            throw new IllegalArgumentException("Provided identifier has an invalid context: " + identifier);
        }
        Path path = module.getProvided(Path.class).resolve(ctxType);
        if (!ctxName.isEmpty())
        {
            path = path.resolve(ctxName);
        }
        RoleConfig config = reflector.create(RoleConfig.class);
        config.roleName = name;
        config.setFile(path.resolve(name + ".yml").toFile());
        config.reload();
        return addSubject(module, service, config, ctxType, ctxName);
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
        loadMirrors();
        loadRoles();
    }

    public Context getMirror(Context context)
    {
        return mirrors.getOrDefault(context, context);
    }
}
