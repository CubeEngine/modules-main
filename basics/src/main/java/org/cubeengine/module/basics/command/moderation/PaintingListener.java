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
package org.cubeengine.module.basics.command.moderation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.cubeengine.service.user.User;
import org.cubeengine.module.basics.Basics;
import org.cubeengine.service.user.UserManager;

import org.spongepowered.api.data.type.Art;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.hanging.Painting;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.player.PlayerInteractEntityEvent;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class PaintingListener
{
    private final Basics module;
    private UserManager um;
    private final Map<UUID, Painting> paintingChange;

    public PaintingListener(Basics module, UserManager um)
    {
        this.module = module;
        this.um = um;
        this.paintingChange = new HashMap<>();
    }

    @Subscribe(order = Order.EARLY)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if (event.getTargetEntity().getType() == EntityTypes.PAINTING)
        {
            User user = um.getExactUser(event.getUser().getUniqueId());

            if (!module.perms().CHANGEPAINTING.isAuthorized(user))
            {
                user.sendTranslated(NEGATIVE, "You are not allowed to change this painting.");
                return;
            }
            Painting painting = (Painting)event.getTargetEntity();

            Painting playerPainting = this.paintingChange.get(user.getUniqueId());
            if(playerPainting == null && this.paintingChange.containsValue(painting))
            {
                user.sendTranslated(NEGATIVE, "This painting is being used by another player.");
            }
            else if (playerPainting == null)
            {
                this.paintingChange.put(user.getUniqueId(), painting);
                user.sendTranslated(POSITIVE, "You can now cycle through the paintings using your mousewheel.");
            }
            else
            {
                this.paintingChange.remove(user.getUniqueId());
                user.sendTranslated(POSITIVE, "Painting locked");
            }
        }
    }

    private int compareSlots(int previousSlot, int newSlot)
    {
        if(previousSlot == 8 && newSlot == 0)
        {
            newSlot = 9;
        }
        if(previousSlot == 0 && newSlot == 8)
        {
            newSlot = -1;
        }
        return Integer.compare(previousSlot, newSlot);
    }

    @Subscribe(order = Order.EARLY)
    public void onItemHeldChange(PlayerItemHeldEvent event)
    {
        if (!this.paintingChange.isEmpty())
        {
            Painting painting = this.paintingChange.get(event.getUser().getUniqueId());

            if (painting != null)
            {
                User user = um.getExactUser(event.getUser().getUniqueId());
                final int maxDistanceSquared = this.module.getConfiguration().maxChangePaintingDistance * this.module
                    .getConfiguration().maxChangePaintingDistance;

                if (painting.getLocation().getPosition()
                            .distanceSquared(user.getLocation().getPosition()) > maxDistanceSquared)
                {
                    this.paintingChange.remove(user.getUniqueId());
                    user.sendTranslated(POSITIVE, "Painting locked");
                    return;
                }

                Art[] arts = Art.values();

                int artNumber = painting.getArtData().getArt().ordinal();
                int change = this.compareSlots(event.getPreviousSlot(), event.getNewSlot());
                artNumber += change;
                if (artNumber >= arts.length)
                {
                    artNumber = 0;
                }
                else if(artNumber < 0)
                {
                    artNumber = arts.length - 1;
                }
                for (Art art : arts)
                {
                    if (painting.setArt(arts[artNumber]))
                    {
                        return;
                    }
                    artNumber += change;
                    if (artNumber >= arts.length)
                    {
                        artNumber = 0;
                    }
                    if (artNumber == -1)
                    {
                        artNumber = arts.length - 1;
                    }
                }
            }
        }
    }

    @Subscribe(order = Order.EARLY)
    public void onPaintingBreakEvent(HangingBreakEvent event)
    {
        if (!(event.getEntity() instanceof Painting))
        {
            return;
        }

        Painting painting = (Painting)event.getEntity();

        Iterator<Entry<UUID, Painting>> paintingIterator = this.paintingChange.entrySet().iterator();
        while(paintingIterator.hasNext())
        {
            Entry<UUID, Painting> entry = paintingIterator.next();
            if(entry.getValue().equals(painting))
            {
                um.getExactUser(entry.getKey()).sendTranslated(NEGATIVE, "The painting broke");
                paintingIterator.remove();
            }
        }
    }
}
