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
package de.cubeisland.engine.module.teleport;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.Disable;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.service.filesystem.FileManager;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.service.command.CommandManager;
import de.cubeisland.engine.service.permission.PermissionManager;
import de.cubeisland.engine.service.task.TaskManager;
import de.cubeisland.engine.service.user.UserManager;
import de.cubeisland.engine.service.world.WorldManager;

/**
 * /setworldspawn 	Sets the world spawn.
 * /spawnpoint 	Sets the spawn point for a player.
 * /tp 	Teleports entities.
 */
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

        cm.addCommands(cm, this, new MovementCommands(this));
        cm.addCommands(cm, this, new SpawnCommands(this, em, um));
        cm.addCommands(cm, this, new TeleportCommands(this, um));
        cm.addCommands(cm, this, new TeleportRequestCommands(this, tm, um));

        em.registerListener(this, new TeleportListener(this, um));

        // TODO load after roles, if OptionSubjects available => per role spawn?
    }

    @Disable
    public void onDisable()
    {
        cm.removeCommands(this);
        em.removeListeners(this);
        pm.cleanup(this);
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
