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
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
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
import org.cubeengine.module.vanillaplus.improvement.OpCommands;
import org.cubeengine.module.vanillaplus.improvement.PlayerListCommand;
import org.cubeengine.module.vanillaplus.improvement.SaveCommands;
import org.cubeengine.module.vanillaplus.improvement.StopCommand;
import org.cubeengine.module.vanillaplus.improvement.TimeCommands;
import org.cubeengine.module.vanillaplus.improvement.WeatherCommands;
import org.cubeengine.module.vanillaplus.improvement.WhitelistCommand;
import org.cubeengine.module.vanillaplus.improvement.removal.RemoveCommands;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.filesystem.ModuleConfig;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.module.vanillaplus.improvement.summon.SpawnMobCommand;
import org.cubeengine.service.matcher.EnchantMatcher;
import org.cubeengine.service.matcher.MaterialMatcher;
import org.cubeengine.service.matcher.TimeMatcher;
import org.cubeengine.service.matcher.WorldMatcher;
import org.cubeengine.service.task.TaskManager;
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
 * /kill (butcher,remove,removeALl)   Kills entities (players, mobs, items, etc.). {@link RemoveCommands#butcher},{@link RemoveCommands#remove},{@link RemoveCommands#removeAll}
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
@ModuleInfo(name = "VanillaPlus", description = "Improved Vanilla")
public class VanillaPlus extends Module
{
    @Inject private CommandManager cm;
    @Inject private Game game;
    @Inject private FileManager fm;
    @Inject private I18n i18n;
    @Inject private MaterialMatcher mm;
    @Inject private EnchantMatcher em;
    @Inject private TimeMatcher tm;
    @Inject private WorldMatcher wm;
    @Inject private TaskManager tam;
    @ModuleConfig private VanillaPlusConfig config;

    @Enable
    public void onEnable()
    {
        enableImprovements();
        enableFixes();
        enableAdditions();
    }

    private void enableAdditions()
    {

    }

    private void enableFixes()
    {

    }

    private void enableImprovements()
    {
        if (config.improve.commandRemove)
        {
            // TODO remove cmds
        }
        if (config.improve.commandSummon)
        {
            cm.addCommands(this, new SpawnMobCommand(this));
        }
        if (config.improve.commandClearinventory)
        {
            cm.addCommands(this, new ClearInventoryCommand(this, i18n));
        }
        if (config.improve.commandDifficulty)
        {
            cm.addCommands(this, new DifficultyCommand(i18n));
        }
        if (config.improve.commandGamemode)
        {
            cm.addCommands(this, new GameModeCommand(this, i18n));
        }
        if (config.improve.commandItem)
        {
            cm.addCommands(this, new ItemCommands(this, mm, em, i18n));
        }
        if (config.improve.commandItemModify)
        {
            cm.addCommands(this, new ItemModifyCommands(this, i18n, em));
        }
        if (config.improve.commandKill)
        {
            cm.addCommands(this, new KillCommands(this, i18n));
        }
        if (config.improve.commandOp)
        {
            cm.addCommands(this, new OpCommands());
        }
        if (config.improve.commandList)
        {
            cm.addCommands(this, new PlayerListCommand(i18n));
        }
        if (config.improve.commandSave)
        {
            cm.addCommands(this, new SaveCommands(i18n));
        }
        if (config.improve.commandStop)
        {
            cm.addCommands(this, new StopCommand());
        }
        if (config.improve.commandTime)
        {
            cm.addCommands(this, new TimeCommands(this, i18n, tm, wm, tam));
        }
        if (config.improve.commandWeather)
        {
            cm.addCommands(this, new WeatherCommands(i18n));
        }
        if (config.improve.commandWhitelist)
        {
            cm.addCommands(this, new WhitelistCommand(this, i18n));
        }
    }

    public VanillaPlusConfig getConfig()
    {
        return config;
    }
}
