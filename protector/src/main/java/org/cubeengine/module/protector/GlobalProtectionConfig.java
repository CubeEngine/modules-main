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
package org.cubeengine.module.protector;

import static org.cubeengine.module.locker.data.LockType.PRIVATE;
import static org.cubeengine.module.locker.data.ProtectionFlag.AUTOCLOSE;
import static org.cubeengine.module.locker.data.ProtectionFlag.BLOCK_REDSTONE;
import static org.cubeengine.module.locker.data.ProtectionFlag.HOPPER_IN;
import static org.cubeengine.module.locker.data.ProtectionFlag.HOPPER_OUT;
import static org.spongepowered.api.block.BlockTypes.ACACIA_DOOR;
import static org.spongepowered.api.block.BlockTypes.ACACIA_FENCE_GATE;
import static org.spongepowered.api.block.BlockTypes.BIRCH_DOOR;
import static org.spongepowered.api.block.BlockTypes.BIRCH_FENCE_GATE;
import static org.spongepowered.api.block.BlockTypes.BREWING_STAND;
import static org.spongepowered.api.block.BlockTypes.CHEST;
import static org.spongepowered.api.block.BlockTypes.DARK_OAK_DOOR;
import static org.spongepowered.api.block.BlockTypes.DARK_OAK_FENCE_GATE;
import static org.spongepowered.api.block.BlockTypes.DISPENSER;
import static org.spongepowered.api.block.BlockTypes.DROPPER;
import static org.spongepowered.api.block.BlockTypes.FURNACE;
import static org.spongepowered.api.block.BlockTypes.HOPPER;
import static org.spongepowered.api.block.BlockTypes.IRON_DOOR;
import static org.spongepowered.api.block.BlockTypes.IRON_TRAPDOOR;
import static org.spongepowered.api.block.BlockTypes.JUNGLE_DOOR;
import static org.spongepowered.api.block.BlockTypes.JUNGLE_FENCE_GATE;
import static org.spongepowered.api.block.BlockTypes.OAK_DOOR;
import static org.spongepowered.api.block.BlockTypes.OAK_SIGN;
import static org.spongepowered.api.block.BlockTypes.SPRUCE_FENCE_GATE;
import static org.spongepowered.api.block.BlockTypes.TRAPPED_CHEST;
import static org.spongepowered.api.entity.EntityTypes.HOPPER_MINECART;
import static org.spongepowered.api.entity.EntityTypes.HORSE;
import static org.spongepowered.api.entity.EntityTypes.ITEM_FRAME;
import static org.spongepowered.api.entity.EntityTypes.PAINTING;

import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.annotations.Name;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public class GlobalProtectionConfig extends ReflectedYaml
{

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


}

