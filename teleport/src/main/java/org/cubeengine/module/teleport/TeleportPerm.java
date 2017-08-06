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
package org.cubeengine.module.teleport;

import javax.inject.Inject;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.spongepowered.api.service.permission.PermissionDescription;

public class TeleportPerm extends PermissionContainer
{
    @Inject
    public TeleportPerm(PermissionManager pm)
    {
        super(pm, Teleport.class);
    }

    public final Permission CMD_SPAWN_PREVENT = register("command.spawn.prevent", "Prevents from being teleported to spawn by someone else", null);
    public final Permission CMD_SPAWN_FORCE = register("command.spawn.force", "Allows teleporting a player to spawn even if the player has the prevent permission", null);
    public final Permission COMMAND_TP_FORCE = register("command.tp.force", "Ignores all prevent permissions when using the /tp command", null);
    public final Permission COMMAND_TP_OTHER = register("command.tp.other", "Allows teleporting another player", null);

    public final Permission TELEPORT_PREVENT_TP = register("teleport.prevent.tp", "Prevents from being teleported by someone else", null);
    public final Permission TELEPORT_PREVENT_TPTO = register("teleport.prevent.tpto", "Prevents from teleporting to you", null);

    public final Permission COMMAND_TPALL_FORCE = register("command.tpall.force", "Ignores all prevent permissions when using the /tpall command", null);
    public final Permission COMMAND_TPHERE_FORCE = register("command.tphere.force", "Ignores all prevent permissions when using the /tphere command", null);
    public final Permission COMMAND_TPHEREALL_FORCE = register("command.tphereall.force", "Ignores all prevent permissions when using the /tphereall command", null);
    public final Permission COMMAND_BACK_USE = register("command.back.use", "Allows using the back command", null);
    public final Permission COMMAND_BACK_ONDEATH = register("command.back.ondeath", "Allows using the back command after dieing (if this is not set you won't be able to tp back to your deathpoint)", null);

    public final Permission COMPASS_JUMPTO_LEFT = register("compass.jumpto.left", "", null);
    public final Permission COMPASS_JUMPTO_RIGHT = register("compass.jumpto.right", "", null);

    public final Permission COMMAND_TPPOS_UNSAFE = register("command.tppos.unsafe", "", null);

}
