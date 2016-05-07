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
package org.cubeengine.module.kickban;

import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

public class KickBanPerms extends PermissionContainer<KickBan>
{
    public KickBanPerms(KickBan module)
    {
        super(module);
    }

    private final Permission COMMAND = register("command", "Base Commands Permission", null);

    public final Permission COMMAND_KICK_ALL = register("kick.all", "Allows kicking all players", COMMAND);
    public final Permission COMMAND_KICK_NOREASON = register("kick.noreason", "Allows kicking without providing a reason", COMMAND);

    public final Permission COMMAND_BAN_NOREASON = register("ban.noreason", "Allows banning without providing a reason",COMMAND);
    public final Permission COMMAND_IPBAN_NOREASON = register("ipban.noreason", "Allows banning without providing a reason",COMMAND);
    public final Permission COMMAND_TEMPBAN_NOREASON = register("tempban.noreason", "Allows banning without providing a reason",COMMAND);

    public final Permission KICK_RECEIVEMESSAGE = register("kick.receivemessage", "Enables notification when a player gets kicked", COMMAND);
    public final Permission BAN_RECEIVEMESSAGE = register("ban.receivemessage", "Enables notification when a player gets banned", COMMAND);
}
