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
package de.cubeisland.engine.module.roles.sponge.subject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import com.google.common.base.Optional;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.data.UserSubjectData;
import org.spongepowered.api.entity.player.User;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.context.ContextCalculator;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;

public class UserSubject extends BaseSubject
{
    private final UserSubjectData data;
    private RolesPermissionService service;
    private User user;
    private final UUID uuid;

    public UserSubject(RolesPermissionService service, User user)
    {
        super(service.getUserSubjects());
        this.service = service;
        this.user = user;
        this.uuid = user.getUniqueId();
        this.data = new UserSubjectData(service, uuid);
    }

    public UserSubject(RolesPermissionService service, UUID uuid)
    {
        super(service.getUserSubjects());
        this.service = service;
        this.data = new UserSubjectData(service, uuid);
        this.uuid = uuid;

        // TODO lazy fetch call getUser in separate thread

        SubjectData defaultData = service.getDefaultData();
        OptionSubjectData transientData = getTransientSubjectData();
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
    public OptionSubjectData getSubjectData()
    {
        return data;
    }

    @Override
    public String getIdentifier()
    {
        return uuid.toString();
    }

    @Override
    public Optional<CommandSource> getCommandSource()
    {
        return getUser().getPlayer().transform(e -> (CommandSource)e);
    }

    @Override
    public Set<Context> getActiveContexts()
    {
        Set<Context> contexts = new HashSet<>();
        for (ContextCalculator calculator : service.getContextCalculators())
        {
            calculator.accumulateContexts(getUser(), contexts);
        }
        return contexts;
    }

    public User getUser()
    {
        if (user == null)
        {
            // TODO create user
        }
        return user;
    }
}
