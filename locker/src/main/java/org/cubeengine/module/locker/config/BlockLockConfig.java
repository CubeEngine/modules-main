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
package org.cubeengine.module.locker.config;

import org.cubeengine.converter.ConversionException;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.locker.storage.ProtectedType;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.spongepowered.api.block.BlockType;

/**
 * Example:
 * B_DOOR:
 *   auto-protect: PRIVATE
 *   flags:
 *      - BLOCK_REDSTONE
 *      - AUTOCLOSE
 */
public class BlockLockConfig extends LockConfig<BlockLockConfig, BlockType>
{
    public BlockLockConfig(BlockType material)
    {
        super(ProtectedType.getProtectedType(material));
        this.type = material;
    }

    public String getTitle()
    {
        return type.getId();
    }

    public static class BlockLockerConfigConverter extends LockConfigConverter<BlockLockConfig>
    {
        private MaterialMatcher mm;

        public BlockLockerConfigConverter(Log logger, MaterialMatcher mm)
        {
            super(logger);
            this.mm = mm;
        }

        protected BlockLockConfig fromString(String s) throws ConversionException
        {
            BlockType material = mm.block(s);
            if (material == null)
            {
                throw ConversionException.of(this, s, "Invalid BlockType!");
            }
            return new BlockLockConfig(material);
        }
    }
}
