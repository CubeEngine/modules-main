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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Hostile;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.weather.LightningBolt;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.ai.SetAITargetEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.spongepowered.api.util.Tristate.*;

@Singleton
public class SettingsListener
{

    private RegionManager manager;
    private PermissionManager pm;
    private I18n i18n;
    public final Map<MoveType, Permission> movePerms = new HashMap<>();
    public final Permission buildPerm;

    public final Permission useBlockPerm;
    public final Permission useItemPerm;
    public final Permission spawnEntityPlayerPerm;
    public final Permission explodePlayer;
    public final Permission command;
    public final Map<UseType, Permission> usePermission = new HashMap<>();
    public final Permission entityDamageAll;
    public final Permission entityDamageLiving;
    public final Permission playerDamgeAll;
    public final Permission playerDamgeLiving;
    public final Permission playerDamgePVP;
    public final Permission playerTargeting;

    @Inject
    public SettingsListener(RegionManager manager, Permission base, PermissionManager pm, I18n i18n)
    {
        this.manager = manager;
        this.pm = pm;
        this.i18n = i18n;
        // TODO description
        // TODO ENTER remember last valid pos.
        movePerms.put(MoveType.MOVE, pm.register(SettingsListener.class, "bypass.move.move", "Region bypass for moving in a region", base));
        movePerms.put(MoveType.EXIT, pm.register(SettingsListener.class, "bypass.move.exit", "Region bypass for exiting a region", base));
        movePerms.put(MoveType.ENTER, pm.register(SettingsListener.class, "bypass.move.enter", "Region bypass for entering a region", base));
        movePerms.put(MoveType.TELEPORT, pm.register(SettingsListener.class, "bypass.move.teleport", "Region bypass for teleport in a region", base));
        movePerms.put(MoveType.TELEPORT_PORTAL, pm.register(SettingsListener.class, "bypass.move.teleport-portal", "Region bypass for teleport using portals in a region", base));
        buildPerm = pm.register(SettingsListener.class, "bypass.build", "Region bypass for building", base);
        useBlockPerm = pm.register(SettingsListener.class, "bypass.use", "Region bypass for using anything", base);
        useItemPerm = pm.register(SettingsListener.class, "bypass.use-item", "Region bypass for using items", base);
        spawnEntityPlayerPerm = pm.register(SettingsListener.class, "bypass.spawn.player", "Region bypass for players spawning entities", base);
        explodePlayer = pm.register(SettingsListener.class, "bypass.blockdamage.explode.player", "Region bypass for players causing blockdamage with explosions", base);
        command = pm.register(SettingsListener.class, "bypass.command", "Region bypass for using all commands", base);
        usePermission.put(UseType.BLOCK, pm.register(SettingsListener.class, "bypass.use-all.block", "Region bypass for using blocks", base));
        usePermission.put(UseType.CONTAINER, pm.register(SettingsListener.class, "bypass.use-all.container", "Region bypass for using containers", base));
        usePermission.put(UseType.ITEM, pm.register(SettingsListener.class, "bypass.use-all.item", "Region bypass for using items", base));
        usePermission.put(UseType.OPEN, pm.register(SettingsListener.class, "bypass.use-all.open", "Region bypass for opening anything", base));
        usePermission.put(UseType.REDSTONE, pm.register(SettingsListener.class, "bypass.use-all.redstone", "Region bypass for using redstone", base));
        entityDamageAll = pm.register(SettingsListener.class, "bypass.entity-damage.all", "", base);
        entityDamageLiving = pm.register(SettingsListener.class, "bypass.entity-damage.living", "", base);
        playerDamgeAll = pm.register(SettingsListener.class, "bypass.player-damage.all", "", base);
        playerDamgeLiving = pm.register(SettingsListener.class, "bypass.player-damage.living", "", base);
        playerDamgePVP = pm.register(SettingsListener.class, "bypass.player-damage.pvp", "", base);
        playerTargeting = pm.register(SettingsListener.class, "bypass.player-targeting", "", base);
    }

    @Listener
    public void onMove(MoveEntityEvent event, @Getter("getEntity") ServerPlayer player)
    {
        List<Region> from = manager.getRegionsAt(player.getWorld().getLocation(event.getOriginalPosition()));
        List<Region> to = manager.getRegionsAt(player.getWorld().getLocation(event.getDestinationPosition()));
        if (from.isEmpty() && to.isEmpty())
        {
            return;
        }

        // Ignore subblock movements
        if (event.getDestinationPosition().toInt().equals(event.getOriginalPosition()))
        {
            return;
        }

        if (event instanceof ChangeEntityWorldEvent.Reposition)
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
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to move in here.");
                return; // Move in from denied
            }

            if (checkMove(event, player, from, to, MoveType.EXIT, true))
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to exit this area.");
                return; // Move out of from denied
            }

            if (checkMove(event, player, to, from, MoveType.ENTER, true))
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to enter this area.");
                return; // Move into to denied
            }
        }
    }

    private boolean checkMove(MoveEntityEvent event, @Root ServerPlayer player, List<Region> source, List<Region> dest, MoveType type, boolean contain)
    {
        Permission perm = this.movePerms.get(type);
        if (!perm.check(player))
        {
            Tristate allow = UNDEFINED;
            for (Region region : source)
            {
                allow = allow.and(region.getSettings().move.getOrDefault(type, UNDEFINED));
                if (allow != UNDEFINED)
                {
                    if (allow == FALSE)
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

    public enum SpawnType
    {
        NATURALLY, PLAYER, PLUGIN
    }

    public enum UseType
    {
        ITEM, BLOCK, CONTAINER, OPEN, REDSTONE
    }

    public enum MoveType
    {
        MOVE, ENTER, EXIT, TELEPORT, TELEPORT_PORTAL
    }

    @Listener
    public void onPreBuild(ChangeBlockEvent.Pre event, @First ServerPlayer player)
    {
        if (this.buildPerm.check(player))
        {
            return;
        }

        for (ServerLocation loc : event.getLocations())
        {
            List<Region> regionsAt = manager.getRegionsAt(loc);
            if (checkSetting(event, player, regionsAt, () -> null, (s) -> s.build, UNDEFINED) == FALSE)
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to build here.");
                return;
            }
        }
    }

    @Listener
    public void onBuild(ChangeBlockEvent.All event, @First ServerPlayer player)
    {
        if (this.buildPerm.check(player))
        {
            return;
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions())
        {
            if (transaction.getOriginal().getState().getType() == transaction.getFinal().getState().getType())
            {
                continue;
            }
            transaction.getOriginal().getLocation().ifPresent(loc -> {
                List<Region> regionsAt = manager.getRegionsAt(loc);
                if (checkSetting(event, player, regionsAt, () -> null, (s) -> s.build, UNDEFINED) == FALSE)
                {
                    i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to build here.");
                    return;
                }
            });
        }
    }

    public Tristate checkSetting(Cancellable event, ServerPlayer player, List<Region> regionsAt, Supplier<Permission> perm, Function<RegionConfig.Settings, Tristate> func, Tristate defaultTo)
    {
        Permission permission = perm.get();
        if (player != null && permission != null && permission.check(player))
        {
            event.setCancelled(false);
            return Tristate.TRUE;
        }
        Tristate allow = UNDEFINED;
        for (Region region : regionsAt)
        {
            allow = allow.and(func.apply(region.getSettings()));
            if (allow != UNDEFINED)
            {
                if (allow == FALSE)
                {
                    event.setCancelled(true);
                    return FALSE;
                }
                break;
            }
        }
        if (allow == TRUE)
        {
            event.setCancelled(false);
        }
        if (allow == UNDEFINED)
        {
            return defaultTo;
        }
        return allow;
    }

    @Listener
    public void onUse(InteractBlockEvent.Secondary event, @Root ServerPlayer player)
    {
        final ServerLocation loc = player.getWorld().getLocation(event.getInteractionPoint());
        // cause when the player tries to place a block and it cannot the client will not send the clicked location
        List<Region> regionsAt = manager.getRegionsAt(loc);
        BlockType type = event.getBlock().getState().getType();
        ItemStack item = player.getItemInHand(HandTypes.MAIN_HAND);

        final ResourceKey typeKey = Sponge.getGame().registries().registry(RegistryTypes.BLOCK_TYPE).valueKey(type);
        Permission blockPerm = pm.register(SettingsListener.class, typeKey.getValue(), "Allows interacting with a " + PlainComponentSerializer.plain().serialize(type.asComponent()) + " Block", useBlockPerm);

        Tristate set = UNDEFINED;
        if (type != BlockTypes.AIR.get())
        {
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.BLOCK), s -> s.use.all.block, set);
        }
        if (type.getDefaultState().supports(Keys.IS_OPEN))
        {
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.OPEN), s -> s.use.all.open, set);
        }
        if (type.getDefaultState().supports(Keys.IS_POWERED))
        {
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.REDSTONE), s -> s.use.all.redstone, set);
        }

        if (loc.getBlockEntity().isPresent())
        {
            final BlockEntity blockEntity = loc.getBlockEntity().get();
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
            final ResourceKey itemKey = Sponge.getGame().registries().registry(RegistryTypes.ITEM_TYPE).valueKey(item.getType());
            Permission usePerm = pm.register(SettingsListener.class, itemKey.getValue(), "Allows interacting with a " + PlainComponentSerializer.plain().serialize(item.getType().asComponent()) + " Item in hand", useItemPerm);
            // Then check individual item
            if (checkSetting(event, player, regionsAt, () -> usePerm, (s) -> s.use.item.getOrDefault(item.getType(), UNDEFINED), set) == FALSE)
            {
                i18n.send(ChatType.ACTION_BAR, player, CRITICAL, "You are not allowed to use this here.");
            }
        }
    }

    @Listener
    public void onUseItem(InteractItemEvent.Secondary event, @Root ServerPlayer player)
    {
        ItemType item = event.getItemStack().getType();
        List<Region> regionsAt = manager.getRegionsAt(player.getServerLocation());
        final ResourceKey itemKey = Sponge.getGame().registries().registry(RegistryTypes.ITEM_TYPE).valueKey(item);
        Permission usePerm = pm.register(SettingsListener.class, itemKey.getValue(), "Allows interacting with a " + PlainComponentSerializer.plain().serialize(item.asComponent()) + " Item in hand", useItemPerm);
        Tristate set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.ITEM), s -> s.use.all.item, UNDEFINED);
        if (checkSetting(event, player, regionsAt, () -> usePerm, (s) -> s.use.item.getOrDefault(item, UNDEFINED), set) == FALSE)
        {
            i18n.send(ChatType.ACTION_BAR, player, CRITICAL, "You are not allowed to use this here.");
        }
    }

    @Listener
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
                    final ResourceKey entityTypeKey = Sponge.getGame().registries().registry(RegistryTypes.ENTITY_TYPE).valueKey(type);
                    Permission usePerm = pm.register(SettingsListener.class, entityTypeKey.getValue(), "Allows spawning a " + PlainComponentSerializer.plain().serialize(type.asComponent()), spawnEntityPlayerPerm);
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
                final ResourceKey entityTypeKey = Sponge.getGame().registries().registry(RegistryTypes.ENTITY_TYPE).valueKey(type);
                Permission usePerm = pm.register(SettingsListener.class, entityTypeKey.getValue(), "Allows spawning a " +  PlainComponentSerializer.plain().serialize(type.asComponent()), spawnEntityPlayerPerm);
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

    @Listener
    public void onPreChangeBlock(ChangeBlockEvent.Pre event, @Root LocatableBlock block)
    {
        for (ServerLocation loc : event.getLocations()) {

            if (loc.getBlockType() != BlockTypes.AIR && loc.getBlockType() != block.getBlockState().getType()) {
                List<Region> regionsAt = manager.getRegionsAt(loc);
                if (this.checkSetting(event, null, regionsAt, () -> null, s -> s.blockDamage.block.getOrDefault(block.getBlockState().getType() , UNDEFINED), UNDEFINED) == FALSE)
                {
                    return;
                }
            }
        }
    }

    @Listener
    public void onChangeBlock(ChangeBlockEvent.All event)
    {
        Object rootCause = event.getCause().root();
        // Check Explosions first...
        if (event instanceof ExplosionEvent)
        {
            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.getOriginal().getLocation().get());
                this.checkSetting(event, null, regionsAt, () -> null, (s) -> s.blockDamage.allExplosion, UNDEFINED);
                ServerPlayer player = event.getCause().getContext().get(EventContextKeys.CREATOR).filter(p -> p instanceof ServerPlayer).map(ServerPlayer.class::cast).orElse(null);
                if (player == null)
                {
                    player = event.getCause().getContext().get(EventContextKeys.IGNITER).filter(p -> p instanceof ServerPlayer).map(ServerPlayer.class::cast).orElse(null);
                }
                if (rootCause instanceof ServerPlayer)
                {
                    player = ((ServerPlayer) rootCause);
                }
                if (player != null)
                {
                    this.checkSetting(event, player, regionsAt, () -> explodePlayer, (s) -> s.blockDamage.playerExplosion, UNDEFINED);
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
            return;
        }

        // Hostile Mob causes BlockChange?
        if (rootCause instanceof Hostile)
        {
            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.getOriginal().getLocation().get());
                if (this.checkSetting(event, null, regionsAt, () -> null, (s) -> s.blockDamage.monster, UNDEFINED) == FALSE)
                {
                    return;
                }
            }
        }

        // Block causes BlockChange?
        if (rootCause instanceof LocatableBlock)
        {
            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.getOriginal().getLocation().get());
                if (this.checkSetting(event, null, regionsAt, () -> null, s -> s.blockDamage.block.getOrDefault(((LocatableBlock) rootCause).getBlockState().getType() , UNDEFINED), UNDEFINED) == FALSE)
                {
                    return;
                }
            }
        }

        if (rootCause instanceof LightningBolt)
        {
            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.getOriginal().getLocation().get());
                if (this.checkSetting(event, null, regionsAt, () -> null, s -> s.blockDamage.lightning, UNDEFINED) == FALSE)
                {
                    return;
                }
            }
        }
    }

    @Listener
    public void onCommand(ExecuteCommandEvent.Pre event, @Root ServerPlayer player)
    {
        CommandMapping mapping = Sponge.getGame().getCommandManager().getCommandMapping(event.getCommand()).orElse(null);
        if (mapping == null)
        {
            return;
        }
        List<Region> regionsAt = manager.getRegionsAt(player.getServerLocation());
        if (this.checkSetting(event, player, regionsAt, () -> command, s -> {

            Tristate value = UNDEFINED;
            // TODO subcommands?
            // TODO register commands as aliascommand
            for (String alias : mapping.getAllAliases())
            {
                value = value.and(s.blockedCommands.getOrDefault(alias.toLowerCase(), UNDEFINED));
                if (value != UNDEFINED)
                {
                    break;
                }
            }
            return value;
        }, UNDEFINED) == FALSE)
        {
            i18n.send(ChatType.ACTION_BAR, player, CRITICAL, "You are not allowed to execute this command here!");
        }
    }

    private boolean isRedstoneChange(BlockState block)
    {
        // TODO check if this can be simplified after the great flattening
        return block.get(Keys.IS_POWERED).isPresent() // Levers etc.
            || block.get(Keys.POWER).isPresent() // Redstone
            || block.getType() == BlockTypes.REDSTONE_LAMP.get() // Lamp
            || block.getType() == BlockTypes.REPEATER.get() // Repeater
            || block.getType() == BlockTypes.COMPARATOR.get() // Comparator
            || block.get(Keys.IS_OPEN).isPresent() // Doors etc.
            || block.get(Keys.IS_EXTENDED).isPresent() // Pistons
//            || block.get(Keys.IS_TRIGGERED).isPresent() // Dropper / Dispenser TODO check if this is now IS_POWERED?
            || block.getType() == BlockTypes.TNT.get() // Tnt
                // TODO other activateable blocks
                ;
    }

    @Listener
    public void onRedstoneChangeNotify(NotifyNeighborBlockEvent event, @Root LocatableBlock block)
    {
        for (Map.Entry<Direction, BlockState> entry : event.getOriginalNeighbors().entrySet())
        {
            ServerLocation loc = block.getServerLocation().relativeTo(entry.getKey());
            List<Region> regionsAt = manager.getRegionsAt(loc);
            if (isRedstoneChange(entry.getValue()))
            {
                // Redstone is disabled entirely?
                if (checkSetting(event, null, regionsAt, () -> null, s -> s.deadCircuit, UNDEFINED) == FALSE)
                {
                    return;
                }
                if (true)
                {
                    // TODO this check is spammed way too often (also with wrong notifier mabye?)
                    return;
                }
                // Redstone is disabled for player?
                Optional<ServerPlayer> player = event.getCause().getContext().get(EventContextKeys.NOTIFIER).filter(p -> p instanceof ServerPlayer).map(ServerPlayer.class::cast);
                if (player.isPresent())
                {
                    if (checkSetting(event, player.get(), regionsAt, () -> usePermission.get(UseType.REDSTONE), s -> s.use.all.redstone, UNDEFINED) == FALSE)
                    {
                        return;
                    }
                }
            }
        }
    }

    @Listener
    public void onEntityDamage(DamageEntityEvent event)
    {
        Entity target = event.getEntity();
        if (target instanceof ServerPlayer)
        {
            onPlayerDamage(event, ((ServerPlayer) target));
        }
        else
        {
            onMobDamage(event, target);
        }
    }

    @Listener
    public void onTargetPlayer(SetAITargetEvent event, @Getter("getTarget") ServerPlayer player)
    {
        List<Region> regions = manager.getRegionsAt(player.getServerLocation());
        checkSetting(event, player, regions, () -> this.playerTargeting, s -> s.playerDamage.aiTargeting, UNDEFINED);
    }

    private Entity getEntitySource(DamageEntityEvent event)
    {
        DamageSource source = event.getCause().first(DamageSource.class).get();
        Entity entitySource = null;
        if (source instanceof EntityDamageSource)
        {
            entitySource = ((EntityDamageSource) source).getSource();
            if (source instanceof IndirectEntityDamageSource)
            {
                entitySource = ((IndirectEntityDamageSource) source).getIndirectSource();
            }
        }
        return entitySource;
    }

    private void onMobDamage(DamageEntityEvent event, Entity target)
    {
        Entity entitySource = getEntitySource(event);
        ServerPlayer playerSource = null;
        if (entitySource instanceof ServerPlayer)
        {
            playerSource = ((ServerPlayer) entitySource);
        }

        List<Region> regionsAt = manager.getRegionsAt(target.getServerLocation());

        Tristate defaultTo = this.checkSetting(event, playerSource, regionsAt, () -> entityDamageAll, s -> s.entityDamage.all, UNDEFINED);

        if (entitySource instanceof Living)
        {
            defaultTo = this.checkSetting(event, playerSource, regionsAt, () -> entityDamageLiving, s -> s.entityDamage.byLiving, defaultTo);
        }
        if (entitySource != null)
        {
            EntityType type = entitySource.getType();
            this.checkSetting(event, null, regionsAt, () -> null, s -> s.entityDamage.byEntity.getOrDefault(type, UNDEFINED), defaultTo);
        }
    }

    private void onPlayerDamage(DamageEntityEvent event, ServerPlayer target)
    {
        Entity entitySource = getEntitySource(event);

        List<Region> regionsAt = manager.getRegionsAt(target.getServerLocation());

        Tristate defaultTo = this.checkSetting(event, target, regionsAt, () -> playerDamgeAll, s -> s.playerDamage.all, UNDEFINED);

        if (entitySource instanceof Living)
        {
            defaultTo = this.checkSetting(event, target, regionsAt, () -> playerDamgeLiving, s -> s.playerDamage.byLiving, defaultTo);
        }
        if (entitySource instanceof Player)
        {
            this.checkSetting(event, target, regionsAt, () -> playerDamgePVP, s -> s.playerDamage.pvp, defaultTo);
        }
    }
}