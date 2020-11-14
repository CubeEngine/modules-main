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
package org.cubeengine.module.locker.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.service.matcher.StringMatcher;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.libcube.util.StringUtils.startsWithIgnoreCase;

/**
 * Flags that can be given to a protection.
 * <p>Flags may not be supported by all {@link ProtectedType}
 */
public enum ProtectionFlag
{

    NOTIFY_ACCESS("Notify", 0),
    // mostly Block related flags:
    // Player: Right-Click on block this includes opening inventories
    BLOCK_INTERACT("Interact", 1),
    // Player: Taking items out of an inventory
    INVENTORY_TAKE("Take",2),
    // Player: Putting items into an inventory
    INVENTORY_PUT("Put",3),
    // Hopper/Minecart: Taking items out of an inventory
    INVENTORY_HOPPER_TAKE("Hopper Take", 4),
    // Hopper/Minecart: Puttings items into an inventory
    INVENTORY_HOPPER_PUT("Hopper Put", 5),
    // Player: Breaking a block
    BLOCK_BREAK("Break",  6),
    // Explosion: Breaking a block
    BLOCK_EXPLOSION("Explosion", 7),
    // Redstone: Activating a bloock
    BLOCK_REDSTONE("Redstone", 8), // e.g. HOPPER/DISPENSER/DROPPER
    //  Entity related flags:
    ENTITY_INTERACT("Interact", 9),
    // Player: Damaging/Breaking an entity
    ENTITY_DAMAGE("Damage", 10),
    // Environment: Damaging/Breaking an entity
    ENTITY_DAMAGE_ENVIRONMENT("Environment", 11),

    // Only for access
    ADMIN("admin", 12),
    ;
    public static final int NONE = 0;
    public static final int FULL = 0b111111111110;
    public static final int ALL = 0b111111111111;

    public final int flagValue;
    public final String flagname;

    ProtectionFlag(String flagname, int shift)
    {
        this.flagname = flagname;
        this.flagValue = 1 << shift;
    }

    public boolean isSet(int bitMask)
    {
        return (bitMask & this.flagValue) == this.flagValue;
    }


    public boolean isBlocked(int blockMask, int accessMask)
    {
        return this.isSet(blockMask) && !this.isSet(accessMask);
    }
}
