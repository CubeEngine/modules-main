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
package org.cubeengine.module.protector.listener;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.inject.Provider;

public class PlayerSettingsListener
{


    private RegionManager manager;
    private PermissionManager pm;
    private I18n i18n;

    public final Map<MoveType, Permission> movePerms = new HashMap<>();
    public final Permission buildPerm;
    public final Permission useBlockPerm;
    public final Permission useItemPerm;

    public PlayerSettingsListener(RegionManager manager, Permission base, PermissionManager pm, I18n i18n)
    {
        this.manager = manager;
        this.pm = pm;
        this.i18n = i18n;
        // TODO description
        movePerms.put(MoveType.MOVE, pm.register(PlayerSettingsListener.class, "bypass.move.move", "", base));
        movePerms.put(MoveType.EXIT, pm.register(PlayerSettingsListener.class, "bypass.move.exit", "", base));
        movePerms.put(MoveType.ENTER, pm.register(PlayerSettingsListener.class, "bypass.move.enter", "", base));
        movePerms.put(MoveType.TELEPORT, pm.register(PlayerSettingsListener.class, "bypass.move.teleport", "", base));
        buildPerm = pm.register(PlayerSettingsListener.class, "bypass.build", "", base);
        useBlockPerm = pm.register(PlayerSettingsListener.class, "bypass.use", "", base);
        useItemPerm = pm.register(PlayerSettingsListener.class, "bypass.use-item", "", base);
    }

    @Listener
    public void onMove(MoveEntityEvent event, @Getter("getTargetEntity") Player player)
    {
        List<Region> from = manager.getRegionsAt(event.getFromTransform().getLocation());
        List<Region> to = manager.getRegionsAt(event.getToTransform().getLocation());
        if (from.isEmpty() && to.isEmpty())
        {
            return;
        }

        if (event instanceof MoveEntityEvent.Teleport)
        {
            if (checkMove(event, player, from, to, MoveType.TELEPORT, false))
            {
                return; // Teleport out denied
            }

            if (checkMove(event, player, to, from, MoveType.TELEPORT, false))
            {
                return; // Teleport in denied
            }
        }
        else if (event.getCause().root() instanceof Player)
        {
            if (checkMove(event, player, from, to, MoveType.MOVE, false))
            {
                i18n.sendTranslated(ACTION_BAR, player, NEGATIVE, "You are not allowed to move in here.");
                return; // Move in from denied
            }

            if (checkMove(event, player, from, to, MoveType.EXIT, true))
            {
                i18n.sendTranslated(ACTION_BAR, player, NEGATIVE, "You are not allowed to exit this area.");
                return; // Move out of from denied
            }

            if (checkMove(event, player, to, from, MoveType.ENTER, true))
            {
                i18n.sendTranslated(ACTION_BAR, player, NEGATIVE, "You are not allowed to enter this area.");
                return; // Move into to denied
            }
        }
    }

    private boolean checkMove(MoveEntityEvent event, @Root Player player, List<Region> source, List<Region> dest, MoveType type, boolean contain)
    {
        Permission perm = this.movePerms.get(type);
        if (!player.hasPermission(perm.getId()))
        {
            Tristate allow = UNDEFINED;
            for (Region region : source)
            {
                allow = allow.and(region.getSettings().move.getOrDefault(type, UNDEFINED));
                if (allow != UNDEFINED)
                {
                    if (allow == Tristate.FALSE)
                    {
                        if (!contain || !dest.contains(region))
                        {
                            event.setCancelled(true);
                            return true;
                        }
                    }
                    if (!contain)
                    {
                        break;
                    }
                }
            }
        }
        return false;
    }

    public enum MoveType
    {
        MOVE, ENTER, EXIT, TELEPORT
    }

    @Listener
    public void onBuild(ChangeBlockEvent event, @Root Player player)
    {
        if (player.hasPermission(this.buildPerm.getId()))
        {
            return;
        }
        for (Transaction<BlockSnapshot> transaction : event.getTransactions())
        {
            transaction.getOriginal().getLocation().ifPresent(loc -> {
                List<Region> regionsAt = manager.getRegionsAt(loc);
                if (checkSetting(event, player, regionsAt, () -> null, (s) -> s.build))
                {
                    i18n.sendTranslated(ACTION_BAR, player, NEGATIVE, "You are not allowed to build here.");
                    return;
                }
            });
        }
    }

    public boolean checkSetting(Cancellable event, Player player, List<Region> regionsAt, Supplier<Permission> perm, Function<RegionConfig.Settings, Tristate> func)
    {
        Permission permission = perm.get();
        if (permission != null && player.hasPermission(permission.getId()))
        {
            return false;
        }
        Tristate allow = UNDEFINED;
        for (Region region : regionsAt)
        {
            allow = allow.and(func.apply(region.getSettings()));
            if (allow != UNDEFINED)
            {
                if (allow == Tristate.FALSE)
                {
                    event.setCancelled(true);
                    return true;
                }
                break;
            }
        }
        return false;
    }

    @Listener
    public void onUse(InteractBlockEvent.Secondary event, @Root Player player)
    {
        Location<World> loc = event.getTargetBlock().getLocation().get();

        List<Region> regionsAt = manager.getRegionsAt(loc);
        BlockType type = event.getTargetBlock().getState().getType();
        ItemStack item = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);

        Permission blockPerm = pm.register(PlayerSettingsListener.class, type.getId(), "Allows interacting with a " + type.getTranslation().get() + " Block", useBlockPerm);

        if (checkSetting(event, player, regionsAt, () -> blockPerm, (s) -> s.blockUsage.block.getOrDefault(type, UNDEFINED)))
        {
            i18n.sendTranslated(ACTION_BAR, player, NEGATIVE, "You are not allowed interact with that here.");
            return;
        }

        if (item != null)
        {
            Permission usePerm = pm.register(PlayerSettingsListener.class, type.getId(), "Allows interacting with a " + type.getTranslation().get() + " Item in hand", useItemPerm);
            if (checkSetting(event, player, regionsAt, () -> usePerm, (s) -> s.blockUsage.item.getOrDefault(item, UNDEFINED)))
            {
                i18n.sendTranslated(ACTION_BAR, player, NEGATIVE, "You are not allowed use that here.");
            }
        }
    }
}
