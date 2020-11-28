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
package org.cubeengine.module.roles.service;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesConfig;
import org.cubeengine.module.roles.config.PermissionTree;
import org.cubeengine.module.roles.config.PermissionTreeConverter;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.config.PriorityConverter;
import org.cubeengine.module.roles.service.collection.FileBasedCollection;
import org.cubeengine.module.roles.service.collection.UserCollection;
import org.cubeengine.module.roles.service.subject.RolesSubjectReference;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionDescription.Builder;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.plugin.PluginContainer;

import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

@Singleton
public class RolesPermissionService implements PermissionService
{
    private final ConcurrentMap<String, SubjectCollection> collections = new ConcurrentHashMap<>();
    private final List<ContextCalculator<Subject>> calculators = new CopyOnWriteArrayList<>();
    private Reflector reflector;
    private final Path modulePath;

    private RolesConfig config;
    private Log logger;

    private final Map<String, PermissionDescription> descriptionMap = new LinkedHashMap<String, PermissionDescription>();
    private Collection<PermissionDescription> descriptions;
    @Inject private PermissionManager pm;

    @Inject
    public RolesPermissionService(Roles module, FileManager fm, Reflector reflector, ModuleManager mm)
    {
        ConverterManager cManager = reflector.getDefaultConverterManager();
        cManager.registerConverter(new PermissionTreeConverter(this), PermissionTree.class);
        cManager.registerConverter(new PriorityConverter(), Priority.class);

        this.reflector = reflector;
        this.modulePath = mm.getPathFor(Roles.class);
        this.logger = mm.getLoggerFor(Roles.class);
        this.config = fm.loadConfig(module, RolesConfig.class);
        collections.put(SUBJECTS_DEFAULT, new FileBasedCollection(modulePath, this, reflector, SUBJECTS_DEFAULT, true));
        collections.put(SUBJECTS_USER, new UserCollection(this));
        collections.put(SUBJECTS_GROUP, new FileBasedCollection(modulePath, this, reflector, SUBJECTS_GROUP, true));

        this.getLoadedCollections().values().stream()
                .filter(c -> c instanceof FileBasedCollection)
                .map(FileBasedCollection.class::cast)
                .forEach(FileBasedCollection::reload);

        collections.put(SUBJECTS_SYSTEM, new FileBasedCollection(modulePath,this, reflector, SUBJECTS_SYSTEM, true));
        // TODO SUBJECTS_COMMAND_BLOCK
        collections.put(SUBJECTS_ROLE_TEMPLATE, new FileBasedCollection(modulePath, this, reflector, SUBJECTS_ROLE_TEMPLATE, true));
    }

    @Override
    public UserCollection getUserSubjects()
    {
        return (UserCollection)collections.get(SUBJECTS_USER);
    }

    @Override
    public FileBasedCollection getGroupSubjects()
    {
        return (FileBasedCollection)collections.get(SUBJECTS_GROUP);
    }

    @Override
    public Subject getDefaults()
    {
        try
        {
            return getCollection(SUBJECTS_DEFAULT).get().loadSubject(SUBJECTS_DEFAULT).get();
        }
        catch (ExecutionException | InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<SubjectCollection> getCollection(String identifier)
    {
        return Optional.of(this.collections.computeIfAbsent(identifier, i -> new FileBasedCollection(modulePath, this, reflector, i).reload()));
    }

    @Override
    public Map<String, SubjectCollection> getLoadedCollections()
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
    public Builder newDescriptionBuilder(PluginContainer plugin)
    {
        return new RolesPermissionDescriptionBuilder(plugin, this);
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
        SubjectCollection subjects = getCollection(SUBJECTS_ROLE_TEMPLATE).get(); // TODO prevent infinite recursion
        roleAssignments.forEach((key, value) ->
            subjects.loadSubject(key).thenAccept(s -> s.getTransientSubjectData().setPermission(GLOBAL_CONTEXT, desc.getId(), value)));

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


    @Override
    public Predicate<String> getIdentifierValidityPredicate()
    {
        return input -> true;
    }

    @Override
    public CompletableFuture<SubjectCollection> loadCollection(String identifier) {
        return CompletableFuture.completedFuture(this.getCollection(identifier).get());
    }

    @Override
    public CompletableFuture<Boolean> hasCollection(String identifier)
    {
        return CompletableFuture.completedFuture(this.collections.containsKey(identifier));
    }

    @Override
    public CompletableFuture<Set<String>> getAllIdentifiers()
    {
        return CompletableFuture.completedFuture(this.collections.keySet());
    }

    @Override
    public SubjectReference newSubjectReference(String collectionIdentifier, String subjectIdentifier)
    {
        return new RolesSubjectReference(subjectIdentifier, getCollection(collectionIdentifier).get());
    }

    public PermissionManager getPermissionManager()
    {
        return pm;
    }

    public Log getLog()
    {
        return logger;
    }

    public void fullReload()
    {
        this.getConfig().reload();
        // TODO
        this.getLoadedCollections().values().stream()
               .filter(c -> c instanceof FileBasedCollection)
               .map(FileBasedCollection.class::cast)
               .forEach(FileBasedCollection::reload);

        this.getUserSubjects().reload();

        // TODO remove cached data ; needed?
    }

}
