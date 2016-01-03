package org.cubeengine.module.conomy.bank;

import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

public class BankPermission extends PermissionContainer<Conomy>
{
    public BankPermission(Conomy module)
    {
        super(module);
    }

    private final PermissionDescription ACCESS = register("access.other.bank", "Grants full access to all banks", null);
    public final PermissionDescription ACCESS_MANAGE = register("manage", "Grants manage access to all banks", ACCESS);
    public final PermissionDescription ACCESS_WITHDRAW = register("withdraw", "Grants withdraw access to all banks", ACCESS, ACCESS_MANAGE);
    public final PermissionDescription ACCESS_DEPOSIT = register("deposit", "Grants deposit access to all banks", ACCESS, ACCESS_WITHDRAW);
    public final PermissionDescription ACCESS_SEE = register("see", "Grants looking at all banks", ACCESS, ACCESS_DEPOSIT);
}
