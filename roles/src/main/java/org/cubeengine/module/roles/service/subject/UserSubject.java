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
package org.cubeengine.module.roles.service.subject;

import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.data.UserSubjectData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class UserSubject extends BaseSubject<UserSubjectData>
{
    private final UUID uuid;
    boolean defaultsApplied = false;

    public UserSubject(RolesPermissionService service, UUID uuid)
    {
        super(service.getUserSubjects(), service, new UserSubjectData(service, uuid));
        this.uuid = uuid;
        applyDefault();
    }

    private void applyDefault()
    {
        defaultsApplied = true;
        SubjectData transientData = getTransientSubjectData();
        SubjectData defaultData = service.getDefaults().getSubjectData(); // TODO transient too?
        for (Entry<Set<Context>, Map<String, Boolean>> entry : defaultData.getAllPermissions().entrySet())
        {
            for (Entry<String, Boolean> perm : entry.getValue().entrySet())
            {
                transientData.setPermission(entry.getKey(), perm.getKey(), Tristate.fromBoolean(perm.getValue()));
            }
        }
        for (Entry<Set<Context>, List<Subject>> entry : defaultData.getAllParents().entrySet())
        {
            for (Subject subject : entry.getValue())
            {
                transientData.addParent(entry.getKey(), subject);
            }
        }
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts)
    {
        List<Subject> parents = super.getParents(contexts);
        if (!parents.isEmpty() && defaultsApplied)
        {
            SubjectData transientData = getTransientSubjectData();
            SubjectData defaultData = service.getDefaults().getSubjectData(); // TODO transient too?
            for (Entry<Set<Context>, Map<String, Boolean>> entry : defaultData.getAllPermissions().entrySet())
            {
                for (Entry<String, Boolean> perm : entry.getValue().entrySet())
                {
                    transientData.setPermission(entry.getKey(), perm.getKey(), Tristate.UNDEFINED);
                }
            }
            for (Entry<Set<Context>, List<Subject>> entry : defaultData.getAllParents().entrySet())
            {
                for (Subject subject : entry.getValue())
                {
                    transientData.removeParent(entry.getKey(), subject);
                }
            }
        }
        return parents;
    }

    @Override
    public String getIdentifier()
    {
        return uuid.toString();
    }

    @Override
    public Optional<CommandSource> getCommandSource()
    {
        return Sponge.getServer().getPlayer(uuid).map(CommandSource.class::cast);
    }

    public void reload()
    {
        this.getSubjectData().reload();
        this.getTransientSubjectData().clearOptions();
        this.getTransientSubjectData().clearParents();
        this.getTransientSubjectData().clearPermissions();
        applyDefault();
    }

    @Override
    public String toString()
    {
        return "UserSubject: " + this.getIdentifier() + " " + this.getCommandSource().map(CommandSource::getName).orElse("?");
    }
}
