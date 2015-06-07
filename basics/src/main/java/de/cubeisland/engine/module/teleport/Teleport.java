package de.cubeisland.engine.module.teleport;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.core.filesystem.FileManager;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import de.cubeisland.engine.module.service.task.TaskManager;
import de.cubeisland.engine.module.service.user.UserManager;
import de.cubeisland.engine.module.service.world.WorldManager;

@ModuleInfo(name = "Teleport", description = "Better Teleportation")
public class Teleport extends Module
{
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private UserManager um;
    @Inject private TaskManager tm;
    @Inject private FileManager fm;
    @Inject private WorldManager wm;
    @Inject private PermissionManager pm;

    private TeleportPerm permissions;
    private TpWorldPermissions tpWorld;
    private TeleportConfiguration config;

    @Enable
    public void onEnable()
    {
        config = fm.loadConfig(this, TeleportConfiguration.class);

        permissions = new TeleportPerm(this);
        tpWorld = new TpWorldPermissions(this, permissions, wm, pm); // per world permissions

        //Teleport:
        cm.addCommands(cm, this, new MovementCommands(this));
        cm.addCommands(cm, this, new SpawnCommands(this, em, um));
        cm.addCommands(cm, this, new TeleportCommands(this, um));
        cm.addCommands(cm, this, new TeleportRequestCommands(this, tm, um));


    }

    public TeleportPerm perms()
    {
        return this.permissions;
    }

    public TpWorldPermissions permsTpWorld()
    {
        return tpWorld;
    }

    public TeleportConfiguration getConfig()
    {
        return config;
    }
}
