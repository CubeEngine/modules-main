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
package org.cubeengine.module.teleport;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.event.EventManager;
import org.cubeengine.service.filesystem.ModuleConfig;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.ModulePermissions;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.Broadcaster;

/**
 * /setworldspawn 	Sets the world spawn.
 * /spawnpoint 	Sets the spawn point for a player.
 * /tp 	Teleports entities.
 */
@ModuleInfo(name = "Teleport", description = "Better Teleportation")
public class Teleport extends Module
{
    // TODO make override of vanilla-commands optional
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private TaskManager tm;
    @Inject private PermissionManager pm;
    @Inject private org.spongepowered.api.Game game;
    @Inject private Broadcaster bc;
    @Inject private I18n i18n;

    @ModulePermissions private TeleportPerm perms;
    @ModuleConfig private TeleportConfiguration config;
    private TpWorldPermissions tpWorld;

    @Enable
    public void onEnable()
    {
        tpWorld = new TpWorldPermissions(this, perms, game, pm); // per world permissions

        TeleportListener tl = new TeleportListener(this, i18n);
        em.registerListener(this, tl);

        cm.addCommands(cm, this, new MovementCommands(this, tl, i18n));
        cm.addCommands(cm, this, new SpawnCommands(this, em, game, bc, tl, i18n));
        cm.addCommands(cm, this, new TeleportCommands(this, game, bc, tl, i18n));
        cm.addCommands(cm, this, new TeleportRequestCommands(this, tm, tl, game, i18n));
    }

    public TeleportPerm perms()
    {
        return this.perms;
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
