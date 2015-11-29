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
package org.cubeengine.module.roles.sponge.data;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.cubeengine.module.roles.config.RoleConfig;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.collection.RoleCollection;
import org.cubeengine.module.roles.sponge.subject.RoleSubject;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;

import static org.cubeengine.module.roles.commands.RoleCommands.toSet;
import static java.util.Collections.singleton;

public class RoleSubjectData extends CachingSubjectData
{
    private final RoleConfig config;
    private Context context;
    private final Set<Context> contexts;
    private final RoleCollection collection;

    public RoleSubjectData(RolesPermissionService service, RoleConfig config, Context context)
    {
        this.config = config;
        this.context = context;
        this.contexts = context == null ? GLOBAL_CONTEXT : singleton(context);
        this.collection = service.getGroupSubjects();
    }

    @Override
    protected void cacheOptions(Set<Context> c)
    {
        c.stream()
         .filter(ctx -> toSet(ctx).equals(getContexts()) && !options.containsKey(getContexts()))
         .forEach(ctx -> options.put(getContexts(), config.metadata));
    }

    @Override
    protected void cachePermissions(Set<Context> c)
    {
        c.stream()
         .filter(ctx -> toSet(ctx).equals(getContexts()) && !permissions.containsKey(getContexts()))
         .forEach(ctx -> permissions.put(getContexts(), config.perms.getPermissions()));
    }

    @Override
    protected void cacheParents(Set<Context> c)
    {
        for (Context ctx : c)
        {
            if (toSet(ctx).equals(getContexts()) && !parents.containsKey(getContexts()))
            {
                List<RoleSubject> parents = new ArrayList<>();
                for (String parent : config.parents)
                {
                    if (!parent.contains(RoleSubject.SEPARATOR))
                    {
                        parent = context.getKey() + RoleSubject.SEPARATOR + context.getValue() + RoleSubject.SEPARATOR + parent;
                    }
                    parents.add(collection.get("role:" + parent));
                }
                Collections.sort(parents);
                this.parents.put(contexts, new ArrayList<>(parents));
            }
        }
    }

    @Override
    public boolean save(boolean changed)
    {
        if (changed)
        {
            List<Subject> list = parents.get(contexts);
            if (list != null)
            {
                config.parents.clear();
                for (Subject subject : list)
                {
                    if (!(subject instanceof RoleSubject))
                    {
                        // TODO WARN: Subject that is not a role will not be persisted
                        continue;
                    }
                    config.parents.add(subject.getIdentifier().substring(5));
                }
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

    public RoleConfig getConfig()
    {
        return config;
    }

    @Override
    public Set<Context> getContexts()
    {
        return contexts;
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
