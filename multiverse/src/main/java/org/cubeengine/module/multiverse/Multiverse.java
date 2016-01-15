package org.cubeengine.module.multiverse;


import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.reflect.Reflector;
import de.cubeisland.engine.reflect.codec.nbt.NBTCodec;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.multiverse.converter.InventoryConverter;
import org.cubeengine.module.multiverse.converter.PotionEffectConverter;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.Game;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.item.inventory.Inventory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Group Worlds into Universes
 * <p>
 * Mass konfigurate worlds (in that universe)
 * <p>
 * Separate PlayerData(Inventories) for universes /
 * Block plugin teleports of entites with inventories / alternative save entity inventory and reload later? using custom data
 * <p>
 * ContextProvider for universe (allowing permissions per universe)
 * <p>
 * permissions for universe access
 *
 * wait for Sponge Inventory or better Data impl of setRawData(..)
 * save all data i want in custom manipulator (Map:World->PlayerData)
 */
@ModuleInfo(name = "Multiverse", description = "Group worlds into universes")
public class Multiverse extends Module
{
    @Inject private Game game;
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private I18n i18n;
    @Inject private Reflector reflector;

    @Enable
    public void onEnable() throws IOException
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

        UniverseManager um = new UniverseManager(this, getProvided(Path.class), getProvided(Log.class), reflector);
        cm.addCommand(new MultiverseCommands(this, um, i18n, game));
        em.registerListener(this, new MultiverseListener());

    }

}
