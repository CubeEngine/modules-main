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
package org.cubeengine.module.basics;

import de.cubeisland.engine.service.permission.Permission;
import org.cubeengine.service.permission.PermissionContainer;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.world.WorldManager;

import static de.cubeisland.engine.service.permission.PermDefault.FALSE;

@SuppressWarnings("all")
public class BasicsPerm extends PermissionContainer<Basics>
{
    public BasicsPerm(Basics module, WorldManager wm, PermissionManager pm)
    {
        super(module);
        this.registerAllPermissions();
    }

    public final Permission COMMAND = getBasePerm().childWildcard("command");

    public final Permission COMMAND_ENCHANT_UNSAFE = COMMAND.childWildcard("enchant").child("unsafe");

    public final Permission COMMAND_LAG_RESET = COMMAND.childWildcard("lag").child("reset");



    private final Permission COMMAND_INVSEE = COMMAND.childWildcard("invsee");
    /**
     * Allows to modify the inventory of other players
     */
    public final Permission COMMAND_INVSEE_MODIFY = COMMAND_INVSEE.child("modify");
    public final Permission COMMAND_INVSEE_ENDERCHEST = COMMAND_INVSEE.child("ender");
    /**
     * Prevents an inventory from being modified unless forced
     */
    public final Permission COMMAND_INVSEE_MODIFY_PREVENT = COMMAND_INVSEE.newPerm("modify.prevent", FALSE);
    /**
     * Allows modifying an inventory even if the player has the prevent permission
     */
    public final Permission COMMAND_INVSEE_MODIFY_FORCE = COMMAND_INVSEE.child("modify.force");
    /**
     * Notifies you when someone is looking into your inventory
     */
    public final Permission COMMAND_INVSEE_NOTIFY = COMMAND_INVSEE.child("notify");
    /**
     * Prevents the other player from being notified when looking into his inventory
     */
    public final Permission COMMAND_INVSEE_QUIET = COMMAND_INVSEE.child("quiet");

    private final Permission COMMAND_GOD = COMMAND.childWildcard("god");
    /**
     * Allows to enable god-mode for other players
     */
    public final Permission COMMAND_GOD_OTHER = COMMAND_GOD.child("other");
    /**
     * Without this permission the player will loose god-mode leaving the server or changing the world
     */

    private final Permission COMMAND_BUTCHER = COMMAND.childWildcard("butcher");
    private final Permission COMMAND_BUTCHER_FLAG = COMMAND_BUTCHER.childWildcard("flag");
    public final Permission COMMAND_BUTCHER_FLAG_PET = COMMAND_BUTCHER_FLAG.child("pet");
    public final Permission COMMAND_BUTCHER_FLAG_ANIMAL = COMMAND_BUTCHER_FLAG.child("animal");
    public final Permission COMMAND_BUTCHER_FLAG_LIGHTNING = COMMAND_BUTCHER_FLAG.child("lightning");
    public final Permission COMMAND_BUTCHER_FLAG_GOLEM = COMMAND_BUTCHER_FLAG.child("golem");
    public final Permission COMMAND_BUTCHER_FLAG_ALLTYPE = COMMAND_BUTCHER_FLAG.child("alltype");
    public final Permission COMMAND_BUTCHER_FLAG_ALL = COMMAND_BUTCHER_FLAG.child("all");
    public final Permission COMMAND_BUTCHER_FLAG_OTHER = COMMAND_BUTCHER_FLAG.child("other");
    public final Permission COMMAND_BUTCHER_FLAG_NPC = COMMAND_BUTCHER_FLAG.child("npc");
    public final Permission COMMAND_BUTCHER_FLAG_MONSTER = COMMAND_BUTCHER_FLAG.child("monster");
    public final Permission COMMAND_BUTCHER_FLAG_BOSS = COMMAND_BUTCHER_FLAG.child("boss");

    private final Permission COMMAND_FEED = COMMAND.childWildcard("feed");
    public final Permission COMMAND_FEED_OTHER = COMMAND_FEED.child("other");

    private final Permission COMMAND_STARVE = COMMAND.childWildcard("starve");
    public final Permission COMMAND_STARVE_OTHER = COMMAND_STARVE.child("other");

    private final Permission COMMAND_HEAL = COMMAND.childWildcard("heal");
    public final Permission COMMAND_HEAL_OTHER = COMMAND_HEAL.child("other");

    private final Permission COMMAND_FLY = COMMAND.childWildcard("fly");
    public final Permission COMMAND_FLY_KEEP = COMMAND_FLY.child("keep");
    public final Permission COMMAND_FLY_OTHER = COMMAND_FLY.child("other");

    /**
     * Allows to change the walkspeed of other players
     */
    public final Permission COMMAND_WALKSPEED_OTHER = COMMAND.childWildcard("walkspeed").child("other");

    /**
     * Allows writing colored signs
     */

    // TODO maybe permissions for obfuscated format?

    public final Permission CHANGEPAINTING = getBasePerm().child("changepainting");


    public final Permission OVERSTACKED_ANVIL_AND_BREWING = getBasePerm().child("allow-overstacked-anvil-and-brewing");


}
