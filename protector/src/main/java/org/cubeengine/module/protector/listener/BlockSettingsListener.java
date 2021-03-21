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
package org.cubeengine.module.protector.listener;

import java.util.List;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.Hostile;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.weather.LightningBolt;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.module.protector.listener.SettingsListener.checkSetting;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

@Singleton
public class BlockSettingsListener extends PermissionContainer
{

    private RegionManager manager;
    private I18n i18n;

    public final Permission buildPerm;
    public final Permission explodePlayer;

    @Inject
    public BlockSettingsListener(RegionManager manager, PermissionManager pm, I18n i18n)
    {
        super(pm, Protector.class);
        this.manager = manager;
        this.i18n = i18n;

        buildPerm = this.register("bypass.build", "Region bypass for building");
        explodePlayer = this.register("bypass.blockdamage.explode.player", "Region bypass for players causing blockdamage with explosions");
    }

    @Listener(order = Order.EARLY)
    public void onPreChangeBlock(ChangeBlockEvent.Pre event, @Root LocatableBlock block)
    {
        for (ServerLocation loc : event.locations()) {

            if (loc.blockType() != BlockTypes.AIR.get() && loc.blockType() != block.blockState().type()) {
                List<Region> regionsAt = manager.getRegionsAt(loc);
                if (checkSetting(event, null, regionsAt, () -> null, s -> s.blockDamage.block.getOrDefault(block.blockState().type() , UNDEFINED), UNDEFINED) == FALSE)
                {
                    return;
                }
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onChangeBlock(ExplosionEvent.Detonate event)
    {
        Object rootCause = event.cause().root();
        // Check Explosions first...

        for (ServerLocation loc : event.affectedLocations())
        {
            List<Region> regionsAt = manager.getRegionsAt(loc);
            checkSetting(event, null, regionsAt, () -> null, (s) -> s.blockDamage.allExplosion, UNDEFINED);
            ServerPlayer player = event.cause().context().get(EventContextKeys.CREATOR).filter(p -> p instanceof ServerPlayer).map(ServerPlayer.class::cast).orElse(null);
            if (player == null)
            {
                player = event.cause().context().get(EventContextKeys.IGNITER).filter(p -> p instanceof ServerPlayer).map(ServerPlayer.class::cast).orElse(null);
            }
            if (rootCause instanceof ServerPlayer)
            {
                player = ((ServerPlayer) rootCause);
            }
            if (player != null)
            {
                checkSetting(event, player, regionsAt, () -> explodePlayer, (s) -> s.blockDamage.playerExplosion, UNDEFINED);
                if (event.isCancelled())
                {
                    i18n.send(ChatType.ACTION_BAR, player, CRITICAL, "You are not allowed to let stuff explode here!");
                }
            }
            if (event.isCancelled())
            {
                return;
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onChangeBlock(ChangeBlockEvent.All event)
    {
        Object rootCause = event.cause().root();
        // Hostile Mob causes BlockChange?
        if (rootCause instanceof Hostile)
        {
            for (Transaction<BlockSnapshot> trans : event.transactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.original().location().get());
                if (checkSetting(event, null, regionsAt, () -> null, (s) -> s.blockDamage.monster, UNDEFINED) == FALSE)
                {
                    return;
                }
            }
        }

        // Block causes BlockChange?
        if (rootCause instanceof LocatableBlock)
        {
            for (Transaction<BlockSnapshot> trans : event.transactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.original().location().get());
                if (checkSetting(event, null, regionsAt, () -> null, s -> s.blockDamage.block.getOrDefault(((LocatableBlock) rootCause).blockState().type() , UNDEFINED), UNDEFINED) == FALSE)
                {
                    return;
                }
            }
        }

        if (rootCause instanceof LightningBolt)
        {
            for (Transaction<BlockSnapshot> trans : event.transactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.original().location().get());
                if (checkSetting(event, null, regionsAt, () -> null, s -> s.blockDamage.lightning, UNDEFINED) == FALSE)
                {
                    return;
                }
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onPreBuild(ChangeBlockEvent.Pre event, @First ServerPlayer player)
    {
        if (this.buildPerm.check(player))
        {
            return;
        }

        for (ServerLocation loc : event.locations())
        {
            List<Region> regionsAt = manager.getRegionsAt(loc);
            if (checkSetting(event, player, regionsAt, () -> null, (s) -> s.build, UNDEFINED) == FALSE)
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to build here.");
                return;
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onBuild(ChangeBlockEvent.All event, @First ServerPlayer player)
    {
        if (this.buildPerm.check(player))
        {
            return;
        }

        for (Transaction<BlockSnapshot> transaction : event.transactions())
        {
            if (transaction.original().state().type() == transaction.finalReplacement().state().type())
            {
                continue;
            }
            transaction.original().location().ifPresent(loc -> {
                List<Region> regionsAt = manager.getRegionsAt(loc);
                if (checkSetting(event, player, regionsAt, () -> null, (s) -> s.build, UNDEFINED) == FALSE)
                {
                    i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to build here.");
                    return;
                }
            });
        }
    }
}