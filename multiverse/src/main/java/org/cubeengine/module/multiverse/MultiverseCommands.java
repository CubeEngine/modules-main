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
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.module.multiverse.player.MultiverseData;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Command(name = "multiverse", desc = "Multiverse commands", alias = "mv")
public class MultiverseCommands extends ContainerCommand
{
    private Multiverse module;
    private I18n i18n;

    public MultiverseCommands(CommandManager base, Multiverse module, I18n i18n)
    {
        super(base, Multiverse.class);
        this.module = module;
        this.i18n = i18n;
    }

    // TODO create nether & create end commands / auto link to world / only works for NORMAL Env worlds
    // TODO command to load inventory from another universe

    @Command(desc = "Moves a world into another universe")
    public void move(CommandSource context, World world, String universe)
    {
        // TODO old universe is not removed
        String previous = module.getUniverse(world);
        if (previous.equals(universe))
        {
            i18n.sendTranslated(context, NEGATIVE, "{world} is already in the universe {name}", world, universe);
            return;
        }
        module.setUniverse(world, universe);
        i18n.sendTranslated(context, POSITIVE, "{world} is now in the universe {input}!", world, universe);

        Sponge.getServer().getOnlinePlayers().stream().filter(player -> player.getWorld().equals(world)).forEach(
            p -> {
                MultiverseData data = p.get(MultiverseData.class).get();
                data.from(previous, world).applyFromPlayer(p);
                data.from(universe, world).applyToPlayer(p);
                i18n.sendTranslated(p, NEUTRAL, "The sky opens up and sucks in the whole world.");
                p.playSound(SoundTypes.BLOCK_PORTAL_TRIGGER, p.getLocation().getPosition(), 1);
                p.offer(Keys.POTION_EFFECTS, Arrays.asList(PotionEffect.of(PotionEffectTypes.BLINDNESS, 1, 2 * 20)));
                i18n.sendTranslated(p, NEUTRAL, "When you open your eyes you now are in {input#univserse}.", universe);
                p.offer(data);
            });
    }

    @Command(desc = "Lists all known universes")
    public void list(CommandSource context)
    {
        // TODO hover & click features
        Set<Entry<String, Set<ConfigWorld>>> universes = module.getConfig().universes.entrySet();
        if (universes.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "There is no universe yet.");
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The following univserses exits:");
        for (Entry<String, Set<ConfigWorld>> entry : universes)
        {
            context.sendMessage(Text.of(entry.getKey(), ":"));
            for (ConfigWorld world : entry.getValue())
            {
                context.sendMessage(Text.of(" - ", world.getName()));
            }
        }
    }

    @Command(desc = "Removes a universe")
    public void remove(CommandSource context, String universe)
    {
        Set<ConfigWorld> removed = module.getConfig().universes.remove(universe);
        if (removed == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "There is no universe named {}", universe);
            return;
        }
        removed.stream().filter(cWorld -> cWorld.getWorld() != null)
                        .forEach(cWorld -> module.setUniverse(cWorld.getWorld(), "unknown"));
        module.getConfig().save();
        i18n.sendTranslated(context, POSITIVE, "{name} was removed and {amount} universes moved to {name}", universe, removed.size(), "unknown");
    }

    @Command(desc = "Renames a universe")
    public void rename(CommandSource context, String universe, String newName)
    {
        Set<ConfigWorld> worlds = module.getConfig().universes.remove(universe);
        if (worlds == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "There is no universe named {}", universe);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "Renamed universe {input} to {input}", universe, newName);
        module.getConfig().universes.put(newName, worlds);
        module.getConfig().save();
    }

}
