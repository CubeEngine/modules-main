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
package org.cubeengine.module.multiverse;

import java.util.Arrays;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.module.multiverse.player.MultiverseData;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.world.World;

import static org.cubeengine.service.i18n.formatter.MessageType.*;

@Command(name = "multiverse", desc = "Multiverse commands", alias = "mv")
public class MultiverseCommands extends ContainerCommand
{
    private Multiverse module;
    private I18n i18n;

    public MultiverseCommands(Multiverse module, I18n i18n)
    {
        super(module);
        this.module = module;
        this.i18n = i18n;
    }

    // TODO create nether & create end commands / auto link to world / only works for NORMAL Env worlds
    // TODO command to load inventory from another universe

    @Command(desc = "Moves a world into another universe")
    public void move(CommandSource context, World world, String universe)
    {
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
                p.offer(Keys.POTION_EFFECTS, Arrays.asList(PotionEffect.of(PotionEffectTypes.BLINDNESS, 1, 2)));
                i18n.sendTranslated(p, NEUTRAL, "When you open your eyes you now are in {input#univserse}.", universe);
                p.offer(data);
            });
    }

}
