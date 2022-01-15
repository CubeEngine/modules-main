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
package org.cubeengine.module.locker.old.door;

import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.annotations.Name;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;

public class DoorConfig extends ReflectedYaml
{
    @Name("settings.open-iron-door-with-click")
    public boolean openIronDoorWithClick = false;

    @Comment("If set to true protected doors will auto-close after the configured time")
    @Name("settings.auto-close.enable")
    public boolean autoCloseEnable = true;

    @Comment("Doors will auto-close after this set amount of seconds.")
    @Name("settings.auto-close.time")
    public int autoCloseSeconds = 3;

}
