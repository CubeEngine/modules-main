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
package org.cubeengine.module.roles.sponge.subject;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.data.UserSubjectData;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.command.CommandSource;

public class UserSubject extends BaseSubject<UserSubjectData>
{
    private Game game;
    private User user;
    private final UUID uuid;

    public UserSubject(Game game, RolesPermissionService service, UUID uuid)
    {
        super(service.getUserSubjects(), service, new UserSubjectData(service, uuid));
        this.game = game;
        this.uuid = uuid;

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
    public String getIdentifier()
    {
        return uuid.toString();
    }

    @Override
    public Optional<CommandSource> getCommandSource()
    {
        return getUser().getPlayer().map(CommandSource.class::cast);
    }

    public User getUser()
    {
        if (user == null)
        {
            Optional<Player> player = game.getServer().getPlayer(uuid);
            user = player.map(User.class::cast).orElse(game.getServiceManager().provideUnchecked(UserStorageService.class).get(uuid).orElse(null));
        }
        return user;
    }
}
