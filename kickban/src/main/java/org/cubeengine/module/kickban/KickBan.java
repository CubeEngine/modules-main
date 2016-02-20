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
package org.cubeengine.module.kickban;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.filesystem.ModuleConfig;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.ModulePermissions;
import org.cubeengine.service.user.Broadcaster;
import org.spongepowered.api.Game;

/**
 * Overrides and improves Vanilla Kick and Ban Commands:
 *
 * /ban 	Adds player to banlist.
 * /ban-ip 	Adds IP address to banlist.
 * /banlist Displays banlist.
 * /kick 	Kicks a player off a server.
 * /pardon 	Removes entries from the banlist.
 */
@ModuleInfo(name = "KickBan", description = "Kick and Ban players")
public class KickBan extends Module
{
    @Inject private CommandManager cm;
    @Inject private Broadcaster bc;
    @Inject private Game game;
    @Inject private FileManager fm;
    @Inject private I18n i18n;
    @ModulePermissions private KickBanPerms perms;
    @ModuleConfig private KickBanConfig config;


    @Enable
    public void onEnable()
    {
        cm.addCommands(this, new KickBanCommands(this, bc, game, i18n));
    }

    public KickBanPerms perms()
    {
        return perms;
    }

    public KickBanConfig getConfiguration()
    {
        return config;
    }
}
