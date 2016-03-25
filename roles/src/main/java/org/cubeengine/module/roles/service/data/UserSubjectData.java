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
package org.cubeengine.module.roles.service.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.data.PermissionData;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class UserSubjectData extends CachingSubjectData
{
    private final UUID uuid;

    public UserSubjectData(RolesPermissionService service, UUID uuid)
    {
        super(service);
        this.uuid = uuid;
    }

    @Override
    protected void cacheOptions(Set<Context> c)
    {
        for (Context context : c)
        {
            context = service.getMirror(context);
            String contextString = stringify(context) + "#";
            if (!options.containsKey(context))
            {
                Map<String, String> opts = getData()
                    .map(PermissionData::getOptions)
                    .orElse(Collections.emptyMap());

                opts = opts.entrySet().stream()
                        .filter(e -> e.getKey().startsWith(contextString))
                        .collect(toMap(e -> e.getKey().split("#")[1], Map.Entry::getValue));

                options.put(context, opts);
            }
        }
    }

    @Override
    protected void cachePermissions(Set<Context> c)
    {
        for (Context context : c)
        {
            context = service.getMirror(context);
            String contextString = stringify(context) + "#";
            if (!permissions.containsKey(context))
            {
                Map<String, Boolean> perms = getData()
                    .map(PermissionData::getPermissions)
                    .orElse(Collections.emptyMap());

                perms = perms.entrySet().stream()
                        .filter(e -> e.getKey().startsWith(contextString))
                        .collect(toMap(e -> e.getKey().split("#")[1], Map.Entry::getValue));

                permissions.put(context, perms);
            }
        }
    }

    private Optional<PermissionData> getData()
    {
        UserStorageService storage = Sponge.getServiceManager().provide(UserStorageService.class).get();
        User player = storage.get(uuid).get()
            .getPlayer().get(); // TODO wait for User Data impl

        Optional<PermissionData> permData = player.get(PermissionData.class);
        if (permData.isPresent())
        {
            return permData;
        }
        return Optional.empty();
    }

    @Override
    protected void cacheParents(Set<Context> c)
    {
        if (!parents.containsKey(RolesUtil.GLOBAL))
        {
            List<String> parentList = getData()
                .map(PermissionData::getParents)
                .orElse(Collections.emptyList());
            List<Subject> list = parentList.stream()
                                           .map(r -> "role:" + r)
                                           .map(roleCollection::get)
                                           .sorted((o1, o2) -> {
                                               if (o1 != null && o2 != null)
                                               {
                                                   return o1.compareTo(o2);
                                               }
                                               return 1;
                                           })
                                           .map(Subject.class::cast)
                                           .collect(toList());
            parents.put(RolesUtil.GLOBAL, list);
        }
    }

    @Override
    public boolean save(boolean changed)
    {
        if (changed)
        {
            UserStorageService storage = Sponge.getServiceManager().provide(UserStorageService.class).get();

            // On users only global assigned Roles get persisted
            List<String> parents = this.parents.get(RolesUtil.GLOBAL).stream()
                                               .filter(s -> s instanceof RoleSubject)
                                               // TODO WARN: Subject that is not a role will not be persisted
                                               .map(RoleSubject.class::cast)
                                               .map(subject -> subject.getName())
                                               .collect(Collectors.toList());

            Map<String, Boolean> permissions = this.permissions.entrySet().stream().flatMap(e -> {
                String context = stringify(e.getKey()) + "#";
                return e.getValue().entrySet().stream()
                        .collect(toMap(
                                ee -> context + ee.getKey(),
                                Map.Entry::getValue
                        )).entrySet().stream();
            }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            Map<String, String> options = this.options.entrySet().stream().flatMap(e -> {
                String context = stringify(e.getKey())+ "#";
                return e.getValue().entrySet().stream()
                        .collect(toMap(
                                ee -> context + ee.getKey(),
                                Map.Entry::getValue
                        )).entrySet().stream();
            }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            User user = storage.get(uuid).get();
            user.offer(new PermissionData(parents, permissions, options));

            // TODO remove once saving data on user is implemented
            user.getPlayer().get().offer(new PermissionData(parents, permissions, options));
        }
        return changed;
    }

    @Override
    protected void cacheParents()
    {
        // TODO cache all the things
    }

    @Override
    protected void cachePermissions()
    {
        // TODO cache all the things
    }

    @Override
    protected void cacheOptions()
    {
        // TODO cache all the things
    }
}