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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.cubeengine.module.multiverse.player.MultiverseData;
import org.cubeengine.module.multiverse.player.PlayerData;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.world.server.ServerWorld;

public class MultiverseListener
{
    private Multiverse module;

    public MultiverseListener(Multiverse module)
    {
        this.module = module;
    }

    @Listener
    public void onWorldChange(ChangeEntityWorldEvent.Reposition event)
    {
        ServerWorld from = event.originalWorld();
        ServerWorld to = event.destinationWorld();
        Entity target = event.entity();

        final String toUniverse = module.getUniverse(to);
        final String fromUniverse = module.getUniverse(from);
        if (from.equals(to) || fromUniverse.equals(toUniverse))
        {
            return;
        }

        if (target instanceof ServerPlayer)
        {
            final Map<String, DataContainer> map = target.get(MultiverseData.DATA).orElse(new HashMap<>());
            final DataContainer dataContainerFrom = map.get(fromUniverse);
            final PlayerData fromPlayerData = PlayerData.of(dataContainerFrom, from).applyFromPlayer(((ServerPlayer)target));// save playerdata
            map.put(fromUniverse, fromPlayerData.toContainer());

            final DataContainer dataContainerTo = map.get(toUniverse);
            PlayerData.of(dataContainerTo, to).applyToPlayer(((ServerPlayer)target));  // load playerdata

            target.offer(MultiverseData.DATA, map);
            target.offer(MultiverseData.UNIVERSE, toUniverse);
            module.getLogger().info("{} entered the universe {}", ((Player)target).name(), toUniverse);
        }
        else
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onJoin(ServerSideConnectionEvent.Join event)
    {
        ServerPlayer player = event.player();
        final Map<String, DataContainer> data = player.get(MultiverseData.DATA).orElse(new HashMap<>());
        final Optional<String> currentUniverse = player.get(MultiverseData.UNIVERSE);
        ServerWorld world = player.world();
        String loginUniverse = module.getUniverse(world);
        if (!currentUniverse.isPresent())
        {
            player.offer(MultiverseData.UNIVERSE, loginUniverse); // set universe
        }
        else if (!currentUniverse.get().equals(loginUniverse)) // Player is not in the expected universe
        {
            player.offer(MultiverseData.UNIVERSE, loginUniverse); // update universe
            PlayerData.of(data.get(loginUniverse), world).applyToPlayer(player); // load playerdata
        }
    }


    @Listener
    public void onQuit(ServerSideConnectionEvent.Disconnect event)
    {
        ServerPlayer player = event.player();
        final Map<String, DataContainer> data = player.get(MultiverseData.DATA).orElse(null);
        if (data == null)
        {
            return;
            // TODO how?
        }
        String universe = module.getUniverse(player.world());
        final PlayerData playerData = PlayerData.of(data.get(universe), player.world()).applyFromPlayer(player);
        data.put(universe, playerData.toContainer());
        player.offer(MultiverseData.DATA, data);
        player.offer(MultiverseData.UNIVERSE, universe);
    }

    @Listener
    public void onRespawn(RespawnPlayerEvent event)
    {
        ServerWorld from = event.originalWorld();
        ServerWorld to = event.destinationWorld();
        ServerPlayer target = event.entity();

        final String fromUniverse = module.getUniverse(from);
        final String toUniverse = module.getUniverse(to);
        if (from.equals(to) || fromUniverse.equals(toUniverse))
        {
            return;
        }

        final Map<String, DataContainer> data = target.get(MultiverseData.DATA).orElse(new HashMap<>());
        data.put(fromUniverse, PlayerData.of(data.get(fromUniverse), from).applyFromPlayer(target).toContainer()); // save playerdata
        PlayerData.of(data.get(toUniverse), to).applyToPlayer(target);
        target.offer(MultiverseData.UNIVERSE, toUniverse);
        target.offer(MultiverseData.DATA, data);
    }
}
