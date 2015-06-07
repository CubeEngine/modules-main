package de.cubeisland.engine.module.teleport;

import de.cubeisland.engine.module.service.permission.Permission;
import de.cubeisland.engine.module.service.permission.PermissionContainer;

import static de.cubeisland.engine.module.service.permission.PermDefault.FALSE;

public class TeleportPerm extends PermissionContainer<Teleport>
{
    public TeleportPerm(Teleport module)
    {
        super(module);
    }

    public final Permission COMMAND = getBasePerm().childWildcard("command");

    private final Permission COMMAND_SPAWN = COMMAND.childWildcard("spawn");
    /**
     * Prevents from being teleported to spawn by someone else
     */
    public final Permission COMMAND_SPAWN_PREVENT = COMMAND_SPAWN.child("prevent");
    /**
     * Allows teleporting a player to spawn even if the player has the prevent permission
     */
    public final Permission COMMAND_SPAWN_FORCE = COMMAND_SPAWN.child("force");

    private final Permission COMMAND_TP = COMMAND.childWildcard("tp");
    /**
     * Ignores all prevent permissions when using the /tp command
     */
    public final Permission COMMAND_TP_FORCE = COMMAND_TP.child("force");
    /**
     * Allows teleporting another player
     */
    public final Permission COMMAND_TP_OTHER = COMMAND_TP.child("other");

    public final Permission COMMAND_TPPOS_SAFE = COMMAND.childWildcard("tppos").child("safe");

    private final Permission TELEPORT = getBasePerm().childWildcard("teleport");
    private final Permission TELEPORT_PREVENT = TELEPORT.newWildcard("prevent");
    /**
     * Prevents from being teleported by someone else
     */
    public final Permission TELEPORT_PREVENT_TP = TELEPORT_PREVENT.child("tp",FALSE);
    /**
     * Prevents from teleporting to you
     */
    public final Permission TELEPORT_PREVENT_TPTO = TELEPORT_PREVENT.child("tpto",FALSE);

    private final Permission COMMAND_TPALL = COMMAND.childWildcard("tpall");
    /**
     * Ignores all prevent permissions when using the /tpall command
     */
    public final Permission COMMAND_TPALL_FORCE = COMMAND_TPALL.child("force");

    private final Permission COMMAND_TPHERE = COMMAND.childWildcard("tphere");
    /**
     * Ignores all prevent permissions when using the /tphere command
     */
    public final Permission COMMAND_TPHERE_FORCE = COMMAND_TPHERE.child("force");

    private final Permission COMMAND_TPHEREALL = COMMAND.childWildcard("tphereall");
    /**
     * Ignores all prevent permissions when using the /tphereall command
     */
    public final Permission COMMAND_TPHEREALL_FORCE = COMMAND_TPHEREALL.child("force");

    private final Permission COMMAND_BACK = COMMAND.childWildcard("back");

    /**
     * Allows using the back command
     */
    public final Permission COMMAND_BACK_USE = COMMAND_BACK.child("use");
    /**
     * Allows using the back command after dieing (if this is not set you won't be able to tp back to your deathpoint)
     */
    public final Permission COMMAND_BACK_ONDEATH = COMMAND_BACK.child("ondeath");
}
