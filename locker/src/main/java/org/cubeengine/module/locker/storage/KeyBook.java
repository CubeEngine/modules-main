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
package org.cubeengine.module.locker.storage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.flowpowered.math.vector.Vector3d;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.data.LockerData;
import org.cubeengine.service.i18n.I18n;
import org.jooq.types.UInteger;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.mutable.entity.VelocityData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;

import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCK;
import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.data.key.Keys.DISPLAY_NAME;
import static org.spongepowered.api.data.key.Keys.ITEM_LORE;
import static org.spongepowered.api.effect.sound.SoundTypes.*;
import static org.spongepowered.api.item.ItemTypes.PAPER;

public class KeyBook
{
    public static final Text TITLE = Text.of(TextColors.RESET, TextColors.GOLD, "Keybook ", TextColors.DARK_GRAY, "#");
    public ItemStack item;
    public final Player holder;
    private final Locker module;
    private I18n i18n;
    public final UInteger lockID;
    private final byte[] pass;
    private final Text display;

    public KeyBook(ItemStack item, LockerData lockerData, Player holder, Locker module, I18n i18n)
    {
        this.item = item;
        this.holder = holder;
        this.module = module;
        this.i18n = i18n;
        display = item.get(DISPLAY_NAME).get();
        lockID = UInteger.valueOf(lockerData.get(LockerData.LOCK_ID).get());
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
                i18n.sendTranslated(holder, NEUTRAL, "You try to open the container with your KeyBook but nothing happens!");
                holder.playSound(BLAZE_HIT, effectLocation.getPosition(), 1, 1);
                holder.playSound(BLAZE_HIT, effectLocation.getPosition(), 1, (float)0.8);
            }
            return false;
        }
        if (!this.isValidFor(lock))
        {
            i18n.sendTranslated(holder, NEGATIVE, "You try to open the container with your KeyBook");
            i18n.sendTranslated(holder, NEGATIVE, "but you get forcefully pushed away!");
            this.invalidate();
            holder.playSound(GHAST_SCREAM, effectLocation.getPosition(), 1, 1);

            final Vector3d userDirection = holder.getRotation();

            // TODO damaging player working? /w effects see Lock for effects playing manually
            holder.offer(Keys.HEALTH, holder.getHealthData().health().get() - 1);
            VelocityData velocity = holder.getOrCreate(VelocityData.class).get();
            velocity.velocity().set(userDirection.mul(-3));
            holder.offer(velocity);
            return false;
        }
        if (effectLocation != null)
        {
            if (!lock.isOwner(holder))
            {
                i18n.sendTranslated(holder, POSITIVE, "As you approach with your KeyBook the magic lock disappears!");
            }
            holder.playSound(PISTON_EXTEND, effectLocation.getPosition(), 1, 2);
            holder.playSound(PISTON_EXTEND, effectLocation.getPosition(), 1, (float) 1.5);
            lock.notifyKeyUsage(holder);
        }
        return true;
    }

    public void invalidate()
    {
        item.offer(DISPLAY_NAME, Text.of(TextColors.DARK_RED, "Broken KeyBook"));
        item.offer(ITEM_LORE, Arrays.asList(i18n.getTranslation(holder, NEUTRAL, "This KeyBook"),
                                            i18n.getTranslation(holder, NEUTRAL, "looks old and"),
                                            i18n.getTranslation(holder, NEUTRAL, "used up. It"),
                                            i18n.getTranslation(holder, NEUTRAL, "won't let you"),
                                            i18n.getTranslation(holder, NEUTRAL, "open any containers!")));
        item = module.getGame().getRegistry().createBuilder(ItemStack.Builder.class).fromItemStack(item).itemType(PAPER).build();
        holder.setItemInHand(item);
    }

    public boolean isValidFor(Lock lock)
    {
        boolean b = Arrays.equals(pass, lock.model.getValue(TABLE_LOCK.PASSWORD));
        if (!b)
        {
            this.module.getProvided(Log.class).debug("Invalid KeyBook detected! {}", display);
        }
        return b;
    }
}
