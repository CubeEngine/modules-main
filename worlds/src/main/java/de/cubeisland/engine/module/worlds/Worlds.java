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
package de.cubeisland.engine.module.worlds;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;

import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.core.config.codec.NBTCodec;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.module.exception.ModuleLoadError;
import de.cubeisland.engine.module.worlds.commands.WorldsCommands;
import de.cubeisland.engine.module.worlds.config.WorldsConfig;
import de.cubeisland.engine.module.worlds.converter.DiffcultyConverter;
import de.cubeisland.engine.module.worlds.converter.EnvironmentConverter;
import de.cubeisland.engine.module.worlds.converter.GameModeConverter;
import de.cubeisland.engine.module.worlds.converter.InventoryConverter;
import de.cubeisland.engine.module.worlds.converter.PotionEffectConverter;
import de.cubeisland.engine.module.worlds.converter.WorldTypeConverter;

public class Worlds extends Module
{
    private WorldsPermissions perms;

    public Multiverse getMultiverse()
    {
        return multiverse;
    }

    private Multiverse multiverse;

    @Override
    public void onLoad()
    {
        ConverterManager manager = this.getCore().getConfigFactory().getDefaultConverterManager();
        manager.registerConverter(new DiffcultyConverter(), Difficulty.class);
        manager.registerConverter(new EnvironmentConverter(), Environment.class);
        manager.registerConverter(new GameModeConverter(), GameMode.class);
        manager.registerConverter(new WorldTypeConverter(), WorldType.class);
///*TODO remove saving into yml too
        manager.registerConverter(new InventoryConverter(Bukkit.getServer()), Inventory.class);
        manager.registerConverter(new PotionEffectConverter(), PotionEffect.class);
//*/
        NBTCodec codec = this.getCore().getConfigFactory().getCodecManager().getCodec(NBTCodec.class);
        manager = codec.getConverterManager();
        manager.registerConverter(new InventoryConverter(Bukkit.getServer()), Inventory.class);
        manager.registerConverter(new PotionEffectConverter(), PotionEffect.class);
    }

    @Override
    public void onEnable()
    {
        try
        {
            multiverse = new Multiverse(this, this.loadConfig(WorldsConfig.class));
        }
        catch (IOException e)
        {
            throw new ModuleLoadError(e);
        }
        this.getCore().getCommandManager().addCommand(new WorldsCommands(this, multiverse));
        this.perms = new WorldsPermissions(this);
    }

    public WorldsPermissions perms()
    {
        return perms;
    }
}
