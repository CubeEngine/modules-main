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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.cubeengine.libcube.util.ContextUtil;
import org.cubeengine.module.roles.data.PermissionData;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.cubeengine.module.roles.service.subject.UserSubject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.MapValue;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.user.UserManager;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.*;
import static org.cubeengine.module.roles.RolesUtil.contextOf;

public class UserSubjectData extends CachingSubjectData
{
    private final UUID uuid;

    public UserSubjectData(RolesPermissionService service, UUID uuid, UserSubject holder)
    {
        super(service, holder);
        this.uuid = uuid;
    }

    private <E> Optional<E> getData(Key<? extends Value<E>> key)
    {
        final Optional<ServerPlayer> player = Sponge.server().player(uuid);
        if (player.isPresent())
        {
            return player.get().get(key);
        }
        final UserManager userManager = Sponge.server().userManager();
        User user = userManager.find(uuid).get();
        return user.get(key);
    }

    @Override
    public CompletableFuture<Boolean> save(CompletableFuture<Boolean> c)
    {
        return c.thenApply(changed -> {
            if (changed)
            {
                cacheOptions();
                cachePermissions();
                cacheParents();
                // Serialize Data
                List<String> parents = serializeToList(this.parents);
                Map<String, Boolean> permissions = serializeToMap(this.permissions);
                Map<String, String> options = serializeToMap(this.options);

                // Get User for Storage
                final UserManager userManager = Sponge.server().userManager();
                User user = userManager.find(uuid).get();
                // Save Data in User
                user.offer(PermissionData.PARENTS, parents);
                user.offer(PermissionData.PERMISSIONS, permissions);
                user.offer(PermissionData.OPTIONS, options);

                Sponge.server().player(uuid).ifPresent(p -> Sponge.server().commandManager().updateCommandTreeForPlayer(p));
            }
            return changed;
        });
    }

    private List<String> serializeToList(Map<Context, List<SubjectReference>> map)
    {
        // On users only global assigned Roles get persisted
        return map.get(ContextUtil.GLOBAL).stream().map(FileSubject::getInternalIdentifier).collect(Collectors.toList());
    }

    private <T> Map<String, T> serializeToMap(Map<Context, Map<String, T>> map)
    {
        return map.entrySet().stream().flatMap(e ->
                        e.getValue().entrySet().stream().collect(mapContextKeyCollector(e)).entrySet().stream())
                  .collect(toMap(Entry::getKey, Entry::getValue));
    }

    private <T> Collector<Entry<String, T>, ?, Map<String, T>> mapContextKeyCollector(Entry<Context, Map<String, T>> entry)
    {
        return toMap(ee -> stringify(entry.getKey())+ "#" + ee.getKey(), Entry::getValue);
    }

    @Override
    protected void cacheParents()
    {
        if (!parents.containsKey(ContextUtil.GLOBAL))
        {
            List<String> parentList = getData(PermissionData.PARENTS).orElse(Collections.emptyList());
            boolean parentRemoved = false;
            List<Subject> list = new ArrayList<>();
            for (String s : new HashSet<>(parentList))
            {
                FileSubject subject = roleCollection.getByInternalIdentifier(s, uuid.toString());
                if (subject == null)
                {
                    parentRemoved = true;
                }
                else
                {
                    list.add(subject);
                }
            }
            list.sort(FileSubject::compare);
            parents.put(ContextUtil.GLOBAL, list.stream().map(Subject::asSubjectReference).collect(toList()));
            if (parentRemoved)
            {
                save(CompletableFuture.completedFuture(true));
            }
        }
    }

    @Override
    protected void cachePermissions()
    {
        if (permissions.isEmpty())
        {
            permissions.putAll(deserialzeMap(PermissionData.PERMISSIONS));
        }
    }

    @Override
    protected void cacheOptions()
    {
        if (options.isEmpty())
        {
            options.putAll(deserialzeMap(PermissionData.OPTIONS));
        }
    }

    private <T> Map<Context, Map<String, T>> deserialzeMap(Key<MapValue<String, T>> key)
    {
        return this.getData(key).orElse(emptyMap()).entrySet().stream().collect(groupedByContext());
    }

    private <T> Collector<Entry<String, T>, ?, Map<Context, Map<String, T>>> groupedByContext()
    {
        return groupingBy(e -> contextOf(e.getKey().split("#")[0]), toMap(e -> e.getKey().split("#")[1], Entry::getValue));
    }
}