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
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.ban.BanService;

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
    @Inject private BanService bs;
    @Inject private UserManager um;
    @Inject private Game game;
    @Inject private FileManager fm;
    private KickBanPerms perms;
    private KickBanConfig config;


    @Enable
    public void onEnable()
    {
        this.perms = new KickBanPerms(this);
        cm.addCommands(this, new KickBanCommands(this, bs, um, game));
        config = fm.loadConfig(this, KickBanConfig.class);
    }

    @Disable
    public void onDisable()
    {
        cm.removeCommands(this);
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