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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesConfig;
import org.cubeengine.module.roles.service.collection.BasicSubjectCollection;
import org.cubeengine.module.roles.service.collection.RoleCollection;
import org.cubeengine.module.roles.service.collection.UserCollection;
import org.cubeengine.module.roles.service.data.DefaultSubjectData;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionDescription.Builder;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.util.Tristate;

import static org.cubeengine.module.roles.service.subject.RoleSubject.SEPARATOR;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

public class RolesPermissionService implements PermissionService
{
    private final ConcurrentMap<String, SubjectCollection> collections = new ConcurrentHashMap<>();
    private final List<ContextCalculator<Subject>> calculators = new CopyOnWriteArrayList<>();

    private final DefaultSubjectData defaultData;
    private final Map<Context, Context> mirrors;
    private Game game;
    private RolesConfig config;
    private Log logger;

    private final Map<String, PermissionDescription> descriptionMap = new LinkedHashMap<String, PermissionDescription>();
    private Collection<PermissionDescription> descriptions;

    public RolesPermissionService(Roles module, Reflector reflector, RolesConfig config, Game game, PermissionManager manager, Log permLogger)
    {
        this.game = game;
        this.config = config;
        logger = permLogger;
        defaultData = new DefaultSubjectData(this, config);
        collections.put(SUBJECTS_USER, new UserCollection(this, game));
        collections.put(SUBJECTS_GROUP, new RoleCollection(module, this, reflector));
        getGroupSubjects().reload();
        collections.put(SUBJECTS_SYSTEM, new BasicSubjectCollection(this, SUBJECTS_SYSTEM, game));
        collections.put(SUBJECTS_ROLE_TEMPLATE, new BasicSubjectCollection(this, SUBJECTS_ROLE_TEMPLATE, game));

        mirrors = readMirrors(config.mirrors); // TODO reload on relaod
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
    public DefaultSubjectData getDefaultData()
    {
        return defaultData;
    }

    @Override
    public SubjectCollection getSubjects(String identifier)
    {
        return collections.get(identifier);
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
        Optional<PluginContainer> container = game.getPluginManager().fromInstance(plugin);
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
        SubjectCollection subjects = getSubjects(SUBJECTS_ROLE_TEMPLATE);
        roleAssignments.entrySet().forEach(e -> subjects.get(e.getKey()).getTransientSubjectData().setPermission(GLOBAL_CONTEXT, desc.getId(), e.getValue()));

        descriptionMap.put(desc.getId().toLowerCase(), desc);
        descriptions = null;
        logger.info(desc.getId().toLowerCase());
        return desc;
    }

    protected final Context readMirror(String source)
    {
        if (!source.contains(SEPARATOR))
        {
            if (!"global".equals(source))
            {
                return new Context(Context.WORLD_KEY, source);
            }
        }
        String[] split = source.split("\\|");
        return new Context(split[0], split[1]);
    }

    protected final Map<Context, Context> readMirrors(Map<String, List<String>> config)
    {
        Map<Context, Context> mirrors = new HashMap<>();
        for (Map.Entry<String, List<String>> roleMirror : config.entrySet())
        {
            Context source = readMirror(roleMirror.getKey());
            for (String mirrored : roleMirror.getValue())
            {
                mirrors.put(readMirror(mirrored), source);
            }
            mirrors.put(source, source); // self-referencing mirror
        }
        return mirrors;
    }

    public Context getMirror(Context context)
    {
        return mirrors.getOrDefault(context, context);
    }

}
