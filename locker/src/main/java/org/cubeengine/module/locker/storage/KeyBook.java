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
import com.flowpowered.math.vector.Vector3d;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.service.user.MultilingualPlayer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.VelocityData;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;

import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.data.key.Keys.DISPLAY_NAME;
import static org.spongepowered.api.data.key.Keys.ITEM_LORE;
import static org.spongepowered.api.effect.sound.SoundTypes.*;
import static org.spongepowered.api.item.ItemTypes.PAPER;

public class KeyBook
{
    public static final String TITLE = ChatFormat.RESET.toString() + ChatFormat.GOLD + "KeyBook " + ChatFormat.DARK_GREY + "#";
    public ItemStack item;
    public final MultilingualPlayer currentHolder;
    private final Locker module;
    public final long lockID;
    private final String keyBookName;

    private KeyBook(ItemStack item, MultilingualPlayer currentHolder, Locker module)
    {
        this.item = item;
        this.currentHolder = currentHolder;
        this.module = module;
        keyBookName = item.get(DISPLAY_NAME).transform(Texts::toPlain).or("");
        lockID = Long.valueOf(keyBookName.substring(keyBookName.indexOf('#') + 1, keyBookName.length()));
    }

    public static KeyBook getKeyBook(ItemStack item, MultilingualPlayer currentHolder, Locker module)
    {
        if (item.getItem() == ItemTypes.ENCHANTED_BOOK
            && item.get(DISPLAY_NAME).transform(Texts::toPlain).transform(s -> s.contains(TITLE)).or(false))
        {
            try
            {
                return new KeyBook(item, currentHolder, module);
            }
            catch (NumberFormatException|IndexOutOfBoundsException ignore)
            {}
        }
        return null;
    }

    public boolean check(Lock lock, Location effectLocation)
    {
        if (lock.getId().equals(lockID)) // Id matches ?
        {
            // Validate book
            if (this.isValidFor(lock))
            {
                if (effectLocation != null)
                {
                    currentHolder.sendTranslated(POSITIVE,
                                                 "As you approach with your KeyBook the magic lock disappears!");
                    currentHolder.original().playSound(PISTON_EXTEND, effectLocation.getPosition(), 1, 2);
                    currentHolder.original().playSound(PISTON_EXTEND, effectLocation.getPosition(), 1, (float)1.5);
                    lock.notifyKeyUsage(currentHolder);
                }
                return true;
            }
            else
            {
                currentHolder.sendTranslated(NEGATIVE, "You try to open the container with your KeyBook");
                currentHolder.sendTranslated(NEGATIVE, "but you get forcefully pushed away!");
                this.invalidate();
                currentHolder.original().playSound(GHAST_SCREAM, effectLocation.getPosition(), 1, 1);

                final Vector3d userDirection = currentHolder.original().getRotation();

                // TODO damaging player working? /w effects see Lock for effects playing manually
                currentHolder.original().offer(Keys.HEALTH, currentHolder.original().getHealthData().health().get() - 1);
                VelocityData velocity = currentHolder.original().getOrCreate(VelocityData.class).get();
                velocity.velocity().set(userDirection.mul(-3));
                currentHolder.original().offer(velocity);
                return false;
            }
        }
        else
        {
            currentHolder.sendTranslated(NEUTRAL, "You try to open the container with your KeyBook but nothing happens!");
            currentHolder.original().playSound(BLAZE_HIT, effectLocation.getPosition(), 1, 1);
            currentHolder.original().playSound(BLAZE_HIT, effectLocation.getPosition(), 1, (float)0.8);
            return false;
        }
    }

    public void invalidate()
    {
        item.offer(DISPLAY_NAME, Texts.of(TextColors.DARK_RED, "Broken KeyBook"));
        item.offer(ITEM_LORE, Arrays.asList(currentHolder.getTranslation(NEUTRAL, "This KeyBook"),
                                            currentHolder.getTranslation(NEUTRAL, "looks old and"),
                                            currentHolder.getTranslation(NEUTRAL, "used up. It"),
                                            currentHolder.getTranslation(NEUTRAL, "won't let you"),
                                            currentHolder.getTranslation(NEUTRAL, "open any containers!")));
        item = module.getGame().getRegistry().createItemBuilder().fromItemStack(item).itemType(PAPER).build();
        currentHolder.original().setItemInHand(item);
    }

    public boolean isValidFor(Lock lock)
    {
        boolean b = keyBookName.startsWith(lock.getColorPass());
        if (!b)
        {
            this.module.getProvided(Log.class).debug("Invalid KeyBook detected! {}|{}", lock.getColorPass(), keyBookName);
        }
        return b;
    }
}
