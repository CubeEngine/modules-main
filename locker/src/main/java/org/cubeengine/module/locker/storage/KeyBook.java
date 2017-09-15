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
package org.cubeengine.module.locker.storage;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCKS;
import static org.spongepowered.api.data.key.Keys.DISPLAY_NAME;
import static org.spongepowered.api.data.key.Keys.ITEM_LORE;
import static org.spongepowered.api.effect.sound.SoundTypes.BLOCK_PISTON_EXTEND;
import static org.spongepowered.api.effect.sound.SoundTypes.ENTITY_BLAZE_HURT;
import static org.spongepowered.api.effect.sound.SoundTypes.ENTITY_GHAST_SCREAM;
import static org.spongepowered.api.item.ItemTypes.PAPER;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.data.LockerData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.VelocityData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSources;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class KeyBook
{
    public static final Text TITLE = Text.of(TextColors.RESET, TextColors.GOLD, "Keybook ", TextColors.DARK_GRAY, "#");
    public ItemStack item;
    public final Player holder;
    private final Locker module;
    private I18n i18n;
    public final Long lockID;
    private final byte[] pass;
    private final Text display;

    public KeyBook(ItemStack item, LockerData lockerData, Player holder, Locker module, I18n i18n)
    {
        this.item = item;
        this.holder = holder;
        this.module = module;
        this.i18n = i18n;
        display = item.get(DISPLAY_NAME).get();
        lockID = lockerData.get(LockerData.LOCK_ID).get();
        List<Byte> pass = lockerData.get(LockerData.LOCK_PASS).get();
        this.pass = new byte[pass.size()];
        for (int i = 0; i < pass.size(); i++)
        {
            this.pass[i] = pass.get(i);
        }
    }

    public static KeyBook getKeyBook(Optional<ItemStack> item, Player currentHolder, Locker module, I18n i18n)
    {
        if (!item.isPresent())
        {
            return null;
        }

        if (item.get().getItem() == ItemTypes.ENCHANTED_BOOK
            && item.get().get(DISPLAY_NAME).map(Text::toPlain).map(s -> s.contains(TITLE.toPlain())).orElse(false))
        {
            Optional<LockerData> lockerData = item.get().get(LockerData.class);
            if (lockerData.isPresent())
            {
                return new KeyBook(item.get(), lockerData.get(), currentHolder, module, i18n);
            }
        }
        return null;
    }

    public boolean check(Lock lock, Location effectLocation)
    {
        if (!lock.getId().equals(lockID)) // Id matches ?
        {
            if (!lock.isOwner(holder))
            {
                i18n.send(ACTION_BAR, holder, NEUTRAL, "You try to open the container with your KeyBook but nothing happens!");
                holder.playSound(ENTITY_BLAZE_HURT, effectLocation.getPosition(), 1, 1);
                holder.playSound(ENTITY_BLAZE_HURT, effectLocation.getPosition(), 1, (float)0.8);
            }
            return false;
        }
        if (!this.isValidFor(lock))
        {
            i18n.send(ACTION_BAR, holder, NEGATIVE, "You try to open the container with your KeyBook but you get forcefully pushed away!");
            this.invalidate();
            holder.playSound(ENTITY_GHAST_SCREAM, effectLocation.getPosition(), 1, 1);

            final Vector3d userDirection = holder.getRotation();
            Sponge.getCauseStackManager().pushCause(item);
            holder.damage(0, DamageSources.MAGIC);
            VelocityData velocity = holder.getOrCreate(VelocityData.class).get();
            Sponge.getCauseStackManager().popCause();
            velocity.velocity().set(userDirection.mul(-3));
            holder.offer(velocity);
            return false;
        }
        if (effectLocation != null)
        {
            if (!lock.isOwner(holder))
            {
                i18n.send(ACTION_BAR, holder, POSITIVE, "As you approach with your KeyBook the magic lock disappears!");
            }

            TaskManager tm = module.getTaskManager();
            playUnlockSound(holder, effectLocation, tm);

            lock.notifyKeyUsage(holder);
        }
        return true;
    }

    public static void playUnlockSound(Player holder, final Location effectLocation, TaskManager tm)
    {
        holder.playSound(BLOCK_PISTON_EXTEND, effectLocation.getPosition(), 1, 2);
        holder.playSound(BLOCK_PISTON_EXTEND, effectLocation.getPosition(), 1, (float) 1.5);

        tm.runTaskDelayed(Locker.class, () -> {
            holder.playSound(BLOCK_PISTON_EXTEND, effectLocation.getPosition(), 1, 2);
            holder.playSound(BLOCK_PISTON_EXTEND, effectLocation.getPosition(), 1, (float)1.5);
        }, 3);
    }

    public void invalidate()
    {
        item.offer(DISPLAY_NAME, Text.of(TextColors.DARK_RED, "Broken KeyBook"));
        item.offer(ITEM_LORE, Arrays.asList(i18n.translate(holder, NEUTRAL, "This KeyBook"),
                                            i18n.translate(holder, NEUTRAL, "looks old and"),
                                            i18n.translate(holder, NEUTRAL, "used up. It"),
                                            i18n.translate(holder, NEUTRAL, "won't let you"),
                                            i18n.translate(holder, NEUTRAL, "open any containers!")));
        item = Sponge.getRegistry().createBuilder(ItemStack.Builder.class).fromItemStack(item).itemType(PAPER).build();
        holder.setItemInHand(HandTypes.MAIN_HAND, item);
    }

    public boolean isValidFor(Lock lock)
    {
        boolean b = Arrays.equals(pass, lock.model.getValue(TABLE_LOCKS.PASSWORD));
        if (!b)
        {
            this.module.getLogger().debug("Invalid KeyBook detected! {}", display);
        }
        return b;
    }
}
