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
package org.cubeengine.module.locker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;

@SuppressWarnings("all")
@Singleton
public class ProtectPerm extends PermissionContainer
{
    @Inject
    public ProtectPerm(PermissionManager pm)
    {
        super(pm, Locker.class);
    }

    public final Permission ALLOW_INVENTORY = register("allow.container", "Allows interaction with inventories");
    public final Permission ALLOW_DOOR = register("allow.door", "", null);
    public final Permission ALLOW_LIVING_ENTITY = register("allow.living-entity", "Allows interaction with living entities");
    public final Permission ALLOW_ENTITY = register("allow.entity", "Allows interaction with non living entities");
    public final Permission ALLOW_HANGING = register("allow.hanging", "Allows interaction with hanging entities");
    public final Permission ALLOW_VEHICLE = register("allow.vehicle", "Allows interaction with vehicles");

}
