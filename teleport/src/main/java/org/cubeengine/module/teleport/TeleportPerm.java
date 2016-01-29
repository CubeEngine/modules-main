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
package org.cubeengine.module.teleport;

import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

public class TeleportPerm extends PermissionContainer<Teleport>
{
    public TeleportPerm(Teleport module)
    {
        super(module);
    }

    final PermissionDescription COMMAND = register("command", "", null);

    public final PermissionDescription CMD_SPAWN_PREVENT = register("spawn.prevent",
                                                                    "Prevents from being teleported to spawn by someone else",
                                                                    COMMAND);
    public final PermissionDescription CMD_SPAWN_FORCE = register("spawn.force",
                                                                  "Allows teleporting a player to spawn even if the player has the prevent permission",
                                                                  COMMAND);

    public final PermissionDescription COMMAND_TP_FORCE = register("tp.force",
                                                                   "Ignores all prevent permissions when using the /tp command",
                                                                   COMMAND);
    public final PermissionDescription COMMAND_TP_OTHER = register("tp.other", "Allows teleporting another player",
                                                                   COMMAND);

    public final PermissionDescription TELEPORT_PREVENT_TP = register("teleport.prevent.tp",
                                                                      "Prevents from being teleported by someone else",
                                                                      null);
    public final PermissionDescription TELEPORT_PREVENT_TPTO = register("teleport.prevent.tpto",
                                                                        "Prevents from teleporting to you", null);

    public final PermissionDescription COMMAND_TPALL_FORCE = register("tpall.force",
                                                                      "Ignores all prevent permissions when using the /tpall command",
                                                                      COMMAND);
    public final PermissionDescription COMMAND_TPHERE_FORCE = register("tphere.force",
                                                                       "Ignores all prevent permissions when using the /tphere command",
                                                                       COMMAND);
    public final PermissionDescription COMMAND_TPHEREALL_FORCE = register("tphereall.force",
                                                                          "Ignores all prevent permissions when using the /tphereall command",
                                                                          COMMAND);
    public final PermissionDescription COMMAND_BACK_USE = register("back.use", "Allows using the back command", COMMAND);
    public final PermissionDescription COMMAND_BACK_ONDEATH = register("back.ondeath",
                                                                       "Allows using the back command after dieing (if this is not set you won't be able to tp back to your deathpoint)",
                                                                       COMMAND);

    public final PermissionDescription COMPASS_JUMPTO_LEFT = register("compass.jumpto.left", "", null);
    public final PermissionDescription COMPASS_JUMPTO_RIGHT = register("right", "", null);

    public final PermissionDescription COMMAND_TPPOS_UNSAFE = register("tppos.unsafe", "", COMMAND);

}
