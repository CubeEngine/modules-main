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

import org.cubeengine.reflect.annotations.Name;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;

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

