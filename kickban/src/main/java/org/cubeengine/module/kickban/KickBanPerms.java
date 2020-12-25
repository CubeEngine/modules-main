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
package org.cubeengine.module.kickban;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;

@Singleton
public class KickBanPerms extends PermissionContainer
{
    @Inject
    public KickBanPerms(PermissionManager pm)
    {
        super(pm, KickBan.class);
    }

    public final Permission COMMAND_KICK_ALL = register("command.kick.all", "Allows kicking all players", null);
    public final Permission COMMAND_KICK_NOREASON = register("command.kick.noreason", "Allows kicking without providing a reason", null);

    public final Permission COMMAND_BAN_NOREASON = register("command.ban.noreason", "Allows banning without providing a reason", null);
    public final Permission COMMAND_IPBAN_NOREASON = register("command.ipban.noreason", "Allows banning without providing a reason", null);
    public final Permission COMMAND_TEMPBAN_NOREASON = register("command.tempban.noreason", "Allows banning without providing a reason",null);

    public final Permission KICK_RECEIVEMESSAGE = register("command.kick.notify", "Enables notification when a player gets kicked", null);
    public final Permission BAN_RECEIVEMESSAGE = register("command.ban.notify", "Enables notification when a player gets banned", null);
}
