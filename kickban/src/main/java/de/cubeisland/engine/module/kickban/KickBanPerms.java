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
package de.cubeisland.engine.module.kickban;

import de.cubeisland.engine.module.service.permission.Permission;
import de.cubeisland.engine.module.service.permission.PermissionContainer;

import static de.cubeisland.engine.module.service.permission.PermDefault.FALSE;

public class KickBanPerms extends PermissionContainer<KickBan>
{
    public KickBanPerms(KickBan module)
    {
        super(module);
        registerAllPermissions();
    }

    public final Permission COMMAND = getBasePerm().childWildcard("command");

    private final Permission COMMAND_KICK = COMMAND.childWildcard("kick");
    public final Permission COMMAND_KICK_ALL = COMMAND_KICK.child("all");
    public final Permission COMMAND_KICK_NOREASON = COMMAND_KICK.newPerm("noreason");

    public final Permission COMMAND_BAN_NOREASON = COMMAND.childWildcard("ban").child("noreason");
    public final Permission COMMAND_IPBAN_NOREASON = COMMAND.childWildcard("ipban").child("noreason", FALSE);
    public final Permission COMMAND_TEMPBAN_NOREASON = COMMAND.childWildcard("tempban").child("noreason",FALSE);

    public final Permission KICK_RECEIVEMESSAGE = getBasePerm().childWildcard("kick").child("receivemessage");
    public final Permission BAN_RECEIVEMESSAGE = getBasePerm().childWildcard("ban").child("receivemessage");

}
