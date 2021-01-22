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
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.module.protector.listener.SettingsListener.checkSetting;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

@Singleton
public class SpawnSettingsListener extends PermissionContainer
{

    private RegionManager manager;
    private PermissionManager pm;
    private I18n i18n;

    public final Permission spawnEntityPlayerPerm;

    @Inject
    public SpawnSettingsListener(RegionManager manager, PermissionManager pm, I18n i18n)
    {
        super(pm, Protector.class);
        this.manager = manager;
        this.pm = pm;
        this.i18n = i18n;
        // TODO description
        spawnEntityPlayerPerm = this.register("bypass.spawn.player", "Region bypass for players spawning entities");
    }

    public enum SpawnType
    {
        NATURALLY, PLAYER, PLUGIN
    }

    @Listener(order = Order.EARLY)
    public void onSpawn(SpawnEntityEvent event)
    {
        for (Entity entity : event.getEntities())
        {
            ServerLocation loc = entity.getServerLocation();
            EntityType<?> type = entity.getType();
            List<Region> regionsAt = manager.getRegionsAt(loc);
            Cause cause = event.getCause();
            Optional<ServerPlayer> player = cause.first(ServerPlayer.class);
            if (event.getCause().getContext().get(EventContextKeys.SPAWN_TYPE).map(t -> t.equals(SpawnTypes.PLUGIN.get())).orElse(false))
            {
                if (player.isPresent())
                {
                    final ResourceKey entityTypeKey = type.key(RegistryTypes.ENTITY_TYPE);
                    Permission usePerm = pm.register(
                            SpawnSettingsListener.class, entityTypeKey.getValue(), "Allows spawning a " + PlainComponentSerializer.plain().serialize(type.asComponent()), spawnEntityPlayerPerm);
                    if (checkSetting(event, player.get(), regionsAt, () -> usePerm, (s) -> s.spawn.player.getOrDefault(type, UNDEFINED), UNDEFINED) == FALSE)
                    {
                        i18n.send(ChatType.ACTION_BAR, player.get(), CRITICAL, "You are not allowed spawn this here.");
                        return;
                    }
                }
                else if (checkSetting(event, null, regionsAt, () -> null, (s) -> s.spawn.plugin.getOrDefault(type, UNDEFINED), UNDEFINED) == FALSE)
                {
                    return;
                }
            }
            else if (player.isPresent())
            {
                final ResourceKey entityTypeKey = type.key(RegistryTypes.ENTITY_TYPE);
                Permission usePerm = pm.register(
                        SpawnSettingsListener.class, entityTypeKey.getValue(), "Allows spawning a " +  PlainComponentSerializer.plain().serialize(type.asComponent()), spawnEntityPlayerPerm);
                if (checkSetting(event, player.get(), regionsAt, () -> usePerm, (s) -> s.spawn.player.getOrDefault(type, UNDEFINED), UNDEFINED) == FALSE)
                {
                    i18n.send(ChatType.ACTION_BAR, player.get(), CRITICAL, "You are not allowed spawn this here.");
                    return;
                }
            }
            else if (checkSetting(event, null, regionsAt, () -> null, (s) -> s.spawn.naturally.getOrDefault(type, UNDEFINED), UNDEFINED) == FALSE)
            {
                entity.remove();
                return; // natural
            }
        }
    }
}