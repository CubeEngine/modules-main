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

import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

public class FlymodeFixListener
{
    @Listener
    public void join(final ServerSideConnectionEvent.Join event)
    {
        if (event.getPlayer().get(SafeLoginData.FLYMODE).orElse(false))
        {
            event.getPlayer().offer(Keys.CAN_FLY, true);
            event.getPlayer().offer(Keys.IS_FLYING, true);
        }
    }

    @Listener
    public void quit(final ServerSideConnectionEvent.Disconnect event)
    {
        event.getPlayer().offer(SafeLoginData.FLYMODE, event.getPlayer().get(Keys.IS_FLYING).orElse(false));
    }
}
