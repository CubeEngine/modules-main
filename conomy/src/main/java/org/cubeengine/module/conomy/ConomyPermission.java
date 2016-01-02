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
package org.cubeengine.module.conomy;

import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

@SuppressWarnings("all")
public class ConomyPermission extends PermissionContainer<Conomy>
{
    public ConomyPermission(Conomy module)
    {
        super(module);
    }

    private final PermissionDescription ALLOWUNDERMIN = register("account.user.allow-under-min", "", null);
    private final PermissionDescription COMMAND = register("command", "", null);
    public final PermissionDescription COMMAND_PAY_ASOTHER = register("money.pay.as-other", "Allows transfering money from anothers players account", COMMAND);
}
