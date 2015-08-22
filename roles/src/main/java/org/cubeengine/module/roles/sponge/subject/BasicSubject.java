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

import java.util.Collections;
import java.util.Set;
import com.google.common.base.Optional;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.data.BaseSubjectData;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.command.CommandSource;

public class BasicSubject extends BaseSubject<BaseSubjectData>
{
    private String identifier;
    private Game game;

    public BasicSubject(String identifier, SubjectCollection collection, RolesPermissionService service, Game game)
    {
        super(collection, service, new BaseSubjectData());
        this.identifier = identifier;
        this.game = game;
    }

    @Override
    public String getIdentifier()
    {
        return identifier;
    }

    @Override
    public Optional<CommandSource> getCommandSource()
    {
        if (identifier.equals("Server"))
        {
            return Optional.of(game.getServer().getConsole());
        }
        if (identifier.equals("RCON"))
        {

        }
        return Optional.absent();
    }

    @Override
    public Set<Context> getActiveContexts()
    {
        if (getContainingCollection().getIdentifier().equals(PermissionService.SUBJECTS_ROLE_TEMPLATE))
        {
            return Collections.emptySet();
        }
        return super.getActiveContexts();
    }
}
