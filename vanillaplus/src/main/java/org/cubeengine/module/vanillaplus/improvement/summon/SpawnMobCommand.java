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
package org.cubeengine.module.vanillaplus.improvement.summon;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.matcher.EntityMatcher;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.util.blockray.BlockRay.onlyAirFilter;

/**
 * The /spawnmob command.
 */
public class SpawnMobCommand
{
    private VanillaPlus module;
    private I18n i18n;
    private EntityMatcher em;

    public SpawnMobCommand(VanillaPlus module, I18n i18n, EntityMatcher em)
    {
        this.module = module;
        this.i18n = i18n;
        this.em = em;
    }

    @Command(desc = "Spawns the specified Mob")
    public void spawnMob(CommandSource context, @Label("<mob>[:data][,<ridingmob>[:data]]") String data, @Optional Integer amount, @Optional Player player)
    {
        Location loc;
        if (player != null)
        {
            loc = player.getLocation();
        }
        else if (!(context instanceof Player))
        {
            i18n.sendTranslated(context, NEUTRAL, "Succesfully spawned some {text:bugs:color=RED} inside your server!");
            return;
        }
        else
        {
            BlockRayHit<World> hit = BlockRay.from(((Player)context)).blockLimit(200).filter(onlyAirFilter()).end().orElse(null);
            if (hit == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "Cannot find Targetblock");
                return;
            }
            loc = hit.getLocation();
        }
        amount = amount == null ? 1 : amount;

        if (amount <= 0)
        {
            i18n.sendTranslated(context, NEUTRAL, "And how am i supposed to know which mobs to despawn?");
            return;
        }
        if (amount > module.getConfig().improve.spawnmobLimit)
        {
            i18n.sendTranslated(context, NEGATIVE, "The serverlimit is set to {amount}, you cannot spawn more mobs at once!", module.getConfig().improve.spawnmobLimit);
            return;
        }
        loc = loc.add(0.5, 0, 0.5);
        Entity[] entitiesSpawned = SpawnMob.spawnMobs(context, data, loc, amount, em, i18n);
        if (entitiesSpawned == null)
        {
            return;
        }
        EntitySnapshot entitySpawned = entitiesSpawned[0].createSnapshot();
        if (!entitySpawned.get(Keys.PASSENGER).isPresent())
        {
            i18n.sendTranslated(context, POSITIVE, "Spawned {amount} {input#entity}!", amount, entitySpawned.getType().getName());
        }
        else
        {
            Text message = Text.of(entitySpawned.getType().getTranslation());
            while (entitySpawned.get(Keys.PASSENGER).isPresent())
            {
                entitySpawned = entitySpawned.get(Keys.PASSENGER).get();
                message = i18n.getTranslation(context, NONE, "{input#entity} riding {input}", entitySpawned.getType().getName(), message);
            }
            message = i18n.getTranslation(context, POSITIVE, "Spawned {amount} {input#message}!", amount, message);
            context.sendMessage(message);
        }
    }
}
