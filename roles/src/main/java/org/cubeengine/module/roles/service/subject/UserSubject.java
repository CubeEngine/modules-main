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

import java.util.Optional;
import java.util.UUID;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.data.UserSubjectData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

public class UserSubject extends BaseSubject<UserSubjectData>
{
    private final UUID uuid;
    private final UserSubjectData data;

    public UserSubject(RolesPermissionService service, UUID uuid)
    {
        super(service.userSubjects(), service);
        this.data = new UserSubjectData(service, uuid, this);
        this.uuid = uuid;
    }

    @Override
    public UserSubjectData subjectData()
    {
        return data;
    }

    @Override
    public String identifier()
    {
        return uuid.toString();
    }

    @Override
    public Optional<String> friendlyIdentifier()
    {
        return Sponge.server().gameProfileManager().uncached().profile(uuid).join().name();
    }

    public Optional<ServerPlayer> player()
    {
        return Sponge.server().player(uuid);
    }

    @Override
    public Optional<?> associatedObject()
    {
        return this.player();
    }

    public void reload()
    {
        this.subjectData().reload();
        this.transientSubjectData().clearOptions();
        this.transientSubjectData().clearParents();
        this.transientSubjectData().clearPermissions();
    }

    @Override
    public boolean isSubjectDataPersisted()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "UserSubject: " + this.identifier() + " " + this.player().map(ServerPlayer::name).orElse("?");
    }
}
