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
package org.cubeengine.module.roles.service;

import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ServiceProvider;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesConfig;
import org.cubeengine.module.roles.service.collection.BasicSubjectCollection;
import org.cubeengine.module.roles.service.collection.RoleCollection;
import org.cubeengine.module.roles.service.collection.UserCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionDescription.Builder;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.util.Tristate;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

@ServiceProvider(PermissionService.class)
public class RolesPermissionService implements PermissionService
{
    public static final String DEFAULT_SUBJECTS = "default";
    private final ConcurrentMap<String, SubjectCollection> collections = new ConcurrentHashMap<>();
    private final List<ContextCalculator<Subject>> calculators = new CopyOnWriteArrayList<>();

    private RolesConfig config;
    private Log logger;

    private final Map<String, PermissionDescription> descriptionMap = new LinkedHashMap<String, PermissionDescription>();
    private Collection<PermissionDescription> descriptions;
    @Inject private PermissionManager pm;

    @Inject
    public RolesPermissionService(Roles module, FileManager fm, Reflector reflector)
    {
        this.logger = module.getProvided(Log.class);
        this.config = fm.loadConfig(module, RolesConfig.class);
        collections.put(DEFAULT_SUBJECTS, new BasicSubjectCollection(this, DEFAULT_SUBJECTS));
        collections.put(SUBJECTS_USER, new UserCollection(this));
        collections.put(SUBJECTS_GROUP, new RoleCollection(module.getProvided(Path.class), this, reflector, SUBJECTS_GROUP));

        getGroupSubjects().reload();
        collections.put(SUBJECTS_SYSTEM, new BasicSubjectCollection(this, SUBJECTS_SYSTEM));
        collections.put(SUBJECTS_ROLE_TEMPLATE, new BasicSubjectCollection(this, SUBJECTS_ROLE_TEMPLATE));
        // TODO persist other types than user/role
    }

    @Override
    public UserCollection getUserSubjects()
    {
        return (UserCollection)collections.get(SUBJECTS_USER);
    }

    @Override
    public RoleCollection getGroupSubjects()
    {
        return (RoleCollection)collections.get(SUBJECTS_GROUP);
    }

    @Override
    public Subject getDefaults()
    {
        // TODO make sure defaultdata is properly resolved
        SubjectCollection collection = getSubjects(DEFAULT_SUBJECTS);
        return collection.get(DEFAULT_SUBJECTS);
    }

    @Override
    public SubjectCollection getSubjects(String identifier)
    {
        SubjectCollection collection = collections.get(identifier);
        if (collection == null)
        {
            collection = new BasicSubjectCollection(this, identifier);
            collections.put(identifier, collection);
        }
        return collection;
    }

    @Override
    public Map<String, SubjectCollection> getKnownSubjects()
    {
        return Collections.unmodifiableMap(collections);
    }

    @Override
    public void registerContextCalculator(ContextCalculator<Subject> calculator)
    {
        calculators.add(calculator);
    }

    public List<ContextCalculator<Subject>> getContextCalculators()
    {
        return this.calculators;
    }

    public RolesConfig getConfig()
    {
        return config;
    }

    @Override
    public Optional<Builder> newDescriptionBuilder(Object plugin)
    {
        // TODO somehow allow modules
        Optional<PluginContainer> container = Sponge.getPluginManager().fromInstance(plugin);
        return Optional.of(new RolesPermissionDescriptionBuilder(container.get(), this));
    }

    @Override
    public Optional<PermissionDescription> getDescription(String permission)
    {
        return Optional.ofNullable(descriptionMap.get(permission.toLowerCase()));
    }

    @Override
    public Collection<PermissionDescription> getDescriptions()
    {
        if (descriptions == null)
        {
            descriptions = Collections.unmodifiableCollection(descriptionMap.values());
        }
        return descriptions;
    }

    protected PermissionDescription addDescription(RolesPermissionDescription desc, Map<String, Tristate> roleAssignments)
    {
        SubjectCollection subjects = getSubjects(SUBJECTS_ROLE_TEMPLATE); // TODO prevent infinite recursion
        roleAssignments.entrySet().forEach(e -> subjects.get(e.getKey()).getTransientSubjectData().setPermission(GLOBAL_CONTEXT, desc.getId(), e.getValue()));

        if (descriptionMap.put(desc.getId().toLowerCase(), desc) == null)
        {
            if (config.debug)
            {
                logger.debug(desc.getId().toLowerCase());
            }
        }
        descriptions = null;
        return desc;
    }

    public PermissionManager getPermissionManager()
    {
        return pm;
    }

    public Log getLog() {
        return logger;
    }
}
