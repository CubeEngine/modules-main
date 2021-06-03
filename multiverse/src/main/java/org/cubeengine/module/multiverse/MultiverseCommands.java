/*
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
package org.cubeengine.module.multiverse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.multiverse.player.MultiverseData;
import org.cubeengine.module.multiverse.player.PlayerData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.world.server.ServerWorld;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
@Command(name = "multiverse", desc = "Multiverse commands", alias = "mv")
public class MultiverseCommands extends DispatcherCommand
{
    private Multiverse module;
    private I18n i18n;

    @Inject
    public MultiverseCommands(Multiverse module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    // TODO create nether & create end commands / auto link to world / only works for NORMAL Env worlds
    // TODO command to load inventory from another universe

    @Command(desc = "Moves a world into another universe")
    public void move(CommandCause context, ServerWorld world, String universe)
    {
        // TODO old universe is not removed
        String previous = module.getUniverse(world);
        if (previous.equals(universe))
        {
            i18n.send(context, NEGATIVE, "{world} is already in the universe {name}", world, universe);
            return;
        }
        module.setUniverse(new ConfigWorld(world), universe);
        i18n.send(context, POSITIVE, "{world} is now in the universe {input}!", world, universe);

        Sponge.server().onlinePlayers().stream().filter(player -> player.world().equals(world)).forEach(
            p -> {
                final Map<String, DataView> data = p.get(MultiverseData.DATA).orElse(new HashMap<>());
                // Serialize current data on previous universe
                data.put(previous, PlayerData.of(data.get(previous), world).applyFromPlayer(p).toContainer());
                // Deserialize new universe data on player
                PlayerData.of(data.get(universe), world).applyToPlayer(p);

                i18n.send(p, NEUTRAL, "The sky opens up and sucks in the whole world.");
                p.playSound(Sound.sound(SoundTypes.BLOCK_PORTAL_TRIGGER, Source.NEUTRAL, 1, 1), p.location().position());
                p.offer(Keys.POTION_EFFECTS, Arrays.asList(PotionEffect.of(PotionEffectTypes.BLINDNESS, 1, 2 * 20)));
                i18n.send(p, NEUTRAL, "When you open your eyes you now are in {input#univserse}.", universe);
                p.offer(MultiverseData.DATA, data);
                p.offer(MultiverseData.UNIVERSE, universe);
            });
    }

    @Command(desc = "Lists all known universes")
    public void list(CommandCause context)
    {
        // TODO hover & click features
        Set<Entry<String, Set<ConfigWorld>>> universes = module.getConfig().universes.entrySet();
        if (universes.isEmpty())
        {
            i18n.send(context, NEUTRAL, "There is no universe yet.");
            return;
        }
        i18n.send(context, POSITIVE, "The following universes exists:");
        for (Entry<String, Set<ConfigWorld>> entry : universes)
        {
            context.sendMessage(Identity.nil(), Component.text(entry.getKey() + ":"));
            for (ConfigWorld world : entry.getValue())
            {
                context.sendMessage(Identity.nil(), Component.text(" - " + world.getName()));
            }
        }
    }

    @Command(desc = "Removes a universe")
    public void remove(CommandCause context, String universe)
    {
        Set<ConfigWorld> removed = module.getConfig().universes.remove(universe);
        if (removed == null)
        {
            i18n.send(context, NEGATIVE, "There is no universe named {}", universe);
            return;
        }
        removed.stream().filter(cWorld -> cWorld.getWorld() != null)
                        .forEach(cWorld -> module.setUniverse(cWorld, "unknown"));
        module.getConfig().save();
        i18n.send(context, POSITIVE, "{name} was removed and {amount} universes moved to {name}", universe, removed.size(), "unknown");
    }

    @Command(desc = "Renames a universe")
    public void rename(CommandCause context, String universe, String newName)
    {
        Set<ConfigWorld> worlds = module.getConfig().universes.remove(universe);
        if (worlds == null)
        {
            i18n.send(context, NEGATIVE, "There is no universe named {}", universe);
            return;
        }
        i18n.send(context, POSITIVE, "Renamed universe {input} to {input}", universe, newName);
        module.getConfig().universes.put(newName, worlds);
        module.getConfig().save();
    }

}
