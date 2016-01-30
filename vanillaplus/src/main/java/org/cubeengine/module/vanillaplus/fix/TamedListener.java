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
package org.cubeengine.module.vanillaplus.fix;

import java.util.UUID;
import com.google.common.base.Optional;
import org.spongepowered.api.data.key.Keys;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class TamedListener
{
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
