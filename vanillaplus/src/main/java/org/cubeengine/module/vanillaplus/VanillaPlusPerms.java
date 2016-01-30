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

import de.cubeisland.engine.service.permission.Permission;
import org.cubeengine.service.permission.PermissionContainer;

import static de.cubeisland.engine.service.permission.PermDefault.FALSE;

@SuppressWarnings("all")
public class VanillaPlusPerms extends PermissionContainer<VanillaPlus>
{
    public VanillaPlusPerms(VanillaPlus module)
    {
        super(module);
        this.registerAllPermissions();
    }

    public final Permission COMMAND = getBasePerm().childWildcard("command");

    private final Permission COMMAND_CLEARINVENTORY = COMMAND.childWildcard("clearinventory");
    /**
     * Allows clearing the inventory of other players
     */
    public final Permission COMMAND_CLEARINVENTORY_OTHER = COMMAND_CLEARINVENTORY.child("notify");
    /**
     * Notifies you if your inventory got cleared by someone else
     */
    public final Permission COMMAND_CLEARINVENTORY_NOTIFY = COMMAND_CLEARINVENTORY.child("other");
    /**
     * Prevents the other player being notified when his inventory got cleared
     */
    public final Permission COMMAND_CLEARINVENTORY_QUIET = COMMAND_CLEARINVENTORY.child("quiet");
    /**
     * Prevents your inventory from being cleared unless forced
     */
    public final Permission COMMAND_CLEARINVENTORY_PREVENT = COMMAND_CLEARINVENTORY.newPerm("prevent", FALSE);
    /**
     * Clears an inventory even if the player has the prevent permission
     */
    public final Permission COMMAND_CLEARINVENTORY_FORCE = COMMAND_CLEARINVENTORY.child("force");

    private final Permission COMMAND_GAMEMODE = COMMAND.childWildcard("gamemode");
    /**
     * Allows to change the game-mode of other players too
     */
    public final Permission COMMAND_GAMEMODE_OTHER = COMMAND_GAMEMODE.child("other");
    /**
     * Without this permission the players game-mode will be reset when leaving the server or changing the world
     */
    public final Permission COMMAND_GAMEMODE_KEEP = COMMAND_GAMEMODE.child("keep");


    private final Permission COMMAND_KILL = COMMAND.childWildcard("kill");
    /**
     * Prevents from being killed by the kill command unless forced
     */
    public final Permission COMMAND_KILL_PREVENT = COMMAND_KILL.newPerm("prevent", FALSE);
    /**
     * Kills a player even if the player has the prevent permission
     */
    public final Permission COMMAND_KILL_FORCE = COMMAND_KILL.child("force");
    /**
     * Allows killing all players currently online
     */
    public final Permission COMMAND_KILL_ALL = COMMAND_KILL.child("all");
    /**
     * Allows killing a player with a lightning strike
     */
    public final Permission COMMAND_KILL_LIGHTNING = COMMAND_KILL.child("lightning");
    /**
     * Prevents the other player being notified who killed him
     */
    public final Permission COMMAND_KILL_QUIET = COMMAND_KILL.child("quiet");
    /**
     * Shows who killed you
     */
    public final Permission COMMAND_KILL_NOTIFY = COMMAND_KILL.child("notify");


    /**
     * Allows to create items that are blacklisted
     */
    public final Permission ITEM_BLACKLIST = getBasePerm().child("item-blacklist");

    private final Permission COMMAND_ITEM = COMMAND.childWildcard("item");
    public final Permission COMMAND_ITEM_ENCHANTMENTS = COMMAND_ITEM.child("enchantments");
    public final Permission COMMAND_ITEM_ENCHANTMENTS_UNSAFE = COMMAND_ITEM.child("enchantments.unsafe");

    public final Permission COMMAND_STACK_FULLSTACK = COMMAND.childWildcard("stack").child("fullstack");

    public final Permission COMMAND_PTIME_OTHER = COMMAND.child("ptime.other");

    public final PermissionDescription SPAM = register("spam", "Prevents getting kicked for the Vanilla Spam Reason", null);

    public final Permission SIGN_COLORED = getBasePerm().childWildcard("sign").child("colored");
    public final Permission SIGN_STYLED = getBasePerm().childWildcard("sign").child("styled");
}
