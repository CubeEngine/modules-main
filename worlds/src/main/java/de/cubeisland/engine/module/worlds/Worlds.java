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
import java.nio.file.Path;
import javax.inject.Inject;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.service.filesystem.FileManager;
import de.cubeisland.engine.service.command.CommandManager;
import de.cubeisland.engine.service.world.WorldManager;
import de.cubeisland.engine.module.worlds.commands.WorldsCommands;
import de.cubeisland.engine.module.worlds.config.WorldsConfig;
import de.cubeisland.engine.module.worlds.converter.InventoryConverter;
import de.cubeisland.engine.module.worlds.converter.PotionEffectConverter;
import de.cubeisland.engine.reflect.Reflector;
import de.cubeisland.engine.reflect.codec.nbt.NBTCodec;
import org.spongepowered.api.Game;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.potion.PotionEffect;

public class Worlds extends Module
{
    private WorldsPermissions perms;
    private Multiverse multiverse;
    @Inject private Reflector reflector;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private WorldManager wm;
    @Inject private Game game;
    @Inject private FileManager fm;
    @Inject private Path modulePath;
    @Inject private Log logger;

    @Enable
    public void onLoad()
    {
        ConverterManager manager = reflector.getDefaultConverterManager();
///*TODO remove saving into yml too
        manager.registerConverter(new InventoryConverter(game), Inventory.class);
        manager.registerConverter(new PotionEffectConverter(), PotionEffect.class);
//*/
        NBTCodec codec = reflector.getCodecManager().getCodec(NBTCodec.class);
        manager = codec.getConverterManager();
        manager.registerConverter(new InventoryConverter(game), Inventory.class);
        manager.registerConverter(new PotionEffectConverter(), PotionEffect.class);

        try
        {
            multiverse = new Multiverse(this, fm.loadConfig(this, WorldsConfig.class), wm, modulePath, cm, logger, em, reflector);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e); // TODO
        }

        this.cm.addCommand(new WorldsCommands(this, multiverse, wm));
        this.perms = new WorldsPermissions(this);
    }

    public Multiverse getMultiverse()
    {
        return multiverse;
    }

    public WorldsPermissions perms()
    {
        return perms;
    }
}
