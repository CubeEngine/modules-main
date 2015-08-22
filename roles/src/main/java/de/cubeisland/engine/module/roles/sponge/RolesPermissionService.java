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
package de.cubeisland.engine.module.roles.sponge;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.google.common.base.Optional;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.RolesConfig;
import de.cubeisland.engine.module.roles.sponge.collection.BasicSubjectCollection;
import de.cubeisland.engine.module.roles.sponge.collection.RoleCollection;
import de.cubeisland.engine.module.roles.sponge.collection.UserCollection;
import de.cubeisland.engine.module.roles.sponge.data.DefaultSubjectData;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.world.WorldManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionDescription.Builder;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.ContextCalculator;
import org.spongepowered.api.util.Tristate;

import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

public class RolesPermissionService implements PermissionService
{
    private final ConcurrentMap<String, SubjectCollection> collections = new ConcurrentHashMap<>();
    private final List<ContextCalculator> calculators = new CopyOnWriteArrayList<>();

    private final DefaultSubjectData defaultData;
    private Game game;
    private Database db;
    private RolesConfig config;

    private final Map<String, PermissionDescription> descriptionMap = new LinkedHashMap<String, PermissionDescription>();
    private Collection<PermissionDescription> descriptions;

    public RolesPermissionService(Roles module, Reflector reflector, RolesConfig config, Game game, Database db, WorldManager wm, PermissionManager manager)
    {
        this.game = game;
        this.db = db;
        this.config = config;
        defaultData = new DefaultSubjectData(this, config);
        collections.put(SUBJECTS_USER, new UserCollection(this, game));
        collections.put(SUBJECTS_GROUP, new RoleCollection(module, this, manager, reflector, wm));
        collections.put(SUBJECTS_SYSTEM, new BasicSubjectCollection(this, SUBJECTS_SYSTEM, game));
        collections.put(SUBJECTS_ROLE_TEMPLATE, new BasicSubjectCollection(this, SUBJECTS_ROLE_TEMPLATE, game));
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
    public void registerContextCalculator(ContextCalculator calculator)
    {
        calculators.add(calculator);
    }

    public List<ContextCalculator> getContextCalculators()
    {
        return this.calculators;
    }

    public Database getDB()
    {
        return db;
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
        return Optional.fromNullable(descriptionMap.get(permission));
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
        roleAssignments.entrySet().forEach(e -> subjects.get(e.getKey()).getTransientSubjectData().setPermission(
            GLOBAL_CONTEXT, desc.getId(), e.getValue()));

        descriptionMap.put(desc.getId().toLowerCase(), desc);
        descriptions = null;
        System.out.print(" #PERM#  " + desc.getId().toLowerCase() + "\n");
        return desc;
    }
}
