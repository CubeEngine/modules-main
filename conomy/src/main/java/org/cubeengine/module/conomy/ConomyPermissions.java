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
public class ConomyPermissions extends PermissionContainer<Conomy>
{
    public ConomyPermissions(Conomy module)
    {
        super(module);
    }

    //public final PermissionDescription NO_PERM = register("no.perm", "no.description", null);

    public final PermissionDescription ACCESS = register("access-all.bank", "Grants full access to all banks", null);
    public final PermissionDescription ACCESS_MANAGE = register("manage", "Grants manage access to all banks", ACCESS);
    public final PermissionDescription ACCESS_WITHDRAW = register("withdraw", "Grants withdraw access to all banks", ACCESS, ACCESS_MANAGE);
    public final PermissionDescription ACCESS_DEPOSIT = register("deposit", "Grants deposit access to all banks", ACCESS, ACCESS_WITHDRAW);
    public final PermissionDescription ACCESS_SEE = register("see", "Grants looking at all banks", ACCESS, ACCESS_DEPOSIT);

    private final Permission ACCOUNT = getBasePerm().childWildcard("account");
    private final Permission ACCOUNT_USER = ACCOUNT.childWildcard("user");

    public final Permission USER_ALLOWUNDERMIN = ACCOUNT_USER.child("allow-under-min");

    private final PermissionDescription COMMAND = register("command", "", null);
    public final PermissionDescription COMMAND_PAY_ASOTHER = register("money.pay.as-other", "Allows transfering money from anothers players account", COMMAND);

    private final Permission COMMAND_BANK =  COMMAND.childWildcard("bank");
    public final Permission COMMAND_BANK_BALANCE_SHOWHIDDEN = COMMAND_BANK.childWildcard("balance").child("show-hidden");

    public final Permission COMMAND_BANK_LISTINVITES_OTHER = COMMAND_BANK.childWildcard("listinvites").child("force");

    private final Permission COMMAND_BANK_JOIN = COMMAND_BANK.childWildcard("join");

    public final Permission COMMAND_BANK_JOIN_OTHER = COMMAND_BANK_JOIN.child("other");
    public final Permission COMMAND_BANK_LEAVE_OTHER = COMMAND_BANK.childWildcard("leave").child("other");

    public final Permission COMMAND_BANK_WITHDRAW_OTHER = COMMAND_BANK.childWildcard("withdraw").child("other");

    private final Permission COMMAND_BANK_DELETE = COMMAND_BANK.childWildcard("delete");
    public final Permission COMMAND_BANK_DELETE_OWN = COMMAND_BANK_DELETE.child("own");
    public final Permission COMMAND_BANK_DELETE_OTHER = COMMAND_BANK_DELETE.child("other");

}
