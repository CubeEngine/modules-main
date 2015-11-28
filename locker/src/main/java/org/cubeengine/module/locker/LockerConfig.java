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
package org.cubeengine.module.locker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Transient;
import org.cubeengine.service.world.ConfigWorld;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;

import static org.cubeengine.module.locker.storage.LockType.PRIVATE;
import static org.cubeengine.module.locker.storage.ProtectionFlag.*;
import static org.cubeengine.module.locker.storage.ProtectionFlag.BLOCK_REDSTONE;
import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.entity.EntityTypes.*;

@SuppressWarnings("all")
public class LockerConfig extends ReflectedYaml
{
    @Name("settings.open-iron-door-with-click")
    public boolean openIronDoorWithClick = false;

    @Name("settings.protect.entity.living-from-damage")
    public boolean protectEntityFromDamage = true;

    @Comment("If set to true protected living entities won't receive damage from environment in addition to damage done by players")
    @Name("settings.protect.entity.living-from-damage-by-environment")
    public boolean protectEntityFromEnvironementalDamage = true;

    @Name("settings.protect.entity.vehicle.from-break")
    public boolean protectVehicleFromBreak = true;

    @Comment("If set to true protected vehicles won't break when receiving damage from environment in addition to the player-protection")
    @Name("settings.protect.entity.vehicle.from-break-by-environment")
    public boolean protectVehicleFromEnvironmental = true;

    @Name("settings.protect.blocks.from-water-and-lava")
    public boolean protectBlocksFromWaterLava = true;

    @Name("settings.protect.blocks.from-explosion")
    public boolean protectBlockFromExplosion = true;

    @Name("settings.protect.blocks.from-fire")
    public boolean protectBlockFromFire = true;

    @Name("settings.protect.blocks.from-rightclick")
    public boolean protectBlockFromRClick = true;

    @Name("settings.protect.entity.from-rightclick")
    public boolean protectEntityFromRClick = true;

    @Name("settings.protect.blocks.from-break")
    public boolean protectFromBlockBreak = true;

    @Name("settings.protect.blocks.from-pistonmove")
    public boolean protectFromPistonMove = true;

    @Name("settings.protect.blocks.from-redstone")
    public boolean protectFromRedstone = true;

    @Comment("Protection from hoppers can potentially cause lag.\n" +
                 "Set to true if you are experiencing lag because of protection from hoppers")
    @Name("settings.disable-hopper-protection")
    public boolean noProtectFromHopper = false;

    @Name("settings.protect.only-when-online")
    public boolean protectWhenOnlyOnline = false;
    @Name("settings.protect.only-when-offline")
    public boolean protectWhenOnlyOffline = false;

    @Comment("If set to true protected doors will auto-close after the configured time")
    @Name("settings.auto-close.enable")
    public boolean autoCloseEnable = true;

    @Comment("Doors will auto-close after this set amount of seconds.")
    @Name("settings.auto-close.time")
    public int autoCloseSeconds = 3;

    @Name("settings.key-books.allow-single")
    public boolean allowKeyBooks = true;

    @Comment("A List of all blocks that can be protected with Locker\n" +
                 "use the auto-protect option to automatically create a protection when placing the block\n" +
                 "additionally you can set default flags which will also be automatically applied")
    @Name("protections.blocks")
    public List<BlockLockConfig> blockprotections;

    @Comment("Set this to false if you wish to disable EntityProtection completely")
    @Name("protections.entities-enable")
    public boolean protEntityEnable = true;

    @Comment("A list of all entities that can be protected with Locker\n" +
                 "auto-protect only applies onto entities that can be tamed")
    @Name("protections.entities")
    public List<EntityLockConfig> entityProtections;

    @Comment("Worlds to disable auto-protect in")
    public List<ConfigWorld> disableAutoProtect = new ArrayList<>();

    // limit protection count#

    @Override
    public void onLoaded(File loadFrom)
    {
        if (blockprotections == null || blockprotections.isEmpty())
        {
            blockprotections = new ArrayList<>();
            blockprotections.add(new BlockLockConfig(CHEST).autoProtect(PRIVATE));
            blockprotections.add(new BlockLockConfig(TRAPPED_CHEST).autoProtect(PRIVATE));
            blockprotections.add(new BlockLockConfig(FURNACE));
            blockprotections.add(new BlockLockConfig(LIT_FURNACE));
            blockprotections.add(new BlockLockConfig(BREWING_STAND));
            blockprotections.add(new BlockLockConfig(DISPENSER).defaultFlags(BLOCK_REDSTONE));
            blockprotections.add(new BlockLockConfig(DROPPER).defaultFlags(BLOCK_REDSTONE));
            blockprotections.add(new BlockLockConfig(STANDING_SIGN));
            blockprotections.add(new BlockLockConfig(WALL_SIGN));
            blockprotections.add(new BlockLockConfig(WOODEN_DOOR).defaultFlags(BLOCK_REDSTONE, AUTOCLOSE));
            blockprotections.add(new BlockLockConfig(IRON_DOOR));
            blockprotections.add(new BlockLockConfig(TRAPDOOR).defaultFlags(BLOCK_REDSTONE, AUTOCLOSE));
            blockprotections.add(new BlockLockConfig(FENCE_GATE).defaultFlags(AUTOCLOSE));
            blockprotections.add(new BlockLockConfig(HOPPER).defaultFlags(HOPPER_IN, HOPPER_OUT));
        }
        if (protEntityEnable && (entityProtections == null || entityProtections.isEmpty()))
        {
            entityProtections = new ArrayList<>();
            entityProtections.add(new EntityLockConfig(HORSE).autoProtect(PRIVATE));
            entityProtections.add(new EntityLockConfig(CHESTED_MINECART));
            entityProtections.add(new EntityLockConfig(HOPPER_MINECART));
        }

        if (this.protectWhenOnlyOffline && this.protectWhenOnlyOnline)
        {
            throw new IllegalArgumentException("Invalid Configuration! Cannot protect only when offline AND only when online");
        }
        detachableEntityCount = -1;
    }

    @Transient
    private int detachableEntityCount = -1;

    public boolean protectsDetachableEntities()
    {
        if (detachableEntityCount == -1)
        {
            for (EntityLockConfig entityProtection : entityProtections)
            {
                if (entityProtection.type.equals(LEASH_HITCH)
                    || entityProtection.type.equals(PAINTING)
                    || entityProtection.type.equals(ITEM_FRAME))
                {
                    detachableEntityCount++;
                }
            }
        }
        return detachableEntityCount != 0;
    }
}

