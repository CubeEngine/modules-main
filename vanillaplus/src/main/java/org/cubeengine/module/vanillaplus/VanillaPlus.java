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
package org.cubeengine.module.vanillaplus;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.module.vanillaplus.addition.PluginCommands;
import org.cubeengine.module.vanillaplus.addition.SudoCommand;
import org.cubeengine.module.vanillaplus.improvement.ClearInventoryCommand;
import org.cubeengine.module.vanillaplus.improvement.DifficultyCommand;
import org.cubeengine.module.vanillaplus.improvement.GameModeCommand;
import org.cubeengine.module.vanillaplus.improvement.ItemCommands;
import org.cubeengine.module.vanillaplus.improvement.ItemModifyCommands;
import org.cubeengine.module.vanillaplus.improvement.KillCommands;
import org.cubeengine.module.vanillaplus.improvement.PlayerListCommand;
import org.cubeengine.module.vanillaplus.improvement.SaveCommands;
import org.cubeengine.module.vanillaplus.improvement.TimeCommands;
import org.cubeengine.module.vanillaplus.improvement.WeatherCommands;
import org.cubeengine.module.vanillaplus.improvement.WhitelistCommand;
import org.cubeengine.module.vanillaplus.improvement.removal.RemovalCommands;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.module.vanillaplus.improvement.summon.SpawnMobCommand;
import org.spongepowered.api.Game;

/**
 * A module to improve vanilla commands:
 *
 * /clear 	Clears items from player inventory. {@link ClearInventoryCommand#clearinventory}
 * ??? /deop 	Revoke operator status from a player.
 * /difficulty 	Sets the difficulty level. {@link DifficultyCommand#difficulty}
 * ??? /effect 	Add or remove status effects.
 * /enchant 	Enchants a player item. {@link ItemModifyCommands#enchant}
 * /execute (sudo)	Executes another command. {@link SudoCommand#sudo}
 * /gamemode 	Sets a player's game mode. {@link GameModeCommand#gamemode}
 * /give 	Gives an item to a player. {@link ItemCommands#give},{@link ItemCommands#item}
 * ??? /help 	Provides help for commands.
 * /kill (butcher,remove,removeALl)   Kills entities (players, mobs, items, etc.). {@link RemovalCommands#butcher},{@link RemovalCommands#remove},{@link RemovalCommands#removeAll}
 * /list 	Lists players on the server. {@link PlayerListCommand#list}
 * ??? /op 	Grants operator status to a player.
 * ??? /replaceitem 	Replaces items in inventories.
 * /save-all 	Saves the server to disk. {@link SaveCommands#saveall}
 * ??? /save-off 	Disables automatic server saves.
 * ??? /save-on 	Enables automatic server saves.
 * configure say color??? /say 	Displays a message to multiple players.
 * /seed 	Displays the world seed.
 * ??? /setidletimeout 	Sets the time before idle players are kicked.
 * ??? /spreadplayers 	Teleports entities to random locations.
 * /stop 	Stops a server.
 * /summon (spawnmob) Summons an entity. {@link SpawnMobCommand#spawnMob}
 * /time 	Changes or queries the world's game time. {@link TimeCommands#time}
 * ??? /toggledownfall 	Toggles the weather.
 * /weather 	Sets the weather. {@link WeatherCommands#weather}
 * /whitelist 	Manages server whitelist. {@link WhitelistCommand}
 * ??? /xp 	Adds or removes player experience.
 *
 * Extra commands:
 *
 * /plugins {@link PluginCommands#plugins}
 * /version {@link PluginCommands#version}
 * /pweather {@link WeatherCommands#pweather}
 * /ptime {@link TimeCommands#ptime}
 * /more {@link ItemCommands#more}
 * /stack {@link ItemCommands#stack}
 * /rename {@link ItemModifyCommands#rename}
 * /headchange {@link ItemModifyCommands#headchange}
 * /repair {@link ItemModifyCommands#repair}
 * /kill (for players) {@link KillCommands#kill}
 * /suicide (kill self) {@link KillCommands#suicide}
 */
public class VanillaPlus extends Module
{
    @Inject private CommandManager cm;
    @Inject private Game game;
    @Inject private FileManager fm;
    @Inject private PermissionManager pm;
    private VanillaPlusConfig config;
    private VanillaPlusPerms perms;

    @Enable
    public void onEnable()
    {
        perms = new VanillaPlusPerms(this);
        config = fm.loadConfig(this, VanillaPlusConfig.class);
        cm.addCommands(this, new PlayerListCommand(this, game));
        cm.addCommands(this, new SaveCommands(this, game, pm));
        cm.addCommands(this, new SpawnMobCommand(this));
    }

    public VanillaPlusConfig getConfig()
    {
        return config;
    }

    public VanillaPlusPerms perms()
    {
        return perms;
    }
}
