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
package de.cubeisland.engine.module.locker.storage;

import com.flowpowered.math.vector.Vector3d;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.locker.Locker;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.data.manipulator.item.LoreData;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.effect.sound.SoundTypes.BLAZE_HIT;
import static org.spongepowered.api.effect.sound.SoundTypes.GHAST_SCREAM;
import static org.spongepowered.api.effect.sound.SoundTypes.PISTON_EXTEND;

public class KeyBook
{
    public static final String TITLE = ChatFormat.RESET.toString() + ChatFormat.GOLD + "KeyBook " + ChatFormat.DARK_GREY + "#";
    public final ItemStack item;
    public final User currentHolder;
    private final Locker module;
    public final long lockID;
    private final String keyBookName;

    private KeyBook(ItemStack item, User currentHolder, Locker module)
    {
        this.item = item;
        this.currentHolder = currentHolder;
        this.module = module;
        keyBookName = Texts.toPlain(item.getData(DisplayNameData.class).get().getDisplayName());
        lockID = Long.valueOf(keyBookName.substring(keyBookName.indexOf('#') + 1, keyBookName.length()));
    }

    public static KeyBook getKeyBook(ItemStack item, User currentHolder, Locker module)
    {
        if (item.getItem() == ItemTypes.ENCHANTED_BOOK
            && item.getData(DisplayNameData.class).isPresent()
            && Texts.toPlain(item.getData(DisplayNameData.class).get().getDisplayName()).contains(KeyBook.TITLE))
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
                    currentHolder.playSound(PISTON_EXTEND, effectLocation.getPosition(), 1, 2);
                    currentHolder.playSound(PISTON_EXTEND, effectLocation.getPosition(), 1, (float)1.5);
                    lock.notifyKeyUsage(currentHolder);
                }
                return true;
            }
            else
            {
                currentHolder.sendTranslated(NEGATIVE, "You try to open the container with your KeyBook");
                currentHolder.sendTranslated(NEGATIVE, "but you get forcefully pushed away!");
                this.invalidate();
                currentHolder.playSound(GHAST_SCREAM, effectLocation.getPosition(), 1, 1);

                final Vector3d userDirection = currentHolder.getPlayer().get().getRotation();
                currentHolder.damage(1);
                currentHolder.setVelocity(userDirection.mul(-3));
                return false;
            }
        }
        else
        {
            currentHolder.sendTranslated(NEUTRAL, "You try to open the container with your KeyBook but nothing happens!");
            currentHolder.playSound(BLAZE_HIT, effectLocation.getPosition(), 1, 1);
            currentHolder.playSound(BLAZE_HIT, effectLocation.getPosition(), 1, (float)0.8);
            return false;
        }
    }

    public void invalidate()
    {
        DisplayNameData display = item.getOrCreate(DisplayNameData.class).get();
        LoreData lore = item.getOrCreate(LoreData.class).get();

        display.setDisplayName(Texts.of(TextColors.DARK_RED, "Broken KeyBook"));
        lore.set(Texts.of(currentHolder.getTranslation(NEUTRAL, "This KeyBook")),
                 Texts.of(currentHolder.getTranslation(NEUTRAL, "looks old and")),
                 Texts.of(currentHolder.getTranslation(NEUTRAL, "used up. It")),
                 Texts.of(currentHolder.getTranslation(NEUTRAL, "won't let you")),
                 Texts.of(currentHolder.getTranslation(NEUTRAL, "open any containers!")));
        item.setType(ItemTypes.PAPER);
        currentHolder.updateInventory();
        item.offer(display);
        item.offer(lore);
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
