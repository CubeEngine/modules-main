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
package org.cubeengine.module.basics.command.general;

import java.util.UUID;
import com.google.common.base.Optional;
import org.cubeengine.module.basics.Basics;
import org.cubeengine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.basics.BasicsUser;
import de.cubeisland.engine.module.basics.storage.BasicsUserEntity;
import de.cubeisland.engine.module.roles.RoleAppliedEvent;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.entity.GameModeData;
import org.spongepowered.api.data.manipulator.entity.InvulnerabilityData;
import org.spongepowered.api.data.manipulator.entity.TameableData;
import org.spongepowered.api.event.entity.player.PlayerInteractEntityEvent;
import org.spongepowered.api.event.entity.player.PlayerPlaceBlockEvent;
import org.spongepowered.api.item.inventory.ItemStack;

import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class GeneralsListener
{
    private final Basics module;
    private UserManager um;

    public GeneralsListener(Basics basics, UserManager um)
    {
        this.module = basics;
        this.um = um;
    }

    @Subscribe
    public void blockplace(final PlayerPlaceBlockEvent event)
    {
        User user = um.getExactUser(event.getUser().getUniqueId());
        if (user.get(BasicsAttachment.class).hasUnlimitedItems())
        {
            Optional<ItemStack> itemInHand = event.getUser().getItemInHand();
            if (itemInHand.isPresent())
            {
                itemInHand.get().setQuantity(itemInHand.get().getQuantity() + 1);
            }
        }
    }

    @Subscribe
    public void onInteractWithTamed(PlayerInteractEntityEvent event)
    {
        UUID uuid = event.getTargetEntity().get(Keys.TAMED_OWNER).or(Optional.<UUID>absent()).orNull();
        if (uuid != null)
        {

            if (!event.getUser().getUniqueId().equals(uuid))
            {
                User clicker = um.getExactUser(event.getUser().getUniqueId());
                User owner = um.getExactUser(uuid);
                clicker.sendTranslated(POSITIVE, "This {name#entity} belongs to {tamer}!",
                                       event.getEntity().getType().getName(), owner);
            }
        }
    }
}
