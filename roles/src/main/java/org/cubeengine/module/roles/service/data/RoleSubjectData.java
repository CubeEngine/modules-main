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

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cubeengine.module.roles.config.PermissionTree;
import org.cubeengine.module.roles.config.RoleConfig;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;

import static java.util.stream.Collectors.toList;

public class RoleSubjectData extends CachingSubjectData
{
    private final RoleConfig config;

    public RoleSubjectData(RolesPermissionService service, RoleConfig config)
    {
        super(service);
        this.config = config;
    }

    @Override
    protected void cacheOptions(Set<Context> c)
    {
        cacheOptions();
    }

    @Override
    protected void cachePermissions(Set<Context> c)
    {
        cachePermissions();
    }

    @Override
    protected void cacheParents(Set<Context> c)
    {
        cacheParents();
    }

    @Override
    public boolean save(boolean changed)
    {
        if (changed)
        {
            config.settings = new HashMap<>();
            for (Map.Entry<Context, List<Subject>> entry : parents.entrySet())
            {
                List<String> collect = entry.getValue().stream()
                        .filter(s -> s instanceof RoleSubject) // TODO WARN: Subject that is not a role will not be persisted
                        .map(RoleSubject.class::cast)
                        .map(RoleSubject::getName)
                        .collect(toList());

                getContextSetting(entry.getKey()).parents.addAll(collect);
            }

            for (Map.Entry<Context, Map<String, Boolean>> entry : permissions.entrySet())
            {
                getContextSetting(entry.getKey()).permissions = new PermissionTree().setPermissions(entry.getValue());
            }

            for (Map.Entry<Context, Map<String, String>> entry : options.entrySet())
            {
                getContextSetting(entry.getKey()).options = entry.getValue();
            }

            try
            {
                Files.createDirectories(config.getFile().toPath().getParent());
            }
            catch (IOException e)
            {
                throw new IllegalStateException(e);
            }
            config.save();// TODO async
        }
        return changed;
    }

    private RoleConfig.ContextSetting getContextSetting(Context context)
    {
        String contextString = stringify(context);
        RoleConfig.ContextSetting setting = config.settings.get(contextString);
        if (setting == null)
        {
            setting = new RoleConfig.ContextSetting();
            config.settings.put(contextString, setting);
        }
        return setting;
    }

    public RoleConfig getConfig()
    {
        return config;
    }

    @Override
    protected void cacheParents()
    {
        if (parents.isEmpty()) // not cached
        {
            for (Map.Entry<String, RoleConfig.ContextSetting> entry : config.settings.entrySet())
            {
                List<RoleSubject> collect = entry.getValue().parents.stream().map(n -> "role:" + n).map(roleCollection::get).collect(toList());
                Collections.sort(collect);
                parents.put(asContext(entry.getKey()), new ArrayList<>(collect));
            }
        }
    }

    @Override
    protected void cachePermissions()
    {
        if (permissions.isEmpty()) // not cached
        {
            for (Map.Entry<String, RoleConfig.ContextSetting> entry : config.settings.entrySet())
            {
                permissions.put(asContext(entry.getKey()), entry.getValue().permissions.getPermissions());
            }
        }
    }

    @Override
    protected void cacheOptions()
    {
        if (options.isEmpty()) // not cached
        {
            for (Map.Entry<String, RoleConfig.ContextSetting> entry : config.settings.entrySet())
            {
                options.put(asContext(entry.getKey()), entry.getValue().options);
            }
        }
    }

    /* TODO rename and delete
     public boolean rename(String newName)
    {
        if (this.provider.getRole(newName) != null)
        {
            return false;
        }
        this.makeDirty();
        if (this.isGlobal())
        {
            this.manager.dsl.update(TABLE_ROLE).set(DSL.row(TABLE_ROLE.ROLE), DSL.row("g:" + newName)).
                where(TABLE_ROLE.ROLE.eq(this.getName())).execute();
        }
        else
        {
            Set<UInteger> worldMirrors = new HashSet<>();
            for (Entry<World, Triplet<Boolean, Boolean, Boolean>> entry : ((WorldRoleProvider)provider).getWorldMirrors().entrySet())
            {
                if (entry.getValue().getSecond())
                {
                    worldMirrors.add(wm.getWorldId(entry.getKey()));
                }
            }
            this.manager.dsl.update(TABLE_ROLE).set(TABLE_ROLE.ROLE, newName).
                where(TABLE_ROLE.ROLE.eq(this.getName()), TABLE_ROLE.CONTEXT.in(worldMirrors)).execute();
        }
        this.delete();
        this.config.roleName = newName;
        this.provider.addRole(this);
        for (Role role : this.resolvedRoles)
        {
            role.dependentRoles.add(this);
        }
        for (ResolvedDataHolder dataHolder : this.dependentRoles)
        {
            dataHolder.assignRole(this);
        }
        this.config.setTarget(new File(this.config.getTarget().getParent(), this.config.roleName + ".yml"));
        this.save();
        return true;
    }

    public void delete()
    {
        for (Role role : this.resolvedRoles)
        {
            role.dependentRoles.remove(this);
        }
        for (ResolvedDataHolder dataHolder : this.dependentRoles)
        {
            dataHolder.removeRole(this);
        }
        if (this.isGlobal())
        {
            this.manager.dsl.delete(TABLE_ROLE).where(TABLE_ROLE.ROLE.eq(this.getName())).execute();
        }
        else
        {
            Set<UInteger> worldMirrors = new HashSet<>();
            for (Entry<World, Triplet<Boolean, Boolean, Boolean>> entry : ((WorldRoleProvider)provider).getWorldMirrors().entrySet())
            {
                if (entry.getValue().getSecond())
                {
                    worldMirrors.add(wm.getWorldId(entry.getKey()));
                }
            }
            this.manager.dsl.delete(TABLE_ROLE).where(TABLE_ROLE.ROLE.eq(this.getName()),
                                                      TABLE_ROLE.CONTEXT.in(worldMirrors)).execute();
        }
        this.provider.removeRole(this);
        try
        {
            Files.delete(this.config.getTarget().toPath());
        }
        catch (IOException e)
        {
            logger.error(e, "Could not delete role {}!", this.config.getTarget().getName());
        }
    }
     */

    @Override
    public void reload()
    {
        config.reload();
        super.reload();
    }

    public void delete()
    {
        try
        {
            Files.delete(config.getFile().toPath());
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
   }
}
