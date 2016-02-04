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
package org.cubeengine.module.vanillaplus;

import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

@SuppressWarnings("all")
public class VanillaPlusPerms extends PermissionContainer<VanillaPlus>
{
    public VanillaPlusPerms(VanillaPlus module)
    {
        super(module);
    }



    public final PermissionDescription COMMAND_PTIME_OTHER = COMMAND.child("ptime.other");

    public final PermissionDescription SPAM = register("spam", "Prevents getting kicked for the Vanilla Spam Reason",
                                                       null);



    public final PermissionDescription COMMAND = getBasePerm().childWildcard("command");

    public final PermissionDescription COMMAND_LAG_RESET = COMMAND.childWildcard("lag").child("reset");


    /**
     * Without this PermissionDescription the player will loose god-mode leaving the server or changing the world
     */

    private final PermissionDescription COMMAND_BUTCHER = COMMAND.childWildcard("butcher");
    private final PermissionDescription COMMAND_BUTCHER_FLAG = COMMAND_BUTCHER.childWildcard("flag");
    public final PermissionDescription COMMAND_BUTCHER_FLAG_PET = COMMAND_BUTCHER_FLAG.child("pet");
    public final PermissionDescription COMMAND_BUTCHER_FLAG_ANIMAL = COMMAND_BUTCHER_FLAG.child("animal");
    public final PermissionDescription COMMAND_BUTCHER_FLAG_LIGHTNING = COMMAND_BUTCHER_FLAG.child("lightning");
    public final PermissionDescription COMMAND_BUTCHER_FLAG_GOLEM = COMMAND_BUTCHER_FLAG.child("golem");
    public final PermissionDescription COMMAND_BUTCHER_FLAG_ALLTYPE = COMMAND_BUTCHER_FLAG.child("alltype");
    public final PermissionDescription COMMAND_BUTCHER_FLAG_ALL = COMMAND_BUTCHER_FLAG.child("all");
    public final PermissionDescription COMMAND_BUTCHER_FLAG_OTHER = COMMAND_BUTCHER_FLAG.child("other");
    public final PermissionDescription COMMAND_BUTCHER_FLAG_NPC = COMMAND_BUTCHER_FLAG.child("npc");
    public final PermissionDescription COMMAND_BUTCHER_FLAG_MONSTER = COMMAND_BUTCHER_FLAG.child("monster");
    public final PermissionDescription COMMAND_BUTCHER_FLAG_BOSS = COMMAND_BUTCHER_FLAG.child("boss");

    /**
     * Allows writing colored signs
     */

    // TODO maybe permissions for obfuscated format?



}
