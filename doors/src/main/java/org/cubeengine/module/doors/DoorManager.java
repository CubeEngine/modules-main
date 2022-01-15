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
package org.cubeengine.module.locker.old.door;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.block.BlockTypes.ACACIA_DOOR;
import static org.spongepowered.api.block.BlockTypes.BIRCH_DOOR;
import static org.spongepowered.api.block.BlockTypes.DARK_OAK_DOOR;
import static org.spongepowered.api.block.BlockTypes.IRON_DOOR;
import static org.spongepowered.api.block.BlockTypes.JUNGLE_DOOR;
import static org.spongepowered.api.block.BlockTypes.SPRUCE_DOOR;
import static org.spongepowered.api.data.type.PortionTypes.TOP;
import static org.spongepowered.api.util.Direction.DOWN;
import static org.spongepowered.api.util.Direction.UP;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.util.BlockUtil;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.data.LockerData;
import org.cubeengine.module.locker.data.ProtectionFlag;
import org.cubeengine.module.locker.storage.Lock;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.World;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class DoorManager
{

    public boolean handleDoorUse(ServerLocation dataHolder, ServerPlayer player)
    {
        // TODO bring back Doors etc.
        final LockType lockType = dataHolder.get(LockerData.TYPE).map(LockType::valueOf).orElse(null);
        if (lockType == null)
        {
            return false;
        }
        final UUID owner = dataHolder.get(LockerData.OWNER).get();
        if (lockType == LockType.PUBLIC)
        {
            this.useDoor(dataHolder, player, owner);
            return true;
        }
        else if (lockType != LockType.PRIVATE)
        {
            throw new IllegalStateException();
        }

        final boolean canAccess = canAccess(dataHolder, player, owner, player.getItemInHand(HandTypes.MAIN_HAND));
        if (!canAccess)
        {
            if (perms.SHOW_OWNER.check(player))
            {
                final String ownerName = Sponge.getServer().getUserManager().get(owner).map(User::getName).orElse("???");
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Magic prevents you from using this door of {user}!", ownerName);
            }
            else
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Magic prevents you from using this door!");
            }
            return true;
        }
        this.useDoor(dataHolder, player, owner);
        return true;
    }

    private void useDoor(ServerLocation door, ServerPlayer player, UUID owner)
    {
        final String ownerName = Sponge.getServer().getUserManager().get(owner).map(User::getName).orElse("???");
        final BlockType blockType = door.getBlockType();
        if (blockType.isAnyOf(BlockTypes.IRON_DOOR) && !module.getConfig().openIronDoorWithClick)
        {
            i18n.send(ChatType.ACTION_BAR, player, NEUTRAL, "You cannot open the heavy door!");
            return;
        }
        if (perms.SHOW_OWNER.check(player))
        {
            i18n.send(ChatType.ACTION_BAR, player, NEUTRAL, "This door is protected by {user}", ownerName);
        }
        final boolean open = door.get(Keys.IS_OPEN).orElse(false);
        for (ServerLocation doorLoc : Arrays.<ServerLocation>asList()) // TODO get all doorLocations
        {
            final BlockState newDoorState = doorLoc.getBlock().with(Keys.IS_OPEN, !open).get();
            doorLoc.setBlock(newDoorState);
            player.playSound(Sound.sound(open ? SoundTypes.BLOCK_WOODEN_DOOR_CLOSE : SoundTypes.BLOCK_WOODEN_DOOR_OPEN, Source.PLAYER, 1, 1), doorLoc.getPosition());
        }

        if (!open)
        {
            // TODO schedule autoclose
        }
        // TODO notify usage
    }

    private void scheduleAutoClose(Player user)
    {
        if (this.hasFlag(ProtectionFlag.AUTOCLOSE))
        {
            if (!this.manager.module.getConfig().autoCloseEnable) return;
            taskId = module.getTaskManager().runTaskDelayed(Locker.class, () -> {
                Sponge.getCauseStackManager().pushCause(user);
                int n = locations.size() / 2;
                for (Location location : locations)
                {
                    if (n-- > 0)
                    {
                        ((World)location.getExtent()).playSound(SoundTypes.BLOCK_WOODEN_DOOR_CLOSE, location.getPosition(), 1);
                    }
                    location.setBlock(location.getBlock().with(Keys.OPEN, false).get());
                }
            }, this.manager.module.getConfig().autoCloseSeconds * 20);
        }
    }

    public void doorFind(ServerLocation oneDoor)
    {
        if (material == WOODEN_DOOR
            || material == IRON_DOOR
            || material == ACACIA_DOOR
            || material == BIRCH_DOOR
            || material == DARK_OAK_DOOR
            || material == JUNGLE_DOOR
            || material == SPRUCE_DOOR)
        {
            locations.add(block); // Original Block
            // Find upper/lower door part

            Optional<? extends Enum<?>> halfTrait = block.getBlock().getTraitValue(EnumTraits.WOODEN_DOOR_HALF);
            boolean upperHalf = halfTrait.isPresent() && halfTrait.get().name().equals("UPPER");

            // if (placed.get(Keys.PORTION_TYPE).get().equals(TOP)) // TODO use once implemented

            Location<World> relative = block.getRelative(upperHalf ? Direction.DOWN : Direction.UP);
            if (relative.getBlockType() != material) // try other half?
            {
                relative = block.getRelative(Direction.UP);
            }
            if (relative.getBlockType() != material)
            {
                throw new IllegalStateException("Other door half is missing");
            }

            if (relative.getBlockType() == material)
            {
                locations.add(relative);

                Direction direction = block.get(Keys.DIRECTION).get();
                Hinge hinge = block.get(Keys.HINGE_POSITION).get();
                Direction otherDoor = BlockUtil.getOtherDoorDirection(direction, hinge);
                Location<World> blockOther = block.getRelative(otherDoor);
                Location<World> relativeOther = relative.getRelative(otherDoor);
                if (blockOther.getBlockType() == material && blockOther.getBlockType().equals(relativeOther.getBlockType()) && // Same DoorMaterial
                    blockOther.get(Keys.DIRECTION).get() == direction && blockOther.get(Keys.HINGE_POSITION).get() != hinge) // Door-Pair
                {
                    locations.add(blockOther);
                    locations.add(relativeOther);
                }
            }
        }
    }

    private boolean isDoor(BlockType type)
    {
        return type == WOODEN_DOOR || type == IRON_DOOR || type == SPRUCE_DOOR || type == BIRCH_DOOR
            || type == JUNGLE_DOOR || type == ACACIA_DOOR || type == DARK_OAK_DOOR;
    }


    private boolean onPlaceDoor(ChangeBlockEvent event, BlockSnapshot placed, Player player)
    {
        Location<World> location = placed.getLocation().get();
        BlockState doorState = placed.getState();
        Hinge hinge = doorState.get(Keys.HINGE_POSITION).get();
        Direction direction = doorState.get(Keys.DIRECTION).get();

        Location<World> relative = location.getRelative(BlockUtil.getOtherDoorDirection(direction, hinge));
        if (!isDoor(relative.getBlockType()))
        {
            return false; // Not a door
        }

        if (!relative.get(Keys.DIRECTION).get().equals(direction) || relative.get(Keys.HINGE_POSITION).get().equals(hinge))
        {
            return false; // Not a doubledoor
        }
        Lock lock = this.manager.getLock(relative);
        if (lock != null)
        {
            if (!lock.validateTypeAt(relative))
            {
                i18n.send(ACTION_BAR, player, NEUTRAL, "Nearby BlockProtection is not valid!");
                lock.delete(player);
                return true;
            }

            if (placed.get(Keys.PORTION_TYPE).get().equals(TOP))
            {
                relative = location.getRelative(DOWN);
            }
            else
            {
                relative = location.getRelative(UP);
            }


            if (lock.isOwner(player) || lock.hasAdmin(player) || player.hasPermission(module.perms().EXPAND_OTHER.getId()))
            {
                this.manager.extendLock(lock, location);
                this.manager.extendLock(lock, relative);
                i18n.send(ACTION_BAR, player, POSITIVE, "Protection expanded to {amount} blocks!", lock.getLocations().size());
            }
            else
            {
                event.setCancelled(true);
                i18n.send(ACTION_BAR, player, NEGATIVE, "The nearby door is protected by someone else!");
            }
            return true;
        }
        return false;
    }


}
