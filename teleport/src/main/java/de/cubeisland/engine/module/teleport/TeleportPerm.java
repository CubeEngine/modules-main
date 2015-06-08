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
package de.cubeisland.engine.module.teleport;

import de.cubeisland.engine.module.service.permission.Permission;
import de.cubeisland.engine.module.service.permission.PermissionContainer;

import static de.cubeisland.engine.module.service.permission.PermDefault.FALSE;

public class TeleportPerm extends PermissionContainer<Teleport>
{
    public TeleportPerm(Teleport module)
    {
        super(module);
    }

    public final Permission COMMAND = getBasePerm().childWildcard("command");

    private final Permission COMMAND_SPAWN = COMMAND.childWildcard("spawn");
    /**
     * Prevents from being teleported to spawn by someone else
     */
    public final Permission COMMAND_SPAWN_PREVENT = COMMAND_SPAWN.child("prevent");
    /**
     * Allows teleporting a player to spawn even if the player has the prevent permission
     */
    public final Permission COMMAND_SPAWN_FORCE = COMMAND_SPAWN.child("force");

    private final Permission COMMAND_TP = COMMAND.childWildcard("tp");
    /**
     * Ignores all prevent permissions when using the /tp command
     */
    public final Permission COMMAND_TP_FORCE = COMMAND_TP.child("force");
    /**
     * Allows teleporting another player
     */
    public final Permission COMMAND_TP_OTHER = COMMAND_TP.child("other");

    public final Permission COMMAND_TPPOS_UNSAFE = COMMAND.childWildcard("tppos").child("unsafe");

    private final Permission TELEPORT = getBasePerm().childWildcard("teleport");
    private final Permission TELEPORT_PREVENT = TELEPORT.newWildcard("prevent");
    /**
     * Prevents from being teleported by someone else
     */
    public final Permission TELEPORT_PREVENT_TP = TELEPORT_PREVENT.child("tp", FALSE);
    /**
     * Prevents from teleporting to you
     */
    public final Permission TELEPORT_PREVENT_TPTO = TELEPORT_PREVENT.child("tpto", FALSE);

    private final Permission COMMAND_TPALL = COMMAND.childWildcard("tpall");
    /**
     * Ignores all prevent permissions when using the /tpall command
     */
    public final Permission COMMAND_TPALL_FORCE = COMMAND_TPALL.child("force");

    private final Permission COMMAND_TPHERE = COMMAND.childWildcard("tphere");
    /**
     * Ignores all prevent permissions when using the /tphere command
     */
    public final Permission COMMAND_TPHERE_FORCE = COMMAND_TPHERE.child("force");

    private final Permission COMMAND_TPHEREALL = COMMAND.childWildcard("tphereall");
    /**
     * Ignores all prevent permissions when using the /tphereall command
     */
    public final Permission COMMAND_TPHEREALL_FORCE = COMMAND_TPHEREALL.child("force");

    private final Permission COMMAND_BACK = COMMAND.childWildcard("back");

    /**
     * Allows using the back command
     */
    public final Permission COMMAND_BACK_USE = COMMAND_BACK.child("use");
    /**
     * Allows using the back command after dieing (if this is not set you won't be able to tp back to your deathpoint)
     */
    public final Permission COMMAND_BACK_ONDEATH = COMMAND_BACK.child("ondeath");

    private final Permission COMPASS_JUMPTO = getBasePerm().childWildcard("compass").childWildcard("jumpto");
    public final Permission COMPASS_JUMPTO_LEFT = COMPASS_JUMPTO.child("left");
    public final Permission COMPASS_JUMPTO_RIGHT = COMPASS_JUMPTO.child("right");

}
