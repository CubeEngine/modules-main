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
package org.cubeengine.module.vanillaplus.improvement.summon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.blockray.RayTrace;
import org.spongepowered.api.util.blockray.RayTraceResult;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

/**
 * The /spawnmob command.
 */
@Singleton
public class SpawnMobCommand
{
    private VanillaPlus module;
    private I18n i18n;
    private EntityMatcher em;

    @Inject
    public SpawnMobCommand(VanillaPlus module, I18n i18n, EntityMatcher em)
    {
        this.module = module;
        this.i18n = i18n;
        this.em = em;
    }

    @Command(desc = "Spawns the specified Mob")
    public void spawnMob(CommandCause context, @Label("<mob>[:data][,<ridingmob>[:data]]") String data, @Option Integer amount, @Option ServerPlayer player)
    {
        ServerLocation loc;
        if (player != null)
        {
            loc = player.getServerLocation();
        }
        else if (!(context instanceof Player))
        {
            i18n.send(context, NEUTRAL, "Succesfully spawned some {text:bugs:color=RED} inside your server!");
            return;
        }
        else
        {
            final RayTraceResult<LocatableBlock> result = RayTrace.block().limit(200).select(RayTrace.onlyAir()).execute().orElse(null);
            if (result == null)
            {
                i18n.send(context, NEGATIVE, "Cannot find Targetblock");
                return;
            }
            loc = result.getSelectedObject().getServerLocation();
        }
        amount = amount == null ? 1 : amount;

        if (amount <= 0)
        {
            i18n.send(context, NEUTRAL, "And how am i supposed to know which mobs to despawn?");
            return;
        }
        if (amount > module.getConfig().improve.spawnmobLimit)
        {
            i18n.send(context, NEGATIVE, "The serverlimit is set to {amount}, you cannot spawn more mobs at once!", module.getConfig().improve.spawnmobLimit);
            return;
        }
        loc = loc.add(0.5, 0, 0.5);
        Entity[] entitiesSpawned = SpawnMob.spawnMobs(context, data, loc, amount, em, i18n);
        if (entitiesSpawned == null)
        {
            return;
        }
        Entity entitySpawned = entitiesSpawned[0];
        if (!entitySpawned.get(Keys.PASSENGERS).isPresent())
        {
            i18n.send(context, POSITIVE, "Spawned {amount} {text#entity}!", amount, entitySpawned.getType().asComponent());
        }
        else
        {
            Component message = entitySpawned.getType().asComponent();
            while (entitySpawned.get(Keys.PASSENGERS).isPresent())
            {
                entitySpawned = entitySpawned.get(Keys.PASSENGERS).get().get(0);
                message = i18n.translate(context, "{text#entity} riding {input}", entitySpawned.getType().asComponent(), message);
            }
            message = i18n.translate(context, POSITIVE, "Spawned {amount} {input#message}!", amount, message);
            context.sendMessage(Identity.nil(), message);
        }
    }
}
