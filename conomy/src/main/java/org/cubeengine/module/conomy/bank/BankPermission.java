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
package org.cubeengine.module.conomy.bank;

import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

public class BankPermission extends PermissionContainer
{
    public BankPermission(PermissionManager pm)
    {
        super(pm, Conomy.class);
    }

    private final Permission ACCESS = register("access.other.bank", "Grants full access to all banks", null);
    public final Permission ACCESS_MANAGE = register("manage", "Grants manage access to all banks", ACCESS);
    public final Permission ACCESS_WITHDRAW = register("withdraw", "Grants withdraw access to all banks", ACCESS);
    public final Permission ACCESS_DEPOSIT = register("deposit", "Grants deposit access to all banks", ACCESS);
    public final Permission ACCESS_SEE = register("see", "Grants looking at all banks", ACCESS);
}
