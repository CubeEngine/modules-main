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

import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;

@SuppressWarnings("all")
public class VanillaPlusConfig extends ReflectedYaml
{
    public Fixes fix;
    public Improvments improve;
    public Additions add;

    public static class Fixes implements Section
    {
        @Comment("Allows using & followed by the appropriate color-code or style-code to write colored signs")
        public boolean styledSigns = true;

        @Comment("Right click on a painting allows switching the painting with the mouse wheel")
        public boolean paintingSwitcher = true;

        @Comment("Shows the tamer of an animal when rightclicking on it")
        public boolean showTamer = true;

        @Comment("Potions or Tools can be put into an Anvil or Brewingstand, allowing to brew/enchant the whole stack instead of just one item\n"
            + "Turning this on prevents putting unsafe Itemstacks into these inventories")
        public boolean preventOverstackedItems = true;
    }

    public static class Improvments implements Section
    {
        @Comment("Also adds an alias /butcher for living entities only")
        public boolean commandRemove = true;
        @Comment("Also adds an alias /spawnmob for living entities only")
        public boolean commandSummon = true;

        public int spawnmobLimit = 20;

        @Comment("Improves /clear and adds some aliases")
        public boolean commandClearinventory = true;
        @Comment("Improves /difficulty")
        public boolean commandDifficulty = true;
        @Comment("Improves /gamemode e.g. allowing to toggle between survival and creative")
        public boolean commandGamemode = true;

        @Comment("Improves /give\n"
            + "adds an alias /item to give an item to yourself\n"
            + "adds /more to refill itemstacks\n"
            + "adds /stack to stack similar items together")
        public boolean commandItem = true;

        @Comment("Allows stacking tools and other items up to 64 even when they usually do not stack that high")
        public boolean commandStackTools = false;

        @Comment("Improves /enchant\n"
            + "adds /rename and /lore to allow colored ItemNames and Lore\n"
            + "adds /headchange to change any head to a player-head of your choice\n"
            + "adds /repair to refill the durability of tools")
        public boolean commandItemModify = true;

        @Comment("Improves /kill\n"
            + "adds an alias /suicide to kill yourself")
        public boolean commandKill = true;

        @Comment("Improves /op and /deop")
        public boolean commandOp = true;

        @Comment("Improves /list")
        public boolean commandList = true;

        @Comment("Improves /save-all /save-on /save-off")
        public boolean commandSave = true;

        @Comment("Improves /stop including a kick reason for the players")
        public boolean commandStop = true;

        @Comment("Improves /time with per World time and adds /ptime for per Player time")
        public boolean commandTime = true;

        @Comment("Improves /weather with per World weather and adds /pweather for per Player weather")
        public boolean commandWeather = true;

        @Comment("Improves /whitelist")
        public boolean commandWhitelist = true;
    }

    public static class Additions implements Section
    {

    }
}
