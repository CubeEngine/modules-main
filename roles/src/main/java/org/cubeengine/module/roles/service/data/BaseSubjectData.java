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
package org.cubeengine.module.roles.service.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.exception.CircularRoleDependencyException;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.collection.FileBasedCollection;
import org.cubeengine.module.roles.service.collection.UserCollection;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.NodeTree;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.service.permission.TransferMethod;
import org.spongepowered.api.util.Tristate;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.cubeengine.libcube.util.ContextUtil.GLOBAL;
import static org.cubeengine.libcube.util.ContextUtil.toSet;
import static org.spongepowered.api.service.context.Context.WORLD_KEY;

/**
 * The Base for Roles OptionSubjectData without persistence
 */
public class BaseSubjectData implements SubjectData
{
    protected final Map<Context, Map<String, String>> options = new ConcurrentHashMap<>();
    protected final Map<Context, Map<String, Boolean>> permissions = new ConcurrentHashMap<>();
    protected final Map<Context, List<SubjectReference>> parents = new ConcurrentHashMap<>();

    protected final UserCollection userCollection;
    protected final FileBasedCollection roleCollection;
    protected final RolesPermissionService service;
    private final boolean isTransient;
    private final Subject subject;

    public BaseSubjectData(RolesPermissionService service, Subject holder, boolean isTransient)
    {
        this.service = service;
        userCollection = service.userSubjects();
        roleCollection = service.groupSubjects();
        this.isTransient = isTransient;
        this.subject = holder;
    }

    public static String stringify(Context c)
    {
        return c.getValue().isEmpty() ? c.getKey() : c.getKey() + "|" + c.getValue();
    }

    public static Context asContext(String string)
    {
        String[] context = string.split("\\|");
        if (context.length == 1)
        {
            if (context[0].equals(GLOBAL.getKey()))
            {
                return new Context(context[0], "");
            }
            return new Context(WORLD_KEY, context[0]);
        }
        if (context.length == 2)
        {
            return new Context(context[0], context[1]);
        }
        throw new IllegalStateException("Invalid context " + string);
    }

    @Override
    public Map<Set<Context>, Map<String, String>> allOptions()
    {
        Map<Set<Context>, Map<String, String>> options = this.options.entrySet().stream()
                .collect(Collectors.toMap(e -> toSet(e.getKey()), Map.Entry::getValue));
        return unmodifiableMap(options);
    }

    @Override
    public Map<String, String> options(Set<Context> contexts)
    {
        return unmodifiableMap(accumulate(contexts, options, new HashMap<>(), Map::putAll));
    }

    @Override
    public Map<String, Boolean> permissions(Set<Context> contexts)
    {
        return unmodifiableMap(accumulate(contexts, permissions, new HashMap<>(), Map::putAll));
    }

    @Override
    public List<SubjectReference> parents(Set<Context> contexts)
    {
        List<SubjectReference> list = accumulate(contexts, parents, new ArrayList<>(), List::addAll);
        return unmodifiableList(list);
    }

    @Override
    public CompletableFuture<Boolean> setOption(Set<Context> contexts, String key, String value)
    {
        return CompletableFuture.supplyAsync(() -> {
            if (value == null)
            {
                return operate(contexts, options, map -> map.remove(key) != null);
            }
            return operate(contexts, options, map -> !value.equals(map.put(key, value)), HashMap::new);
        });
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(Set<Context> contexts)
    {
        return CompletableFuture.supplyAsync(() ->
            operate(contexts, options,  m -> {
                boolean empty = !m.isEmpty();
                m.clear();
                return empty;
        }));
    }

    @Override
    public CompletableFuture<Boolean> clearOptions()
    {
        return CompletableFuture.supplyAsync(() -> {
            boolean changed = false;
            for (Map<String, String> map : options.values())
            {
                if (!map.isEmpty())
                {
                    changed = true;
                }
                map.clear();
            }
            return changed;
        });
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> allPermissions()
    {
        Map<Set<Context>, Map<String, Boolean>> permissions = this.permissions.entrySet().stream()
                .collect(Collectors.toMap(e -> toSet(e.getKey()), Map.Entry::getValue));

        return unmodifiableMap(permissions);
    }

    @Override
    public CompletableFuture<Boolean> setPermission(Set<Context> contexts, String permission, Tristate value)
    {
        return CompletableFuture.supplyAsync(() -> {
            if (value == Tristate.UNDEFINED)
            {
                return operate(contexts, permissions, map -> map.remove(permission) != null);
            }
            return operate(contexts, permissions, map -> {
                Boolean replaced = map.put(permission, value.asBoolean());
                return replaced == null || replaced != value.asBoolean();
            }, HashMap::new);
        });
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(Set<Context> contexts)
    {
        return CompletableFuture.supplyAsync(() ->
            operate(contexts, permissions, m -> {
                boolean empty = !m.isEmpty();
                m.clear();
                return empty;
        }));
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions()
    {
        return CompletableFuture.supplyAsync(() -> {
            boolean changed = false;
            for (Map<String, Boolean> map : permissions.values())
            {
                if (!map.isEmpty())
                {
                    changed = true;
                }
                map.clear();
            }
            return changed;
        });
    }

    @Override
    public Map<Set<Context>, List<SubjectReference>> allParents()
    {
        Map<Set<Context>, List<SubjectReference>> parents = this.parents.entrySet().stream()
                .collect(Collectors.toMap(e -> toSet(e.getKey()), Map.Entry::getValue));

        return unmodifiableMap(parents);
    }

    @Override
    public CompletableFuture<Boolean> addParent(Set<Context> contexts, SubjectReference parent)
    {
        CompletableFuture<Boolean> ret = parent.resolve().thenApply(p -> {
            if (PermissionService.SUBJECTS_DEFAULT.equals(parent.collectionIdentifier())) {
                return false; // You can never add defaults as parents
            }

            checkForCircularDependency(contexts, p, 0);

            if (contexts.isEmpty() && parents.get(GLOBAL) != null && parents.get(GLOBAL).contains(parent)) {
                return false;
            }

            for (Context context : contexts) {
                if (parents.containsKey(context) && parents.get(context).contains(parent)) {
                    return false;
                }
            }

            return operate(contexts, parents, l -> l.add(p.asSubjectReference()), ArrayList::new);
        });
        RolesUtil.invalidateCache();
        return ret;
    }

    protected void checkForCircularDependency(Set<Context> contexts, Subject parent, int depth)
    {
        if (this == parent.subjectData())
        {
            throw new CircularRoleDependencyException("at", depth); // TODO translatable / show parameter
        }
        depth++;

        for (SubjectReference parentParents : parent.parents(contexts))
        {
            checkForCircularDependency(contexts, parentParents.resolve().join(), depth);
        }
    }

    @Override
    public CompletableFuture<Boolean> removeParent(Set<Context> contexts, SubjectReference parent)
    {
        CompletableFuture<Boolean> ret = CompletableFuture.supplyAsync(() -> operate(contexts, parents, l -> l.remove(parent)));
        RolesUtil.invalidateCache();
        return ret;
    }

    @Override
    public CompletableFuture<Boolean> clearParents()
    {
        CompletableFuture<Boolean> ret = CompletableFuture.supplyAsync(() -> {
            boolean changed = false;
            for (List<SubjectReference> list : parents.values()) {
                if (!list.isEmpty()) {
                    list.clear();
                    changed = true;
                }
            }
            return changed;
        });
        RolesUtil.invalidateCache();
        return ret;
    }

    @Override
    public CompletableFuture<Boolean> clearParents(Set<Context> contexts)
    {
        CompletableFuture<Boolean> ret = CompletableFuture.supplyAsync(() ->
                operate(contexts, parents, l -> {
                    boolean empty = !l.isEmpty();
                    l.clear();
                    return empty;
                }));
        RolesUtil.invalidateCache();
        return ret;
    }

    @FunctionalInterface
    interface Operator<T>
    {
        boolean operate(T mapOrList);
    }

    private <T> boolean operate(Set<Context> contexts, Map<Context, T> all, Operator<T> operator)
    {
        return operate(contexts, all, operator, () -> null);
    }

    private <T> boolean operate(Set<Context> contexts, Map<Context, T> all, Operator<T> operator, Supplier<T> provider)
    {
        boolean changed = false;
        if (contexts.isEmpty())
        {
            changed = operateOn(all, operator, provider, changed, GLOBAL);
        }
        for (Context context : contexts)
        {
            changed = operateOn(all, operator, provider, changed, context);
        }
        return changed;
    }

    private <T> boolean operateOn(Map<Context, T> all, Operator<T> operator, Supplier<T> provider, boolean changed, Context context)
    {
        T val = all.get(context);
        if (val == null)
        {
            val = provider.get();
            if (val != null)
            {
                all.put(context, val);
            }
        }
        if (val != null)
        {
            changed = operator.operate(val);
        }
        return changed;
    }

    @FunctionalInterface
    interface Accumulator<T>
    {
        void operate(T mapOrList, T other);
    }

    private <T> T accumulate(Set<Context> contexts, Map<Context, T> all, T result, Accumulator<T> accumulator)
    {
        T other = all.get(GLOBAL);
        if (other != null)
        {
            accumulator.operate(result, other);
        }

        for (Context context : contexts)
        {
            other = all.get(context);
            if (other != null)
            {
                accumulator.operate(result, other);
            }
        }
        return result;
    }

    @Override
    public Subject subject()
    {
        return this.subject;
    }

    @Override
    public boolean isTransient()
    {
        return this.isTransient;
    }

    @Override
    public Tristate fallbackPermissionValue(Set<Context> contexts)
    {
        return Tristate.UNDEFINED;
    }

    @Override
    public Map<Set<Context>, Tristate> allFallbackPermissionValues()
    {
        return Collections.emptyMap();
    }

    @Override
    public CompletableFuture<Boolean> setFallbackPermissionValue(Set<Context> contexts, Tristate fallback)
    {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> clearFallbackPermissionValues()
    {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> setPermissions(Set<Context> contexts, Map<String, Boolean> permissions, TransferMethod method)
    {
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(true);
        if (method == TransferMethod.OVERWRITE)
        {
            future = this.clearOptions(contexts);
        }
        return future.thenCompose(b -> CompletableFuture.allOf(permissions.entrySet().stream().map(e ->
           this.setPermission(contexts, e.getKey(), Tristate.fromBoolean(e.getValue()))).toArray(CompletableFuture[]::new)).thenApply(v -> true));
    }

    @Override
    public CompletableFuture<Boolean> setParents(Set<Context> contexts, List<? extends SubjectReference> parents, TransferMethod method)
    {
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(true);
        if (method == TransferMethod.OVERWRITE)
        {
            future = this.clearOptions(contexts);
        }
        future = future.thenCompose(b -> CompletableFuture.allOf(parents.stream().map(p ->
                       this.addParent(contexts, p)).toArray(CompletableFuture[]::new)).thenApply(v -> true));
        RolesUtil.invalidateCache();
        return future;
    }

    @Override
    public CompletableFuture<Boolean> setOptions(Set<Context> contexts, Map<String, String> options, TransferMethod method)
    {
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(true);
        if (method == TransferMethod.OVERWRITE)
        {
            future = this.clearOptions(contexts);
        }
        return future.thenCompose(b -> CompletableFuture.allOf(options.entrySet().stream().map(e ->
           this.setOption(contexts, e.getKey(), e.getValue())).toArray(CompletableFuture[]::new)).thenApply(v -> true));
    }

    @Override
    public CompletableFuture<Boolean> copyFrom(SubjectData other, TransferMethod method)
    {
        final CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            switch (method)
            {
                case OVERWRITE:
                    this.permissions.clear();
                    this.options.clear();
                    this.parents.clear();
                case MERGE:
                    this.permissions.putAll(((BaseSubjectData)other).permissions);
                    this.options.putAll(((BaseSubjectData)other).options);
                    this.parents.putAll(((BaseSubjectData)other).parents);
            }
            return true;
        });
        RolesUtil.invalidateCache();
        return future;
    }

    @Override
    public CompletableFuture<Boolean> moveFrom(SubjectData other, TransferMethod method)
    {
        return this.copyFrom(other, method).thenApply(b -> {
            other.clearPermissions();
            other.clearOptions();
            other.clearParents();
            return b;
        });
    }
}
