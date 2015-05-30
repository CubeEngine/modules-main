package de.cubeisland.engine.module.roles.sponge;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.RolesConfig;
import de.cubeisland.engine.module.roles.config.RoleConfig;
import de.cubeisland.engine.module.roles.sponge.collection.RoleCollection;
import de.cubeisland.engine.module.roles.sponge.collection.UserCollection;
import de.cubeisland.engine.module.roles.sponge.data.DefaultSubjectData;
import de.cubeisland.engine.module.roles.sponge.subject.RoleSubject;
import de.cubeisland.engine.module.service.database.Database;
import de.cubeisland.engine.reflect.Reflector;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.ContextCalculator;

public class RolesPermissionService implements PermissionService
{
    private final ConcurrentMap<String, SubjectCollection> collections = new ConcurrentHashMap<>();
    private final List<ContextCalculator> calculators = new CopyOnWriteArrayList<>();

    private final SubjectData defaultData;
    private Database db;
    private RolesConfig config;

    public RolesPermissionService(Roles module, Reflector reflector, RolesConfig config, Game game, Database db)
    {
        this.db = db;
        this.config = config;
        defaultData = new DefaultSubjectData(this, config);
        collections.put(SUBJECTS_USER, new UserCollection(this, game));
        collections.put(SUBJECTS_GROUP, new RoleCollection(module, this, reflector));
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
    public SubjectData getDefaultData()
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
}
