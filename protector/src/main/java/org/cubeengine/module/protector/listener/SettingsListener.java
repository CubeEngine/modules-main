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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
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
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Hostile;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

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
    public final Permission spawnEntityPluginPerm;
    public final Permission explodePlayer;

    public SettingsListener(RegionManager manager, Permission base, PermissionManager pm, I18n i18n)
    {
        this.manager = manager;
        this.pm = pm;
        this.i18n = i18n;
        // TODO description
        movePerms.put(MoveType.MOVE, pm.register(SettingsListener.class, "bypass.move.move", "", base));
        movePerms.put(MoveType.EXIT, pm.register(SettingsListener.class, "bypass.move.exit", "", base));
        movePerms.put(MoveType.ENTER, pm.register(SettingsListener.class, "bypass.move.enter", "", base));
        movePerms.put(MoveType.TELEPORT, pm.register(SettingsListener.class, "bypass.move.teleport", "", base));
        buildPerm = pm.register(SettingsListener.class, "bypass.build", "", base);
        useBlockPerm = pm.register(SettingsListener.class, "bypass.use", "", base);
        useItemPerm = pm.register(SettingsListener.class, "bypass.use-item", "", base);
        spawnEntityPlayerPerm = pm.register(SettingsListener.class, "bypass.spawn.player", "", base);
        spawnEntityPluginPerm = pm.register(SettingsListener.class, "bypass.spawn.plugin", "", base);
        explodePlayer = pm.register(SettingsListener.class, "bypass.blockdamage.explode.player", "", base);
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
                if (checkSetting(event, player, regionsAt, () -> null, (s) -> s.build) == FALSE)
                {
                    i18n.sendTranslated(ACTION_BAR, player, NEGATIVE, "You are not allowed to build here.");
                    return;
                }
            });
        }
    }

    public Tristate checkSetting(Cancellable event, Player player, List<Region> regionsAt, Supplier<Permission> perm, Function<RegionConfig.Settings, Tristate> func)
    {
        Permission permission = perm.get();
        if (permission != null && player.hasPermission(permission.getId()))
        {
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
        return Tristate.UNDEFINED;
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

        if (checkSetting(event, player, regionsAt, () -> blockPerm, (s) -> s.blockUsage.block.getOrDefault(type, UNDEFINED)) == FALSE)
        {
            i18n.sendTranslated(ACTION_BAR, player, CRITICAL, "You are not allowed to interact with this here.");
            return;
        }

        if (item != null)
        {
            Permission usePerm = pm.register(SettingsListener.class, item.getItem().getId(), "Allows interacting with a " + item.getItem().getTranslation().get() + " Item in hand", useItemPerm);
            if (checkSetting(event, player, regionsAt, () -> usePerm, (s) -> s.blockUsage.item.getOrDefault(item.getItem(), UNDEFINED)) == FALSE)
            {
                i18n.sendTranslated(ACTION_BAR, player, CRITICAL, "You are not allowed to use this here.");
            }
        }
    }

    @Listener
    public void onUseItem(InteractItemEvent.Secondary event, @Root Player player)
    {
        ItemType item = event.getItemStack().getType();
        List<Region> regionsAt = manager.getRegionsAt(player.getLocation());
        Permission usePerm = pm.register(SettingsListener.class, item.getId(), "Allows interacting with a " + item.getTranslation().get() + " Item in hand", useItemPerm);
        if (checkSetting(event, player, regionsAt, () -> usePerm, (s) -> s.blockUsage.item.getOrDefault(item, UNDEFINED)) == FALSE)
        {
            i18n.sendTranslated(ACTION_BAR, player, CRITICAL, "You are not allowed to use this here.");
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
            if (((SpawnCause)cause.root()).getType() == SpawnTypes.PLUGIN)
            {
                if (player.isPresent())
                {
                    Permission usePerm = pm.register(SettingsListener.class, type.getId(), "Allows spawning a " + type.getTranslation().get(), spawnEntityPluginPerm);
                    if (checkSetting(event, player.get(), regionsAt, () -> usePerm, (s) -> s.spawn.player.getOrDefault(type, UNDEFINED)) == FALSE)
                    {
                        i18n.sendTranslated(ACTION_BAR, player.get(), CRITICAL, "You are not allowed spawn this here.");
                        return;
                    }
                }
                else if (checkSetting(event, null, regionsAt, () -> null, (s) -> s.spawn.plugin.getOrDefault(type, UNDEFINED)) == FALSE)
                {
                    return;
                }
            }
            else if (player.isPresent())
            {
                Permission usePerm = pm.register(SettingsListener.class, type.getId(), "Allows spawning a " + type.getTranslation().get(), spawnEntityPlayerPerm);
                if (checkSetting(event, player.get(), regionsAt, () -> usePerm, (s) -> s.spawn.player.getOrDefault(type, UNDEFINED)) == FALSE)
                {
                    i18n.sendTranslated(ACTION_BAR, player.get(), CRITICAL, "You are not allowed spawn this here.");
                    return;
                }
            }
            else if (checkSetting(event, null, regionsAt, () -> null, (s) -> s.spawn.naturally.getOrDefault(type, UNDEFINED)) == FALSE)
            {
                return; // natural
            }
        }
    }

    @Listener
    public void onChangeBlock(ChangeBlockEvent event)
    {
        Object rootCause = event.getCause().root();
        if (event instanceof ExplosionEvent)
        {
            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.getOriginal().getLocation().get());
                this.checkSetting(event, null, regionsAt, () -> null, (s) -> s.blockDamage.allExplosion);
                Player player = event.getCause().get(NamedCause.OWNER, Player.class).orElse(null);
                if (rootCause instanceof Player)
                {
                    player = ((Player) rootCause);
                }
                if (player != null)
                {
                    if (this.checkSetting(event, player, regionsAt, () -> explodePlayer, (s) -> s.blockDamage.playerExplosion) == TRUE)
                    {
                        event.setCancelled(false);
                    }
                    else if (event.isCancelled())
                    {
                        i18n.sendTranslated(player, NEGATIVE, "You are not allowed to let stuff explode here!");
                    }
                }
                if (event.isCancelled())
                {
                    return;
                }
            }
            return;
        }

        if (rootCause instanceof Hostile)
        {
            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.getOriginal().getLocation().get());
                this.checkSetting(event, null, regionsAt, () -> null, (s) -> s.blockDamage.monster);
            }
        }

        if (rootCause instanceof LocatableBlock)
        {
            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                List<Region> regionsAt = manager.getRegionsAt(trans.getOriginal().getLocation().get());
                this.checkSetting(event, null, regionsAt, () -> null, (s) -> s.blockDamage.block.getOrDefault(((LocatableBlock) rootCause).getBlockState().getType() , UNDEFINED));
            }
        }
    }
}
