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
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.ai.SetAITargetEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.util.Tristate;

import static org.cubeengine.module.protector.listener.SettingsListener.checkSetting;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

@Singleton
public class DamageSettingsListener extends PermissionContainer
{
    private RegionManager manager;

    public final Permission entityDamageAll;
    public final Permission entityDamageLiving;
    public final Permission playerDamgeAll;
    public final Permission playerDamgeLiving;
    public final Permission playerDamgePVP;
    public final Permission playerTargeting;

    @Inject
    public DamageSettingsListener(RegionManager manager, PermissionManager pm)
    {
        super(pm, Protector.class);
        this.manager = manager;
    // TODO description
        entityDamageAll = this.register("bypass.entity-damage.all", "");
        entityDamageLiving = this.register("bypass.entity-damage.living", "");
        playerDamgeAll = this.register("bypass.player-damage.all", "");
        playerDamgeLiving = this.register("bypass.player-damage.living", "");
        playerDamgePVP = this.register("bypass.player-damage.pvp", "");
        playerTargeting = this.register("bypass.player-targeting", "");
    }

    @Listener(order = Order.EARLY)
    public void onEntityDamage(DamageEntityEvent event)
    {
        Entity target = event.entity();
        if (target instanceof ServerPlayer)
        {
            onPlayerDamage(event, ((ServerPlayer) target));
        }
        else
        {
            onMobDamage(event, target);
        }
    }

    private void onMobDamage(DamageEntityEvent event, Entity target)
    {
        Entity entitySource = getEntitySource(event);
        ServerPlayer playerSource = null;
        if (entitySource instanceof ServerPlayer)
        {
            playerSource = ((ServerPlayer) entitySource);
        }

        List<Region> regionsAt = manager.getRegionsAt(target.serverLocation());

        Tristate defaultTo = checkSetting(event, playerSource, regionsAt, () -> entityDamageAll, s -> s.entityDamage.all, UNDEFINED);

        if (entitySource instanceof Living)
        {
            defaultTo = checkSetting(event, playerSource, regionsAt, () -> entityDamageLiving, s -> s.entityDamage.byLiving, defaultTo);
        }
        if (entitySource != null)
        {
            EntityType type = entitySource.type();
            checkSetting(event, null, regionsAt, () -> null, s -> s.entityDamage.byEntity.getOrDefault(type, UNDEFINED), defaultTo);
        }
    }

    private void onPlayerDamage(DamageEntityEvent event, ServerPlayer target)
    {
        Entity entitySource = getEntitySource(event);

        List<Region> regionsAt = manager.getRegionsAt(target.serverLocation());

        Tristate defaultTo = checkSetting(event, target, regionsAt, () -> playerDamgeAll, s -> s.playerDamage.all, UNDEFINED);

        if (entitySource instanceof Living)
        {
            defaultTo = checkSetting(event, target, regionsAt, () -> playerDamgeLiving, s -> s.playerDamage.byLiving, defaultTo);
        }
        if (entitySource instanceof Player)
        {
            checkSetting(event, target, regionsAt, () -> playerDamgePVP, s -> s.playerDamage.pvp, defaultTo);
        }
    }

    private Entity getEntitySource(DamageEntityEvent event)
    {
        DamageSource source = event.cause().first(DamageSource.class).get();
        Entity entitySource = null;
        if (source instanceof EntityDamageSource)
        {
            entitySource = ((EntityDamageSource) source).source();
            if (source instanceof IndirectEntityDamageSource)
            {
                entitySource = ((IndirectEntityDamageSource) source).indirectSource();
            }
        }
        return entitySource;
    }


    @Listener(order = Order.EARLY)
    public void onTargetPlayer(SetAITargetEvent event, @Getter("target") ServerPlayer player)
    {
        List<Region> regions = manager.getRegionsAt(player.serverLocation());
        checkSetting(event, player, regions, () -> this.playerTargeting, s -> s.playerDamage.aiTargeting, UNDEFINED);
    }

}