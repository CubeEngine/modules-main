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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.module.protector.listener.SettingsListener.checkSetting;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class InteractSettingsListener extends PermissionContainer
{

    private final RegionManager manager;
    private final PermissionManager pm;
    private final I18n i18n;

    public final Permission useBlockPerm;
    public final Permission useItemPerm;
    public final Map<UseType, Permission> usePermission = new HashMap<>();

    @Inject
    public InteractSettingsListener(RegionManager manager, PermissionManager pm, I18n i18n)
    {
        super(pm, Protector.class);
        this.manager = manager;
        this.pm = pm;
        this.i18n = i18n;
        useBlockPerm = this.register("bypass.use", "Region bypass for using anything");
        useItemPerm = this.register("bypass.use-item", "Region bypass for using items");

        usePermission.put(UseType.BLOCK, this.register("bypass.use-all.block", "Region bypass for using blocks"));
        usePermission.put(UseType.CONTAINER, this.register("bypass.use-all.container", "Region bypass for using containers"));
        usePermission.put(UseType.ITEM, this.register("bypass.use-all.item", "Region bypass for using items"));
        usePermission.put(UseType.OPEN, this.register("bypass.use-all.open", "Region bypass for opening anything"));
        usePermission.put(UseType.REDSTONE, this.register("bypass.use-all.redstone", "Region bypass for using redstone"));
        usePermission.put(UseType.ENTITY, this.register("bypass.use-all.entity", "Region bypass for using entities"));
    }

    @Listener
    public void onEntityUse(InteractEntityEvent.Secondary event, @Root ServerPlayer player)
    {
        List<Region> regionsAt = manager.getRegionsAt(player.serverLocation());
        if (checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.ENTITY), (s) -> s.use.all.entity, UNDEFINED) == FALSE)
        {
            i18n.send(ChatType.ACTION_BAR, player, CRITICAL, "You are not allowed to interact with this entity here.");
        }
    }

// TODO event not called
//    @Listener
//    public void onEntityUse(CollideEntityEvent event, @Root ServerPlayer player)
//    {
//        List<Region> regionsAt = manager.getRegionsAt(player.getServerLocation());
//        Tristate set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.ENTITY), s -> s.use.all.item, UNDEFINED);
//        checkSetting(event, player, regionsAt, () -> null, (s) -> s.use.all.entity, set); // no message
//    }

    @Listener(order = Order.EARLY)
    public void onUseItem(InteractItemEvent.Secondary event, @Root ServerPlayer player)
    {
        ItemType item = event.itemStack().type();
        List<Region> regionsAt = manager.getRegionsAt(player.serverLocation());
        final ResourceKey itemKey = item.key(RegistryTypes.ITEM_TYPE);
        Permission usePerm = pm.register(SettingsListener.class, itemKey.value(), "Allows interacting with a " + PlainComponentSerializer.plain().serialize(item.asComponent()) + " Item in hand", useItemPerm);
        Tristate set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.ITEM), s -> s.use.all.item, UNDEFINED);
        if (checkSetting(event, player, regionsAt, () -> usePerm, (s) -> s.use.item.getOrDefault(item, UNDEFINED), set) == FALSE)
        {
            i18n.send(ChatType.ACTION_BAR, player, CRITICAL, "You are not allowed to use this here.");
        }
    }


    @Listener(order = Order.EARLY)
    public void onUse(InteractBlockEvent.Secondary event, @Root ServerPlayer player)
    {
        final ServerLocation loc = player.world().location(event.interactionPoint());
        // cause when the player tries to place a block and it cannot the client will not send the clicked location
        List<Region> regionsAt = manager.getRegionsAt(loc);
        BlockType type = event.block().state().type();
        ItemStack item = player.itemInHand(HandTypes.MAIN_HAND);

        final ResourceKey typeKey = type.key(RegistryTypes.BLOCK_TYPE);
        Permission blockPerm = pm.register(SettingsListener.class, typeKey.value(), "Allows interacting with a " + PlainComponentSerializer.plain().serialize(type.asComponent()) + " Block", useBlockPerm);

        Tristate set = UNDEFINED;
        if (type != BlockTypes.AIR.get())
        {
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.BLOCK), s -> s.use.all.block, set);
        }
        if (type.defaultState().supports(Keys.IS_OPEN))
        {
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.OPEN), s -> s.use.all.open, set);
        }
        if (type.defaultState().supports(Keys.IS_POWERED))
        {
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.REDSTONE), s -> s.use.all.redstone, set);
        }

        if (loc.blockEntity().isPresent())
        {
            final BlockEntity blockEntity = loc.blockEntity().get();
            if (blockEntity instanceof Carrier)
            {
                // TODO check if this is right
                set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.CONTAINER), s -> s.use.all.container, set);
            }
        }

        set = checkSetting(event, player, regionsAt, () -> blockPerm, (s) -> s.use.block.getOrDefault(type, UNDEFINED), set);
        if (set == FALSE)
        {
            i18n.send(ChatType.ACTION_BAR, player, CRITICAL, "You are not allowed to interact with this here.");
            return;
        }

        if (item != null)
        {
            // Check all items
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.ITEM), s -> s.use.all.item, UNDEFINED);
            final ResourceKey itemKey = item.type().key(RegistryTypes.ITEM_TYPE);
            Permission usePerm = pm.register(SettingsListener.class, itemKey.value(), "Allows interacting with a " + PlainComponentSerializer.plain().serialize(item.type().asComponent()) + " Item in hand", useItemPerm);
            // Then check individual item
            if (checkSetting(event, player, regionsAt, () -> usePerm, (s) -> s.use.item.getOrDefault(item.type(), UNDEFINED), set) == FALSE)
            {
                i18n.send(ChatType.ACTION_BAR, player, CRITICAL, "You are not allowed to use this here.");
            }
        }
    }


//    @Listener(order = Order.EARLY)
//    public void onRedstoneChangeNotify(NotifyNeighborBlockEvent event, @Root LocatableBlock block)
//    {
//        for (Map.Entry<Direction, BlockState> entry : event.originalNeighbors().entrySet())
//        {
//            ServerLocation loc = block.serverLocation().relativeTo(entry.getKey());
//            List<Region> regionsAt = manager.getRegionsAt(loc);
//            if (isRedstoneChange(entry.getValue()))
//            {
//                // Redstone is disabled entirely?
//                if (checkSetting(event, null, regionsAt, () -> null, s -> s.deadCircuit, UNDEFINED) == FALSE)
//                {
//                    return;
//                }
//                if (true)
//                {
//                    // TODO this check is spammed way too often (also with wrong notifier mabye?)
//                    return;
//                }
//                // Redstone is disabled for player?
//                Optional<ServerPlayer> player = event.cause().context().get(EventContextKeys.NOTIFIER).filter(p -> p instanceof ServerPlayer).map(ServerPlayer.class::cast);
//                if (player.isPresent())
//                {
//                    if (checkSetting(event, player.get(), regionsAt, () -> usePermission.get(InteractSettingsListener.UseType.REDSTONE), s -> s.use.all.redstone, UNDEFINED) == FALSE)
//                    {
//                        return;
//                    }
//                }
//            }
//        }
//    }


    private boolean isRedstoneChange(BlockState block)
    {
        // TODO check if this can be simplified after the great flattening
        return block.get(Keys.IS_POWERED).isPresent() // Levers etc.
            || block.get(Keys.POWER).isPresent() // Redstone
            || block.type().isAnyOf(BlockTypes.REDSTONE_LAMP, BlockTypes.REPEATER, BlockTypes.COMPARATOR)
            || block.get(Keys.IS_OPEN).isPresent() // Doors etc.
            || block.get(Keys.IS_EXTENDED).isPresent() // Pistons
//            || block.get(Keys.IS_TRIGGERED).isPresent() // Dropper / Dispenser TODO check if this is now IS_POWERED?
            || block.type().isAnyOf(BlockTypes.TNT)
            // TODO other activateable blocks
            ;
    }


    public enum UseType
    {
        ITEM, BLOCK, CONTAINER, OPEN, REDSTONE, ENTITY
    }
}