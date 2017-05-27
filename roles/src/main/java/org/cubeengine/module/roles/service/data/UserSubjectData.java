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
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.cubeengine.module.roles.data.PermissionData;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.cubeengine.libcube.util.ContextUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.*;
import static org.cubeengine.module.roles.RolesUtil.contextOf;

public class UserSubjectData extends CachingSubjectData
{
    private final UUID uuid;

    public UserSubjectData(RolesPermissionService service, UUID uuid)
    {
        super(service);
        this.uuid = uuid;
    }

    private Optional<PermissionData> getData()
    {
        UserStorageService storage = Sponge.getServiceManager().provide(UserStorageService.class).get();
        User player = storage.get(uuid).get();
        player = player.getPlayer().map(User.class::cast).orElse(player); // TODO wait for User Data impl

        return player.get(PermissionData.class);
    }

    @Override
    public boolean save(boolean changed)
    {
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
            UserStorageService storage = Sponge.getServiceManager().provide(UserStorageService.class).get();
            User user = storage.get(uuid).get();
            // Save Data in User
            user.offer(new PermissionData(parents, permissions, options));
            // actually save Data in Player til -> TODO remove once saving data on user is implemented
            user.getPlayer().map(User.class::cast).orElse(user).offer(new PermissionData(parents, permissions, options));
        }
        return changed;
    }

    private List<String> serializeToList(Map<Context, List<Subject>> map)
    {
        // On users only global assigned Roles get persisted
        return map.get(ContextUtil.GLOBAL).stream().map(RoleSubject::getInternalIdentifier).collect(Collectors.toList());
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
            List<String> parentList = getData().map(PermissionData::getParents).orElse(Collections.emptyList());
            boolean parentRemoved = false;
            List<Subject> list = new ArrayList<>();
            for (String s : new HashSet<>(parentList))
            {
                RoleSubject subject = roleCollection.getByInternalIdentifier(s, uuid.toString());
                if (subject == null)
                {
                    parentRemoved = true;
                }
                else
                {
                    list.add(subject);
                }
            }
            list.sort(RoleSubject::compare);
            parents.put(ContextUtil.GLOBAL, list);
            if (parentRemoved)
            {
                save(true);
            }
        }
    }

    @Override
    protected void cachePermissions()
    {
        if (!permissions.isEmpty())
        {
            permissions.putAll(deserialzeMap(PermissionData::getPermissions));
        }
    }

    @Override
    protected void cacheOptions()
    {
        if (options.isEmpty())
        {
            options.putAll(deserialzeMap(PermissionData::getOptions));
        }
    }

    private <T> Map<Context, Map<String, T>> deserialzeMap(Function<PermissionData, Map<String, T>> func)
    {
        return getData().map(func).orElse(emptyMap()).entrySet().stream().collect(groupedByContext());
    }

    private <T> Collector<Entry<String, T>, ?, Map<Context, Map<String, T>>> groupedByContext()
    {
        return groupingBy(e -> contextOf(e.getKey().split("#")[0]), toMap(e -> e.getKey().split("#")[1], Entry::getValue));
    }
}