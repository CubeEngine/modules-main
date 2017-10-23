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
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.spongepowered.api.block.trait.BooleanTraits.DROPPER_TRIGGERED;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.TRUE;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Hostile;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

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

    public SettingsListener(RegionManager manager, Permission base, PermissionManager pm, I18n i18n)
    {
        this.manager = manager;
        this.pm = pm;
        this.i18n = i18n;
        // TODO description
        // TODO ENTER remember last valid pos.
        movePerms.put(MoveType.MOVE, pm.register(SettingsListener.class, "bypass.move.move", "", base));
        movePerms.put(MoveType.EXIT, pm.register(SettingsListener.class, "bypass.move.exit", "", base));
        movePerms.put(MoveType.ENTER, pm.register(SettingsListener.class, "bypass.move.enter", "", base));
        movePerms.put(MoveType.TELEPORT, pm.register(SettingsListener.class, "bypass.move.teleport", "", base));
        buildPerm = pm.register(SettingsListener.class, "bypass.build", "", base);
        useBlockPerm = pm.register(SettingsListener.class, "bypass.use", "", base);
        useItemPerm = pm.register(SettingsListener.class, "bypass.use-item", "", base);
        spawnEntityPlayerPerm = pm.register(SettingsListener.class, "bypass.spawn.player", "", base);
        explodePlayer = pm.register(SettingsListener.class, "bypass.blockdamage.explode.player", "", base);
        command = pm.register(SettingsListener.class, "bypass.command", "", base);
        usePermission.put(UseType.BLOCK, pm.register(SettingsListener.class, "bypass.use-all.block", "", base));
        usePermission.put(UseType.CONTAINER, pm.register(SettingsListener.class, "bypass.use-all.container", "", base));
        usePermission.put(UseType.ITEM, pm.register(SettingsListener.class, "bypass.use-all.item", "", base));
        usePermission.put(UseType.OPEN, pm.register(SettingsListener.class, "bypass.use-all.open", "", base));
        usePermission.put(UseType.REDSTONE, pm.register(SettingsListener.class, "bypass.use-all.redstone", "", base));
        entityDamageAll = pm.register(SettingsListener.class, "bypass.entity-damage.all", "", base);
        entityDamageLiving = pm.register(SettingsListener.class, "bypass.entity-damage.living", "", base);
        playerDamgeAll = pm.register(SettingsListener.class, "bypass.player-damage.all", "", base);
        playerDamgeLiving = pm.register(SettingsListener.class, "bypass.player-damage.living", "", base);
        playerDamgePVP = pm.register(SettingsListener.class, "bypass.player-damage.pvp", "", base);
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

        // Ignore subblock movements
        if (event.getFromTransform().getPosition().toInt().equals(event.getToTransform().getPosition().toInt()))
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
                i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to move in here.");
                return; // Move in from denied
            }

            if (checkMove(event, player, from, to, MoveType.EXIT, true))
            {
                i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to exit this area.");
                return; // Move out of from denied
            }

            if (checkMove(event, player, to, from, MoveType.ENTER, true))
            {
                i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to enter this area.");
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

    public enum UseType
    {
        ITEM, BLOCK, CONTAINER, OPEN, REDSTONE
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
            if (transaction.getOriginal().getState().getType() == transaction.getFinal().getState().getType())
            {
                continue;
            }
            transaction.getOriginal().getLocation().ifPresent(loc -> {
                List<Region> regionsAt = manager.getRegionsAt(loc);
                if (checkSetting(event, player, regionsAt, () -> null, (s) -> s.build, UNDEFINED) == FALSE)
                {
                    i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to build here.");
                    return;
                }
            });
        }
    }

    public Tristate checkSetting(Cancellable event, Player player, List<Region> regionsAt, Supplier<Permission> perm, Function<RegionConfig.Settings, Tristate> func, Tristate defaultTo)
    {
        Permission permission = perm.get();
        if (player != null && permission != null && player.hasPermission(permission.getId()))
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
    public void onUse(InteractBlockEvent.Secondary event, @Root Player player)
    {
        Location<World> loc = event.getTargetBlock().getLocation().orElse(player.getLocation());
        // cause when the player tries to place a block and it cannot the client will not send the clicked location
        List<Region> regionsAt = manager.getRegionsAt(loc);
        BlockType type = event.getTargetBlock().getState().getType();
        ItemStack item = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);

        Permission blockPerm = pm.register(SettingsListener.class, type.getId(), "Allows interacting with a " + type.getTranslation().get() + " Block", useBlockPerm);

        Tristate set = UNDEFINED;
        if (type != BlockTypes.AIR)
        {
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.BLOCK), s -> s.use.all.block, set);
        }
        if (type.getDefaultState().supports(Keys.OPEN))
        {
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.OPEN), s -> s.use.all.open, set);
        }
        if (type.getDefaultState().supports(Keys.POWERED))
        {
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.REDSTONE), s -> s.use.all.redstone, set);
        }

        if (event.getTargetBlock().getLocation().isPresent() && event.getTargetBlock().getLocation().get().getTileEntity().isPresent())
        {
            if (event.getTargetBlock().getLocation().get().getTileEntity().get() instanceof Carrier)
            {
                // TODO check if this is right
                set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.CONTAINER), s -> s.use.all.container, set);
            }
        }

        set = checkSetting(event, player, regionsAt, () -> blockPerm, (s) -> s.use.block.getOrDefault(type, UNDEFINED), set);
        if (set == FALSE)
        {
            i18n.send(ACTION_BAR, player, CRITICAL, "You are not allowed to interact with this here.");
            return;
        }

        if (item != null)
        {
            // Check all items
            set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.ITEM), s -> s.use.all.item, UNDEFINED);
            Permission usePerm = pm.register(SettingsListener.class, item.getType().getId(), "Allows interacting with a " + item.getType().getTranslation().get() + " Item in hand", useItemPerm);
            // Then check individual item
            if (checkSetting(event, player, regionsAt, () -> usePerm, (s) -> s.use.item.getOrDefault(item.getType(), UNDEFINED), set) == FALSE)
            {
                i18n.send(ACTION_BAR, player, CRITICAL, "You are not allowed to use this here.");
            }
        }
    }

    @Listener
    public void onUseItem(InteractItemEvent.Secondary event, @Root Player player)
    {
        ItemType item = event.getItemStack().getType();
        List<Region> regionsAt = manager.getRegionsAt(player.getLocation());
        Permission usePerm = pm.register(SettingsListener.class, item.getId(), "Allows interacting with a " + item.getTranslation().get() + " Item in hand", useItemPerm);
        Tristate set = checkSetting(event, player, regionsAt, () -> usePermission.get(UseType.ITEM), s -> s.use.all.item, UNDEFINED);
        if (checkSetting(event, player, regionsAt, () -> usePerm, (s) -> s.use.item.getOrDefault(item, UNDEFINED), set) == FALSE)
        {
            i18n.send(ACTION_BAR, player, CRITICAL, "You are not allowed to use this here.");
        }
    }

    public enum SpawnType
    {
        NATURALLY, PLAYER, PLUGIN
    }

    @Listener
    public void onSpawn(SpawnEntityEvent event)
    {
        for (Entity entity : event.getEntities())
        {
            Location<World> loc = entity.getLocation();
            EntityType type = entity.getType();
            List<Region> regionsAt = manager.getRegionsAt(loc);
            Cause cause = event.getCause();
            Optional<Player> player = cause.first(Player.class);
            if (event.getCause().getContext().get(EventContextKeys.SPAWN_TYPE).map(t -> t.equals(SpawnTypes.PLUGIN)).orElse(false))
            {
                if (player.isPresent())
                {
                    Permission usePerm = pm.register(SettingsListener.class, type.getId(), "Allows spawning a " + type.getTranslation().get(), spawnEntityPlayerPerm);
                    if (checkSetting(event, player.get(), regionsAt, () -> usePerm, (s) -> s.spawn.player.getOrDefault(type, UNDEFINED), UNDEFINED) == FALSE)
                    {
                        i18n.send(ACTION_BAR, player.get(), CRITICAL, "You are not allowed spawn this here.");
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
                Permission usePerm = pm.register(SettingsListener.class, type.getId(), "Allows spawning a " + type.getTranslation().get(), spawnEntityPlayerPerm);
                if (checkSetting(event, player.get(), regionsAt, () -> usePerm, (s) -> s.spawn.player.getOrDefault(type, UNDEFINED), UNDEFINED) == FALSE)
                {
                    i18n.send(ACTION_BAR, player.get(), CRITICAL, "You are not allowed spawn this here.");
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
        for (Location<World> loc : event.getLocations()) {

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
    public void onChangeBlock(ChangeBlockEvent event)
    {
        Object rootCause = event.getCause().root();
        // Check Explosions first...
        if (event instanceof ExplosionEvent)
        {
            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.getOriginal().getLocation().get());
                this.checkSetting(event, null, regionsAt, () -> null, (s) -> s.blockDamage.allExplosion, UNDEFINED);
                Player player = event.getCause().getContext().get(EventContextKeys.OWNER).filter(p -> p instanceof Player).map(Player.class::cast).orElse(null);
                if (player == null)
                {
                    player = event.getCause().getContext().get(EventContextKeys.IGNITER).filter(p -> p instanceof Player).map(Player.class::cast).orElse(null);
                }
                if (rootCause instanceof Player)
                {
                    player = ((Player) rootCause);
                }
                if (player != null)
                {
                    this.checkSetting(event, player, regionsAt, () -> explodePlayer, (s) -> s.blockDamage.playerExplosion, UNDEFINED);
                    if (event.isCancelled())
                    {
                        i18n.send(ACTION_BAR, player, CRITICAL, "You are not allowed to let stuff explode here!");
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
    }

    @Listener
    public void onCommand(SendCommandEvent event, @Root Player player)
    {
        CommandMapping mapping = Sponge.getGame().getCommandManager().get(event.getCommand()).orElse(null);
        if (mapping == null)
        {
            return;
        }
        List<Region> regionsAt = manager.getRegionsAt(player.getLocation());
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
            i18n.send(ACTION_BAR, player, CRITICAL, "You are not allowed to execute this command here!");
        }
    }

    private boolean isRedstoneChange(BlockState block)
    {
        return block.get(Keys.POWERED).isPresent() // Levers etc.
            || block.get(Keys.POWER).isPresent() // Redstone
            || block.getType() == BlockTypes.REDSTONE_LAMP // Lamp
            || block.getType() == BlockTypes.LIT_REDSTONE_LAMP
            || block.getType() == BlockTypes.POWERED_REPEATER // Repeater
            || block.getType() == BlockTypes.UNPOWERED_REPEATER
            || block.getType() == BlockTypes.POWERED_COMPARATOR // Comparator
            || block.getType() == BlockTypes.UNPOWERED_COMPARATOR
            || block.get(Keys.OPEN).isPresent() // Doors etc.
            || block.get(Keys.EXTENDED).isPresent() // Pistons
            || block.getTraitValue(DROPPER_TRIGGERED).isPresent() // Dropper / Dispenser
            || block.getType() == BlockTypes.TNT // Tnt
                // TODO other activateable blocks
                ;
    }

    @Listener
    public void onRedstoneChangeNotify(NotifyNeighborBlockEvent event, @Root LocatableBlock block)
    {
        for (Map.Entry<Direction, BlockState> entry : event.getOriginalNeighbors().entrySet())
        {
            Location<World> loc = block.getLocation().getRelative(entry.getKey());
            List<Region> regionsAt = manager.getRegionsAt(loc);
            if (isRedstoneChange(entry.getValue()))
            {
                // Redstone is disabled entirely?
                if (checkSetting(event, null, regionsAt, () -> null, s -> s.deadCircuit, UNDEFINED) == FALSE)
                {
                    return;
                }
                // Redstone is disabled for player?
                Optional<Player> player = event.getCause().getContext().get(EventContextKeys.NOTIFIER).filter(p -> p instanceof Player).map(Player.class::cast);
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
        Entity target = event.getTargetEntity();
        if (target instanceof Player)
        {
            onPlayerDamage(event, ((Player) target));
        }
        else
        {
            onMobDamage(event, target);
        }
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
        Player playerSource = null;
        if (entitySource instanceof Player)
        {
            playerSource = ((Player) entitySource);
        }

        List<Region> regionsAt = manager.getRegionsAt(target.getLocation());

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

    private void onPlayerDamage(DamageEntityEvent event, Player target)
    {
        Entity entitySource = getEntitySource(event);

        List<Region> regionsAt = manager.getRegionsAt(target.getLocation());

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