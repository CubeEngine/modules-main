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
package org.cubeengine.module.conomy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;

@SuppressWarnings("all")
@Singleton
public class ConomyPermission extends PermissionContainer
{
    @Inject
    public ConomyPermission(PermissionManager pm)
    {
        super(pm, Conomy.class);
    }

    private final Permission ALLOWUNDERMIN = register("account.user.allow-under-min", "", null);

    private final Permission ACCESS = register("access.other.player", "Grants full access to all player accounts", null);
    public final Permission ACCESS_WITHDRAW = register("withdraw", "Allows transfering money from anothers players account", ACCESS);
    public final Permission ACCESS_SEE = register("seehidden", "Allows seeing hidden player accounts", ACCESS);
}
