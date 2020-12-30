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
package org.cubeengine.module.vanillaplus.fix;

import java.util.Optional;
import java.util.UUID;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.First;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class TamedListener
{
    private I18n i18n;

    public TamedListener(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Listener
    public void onInteractWithTamed(InteractEntityEvent event, @First Player player)
    {
        Optional<UUID> uuid = event.getEntity().get(Keys.TAMER);
        if (uuid.isPresent())
        {
            final Optional<User> owner = Sponge.getServer().getUserManager().get(uuid.get());
            i18n.send(player, POSITIVE, "This {text#entity} belongs to {tamer}!",
                                event.getEntity().getType().asComponent(), owner.get());
        }
    }
}
