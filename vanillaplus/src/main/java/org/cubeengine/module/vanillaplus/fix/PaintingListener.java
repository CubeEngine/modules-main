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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Art;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.hanging.Painting;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class PaintingListener extends PermissionContainer
{
    private VanillaPlus module;
    private I18n i18n;
    private final Map<UUID, Painting> paintingChange;

    public final Permission CHANGEPAINTING = register("changepainting", "", null);


    public PaintingListener(PermissionManager pm, VanillaPlus module, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.module = module;
        this.i18n = i18n;
        this.paintingChange = new HashMap<>();
    }
    // TODO maybe save painting type when breaking so the same can be placed and allow stacking them somehow

    @Listener(order = Order.EARLY)
    public void onPlayerInteractEntity(InteractEntityEvent event, @First Player player)
    {
        if (event.getTargetEntity().getType() == EntityTypes.PAINTING)
        {
            if (!player.hasPermission(CHANGEPAINTING.getId()))
            {
                i18n.send(player, NEGATIVE, "You are not allowed to change this painting.");
                return;
            }
            Painting painting = (Painting)event.getTargetEntity();

            Painting playerPainting = this.paintingChange.get(player.getUniqueId());
            if(playerPainting == null && this.paintingChange.containsValue(painting))
            {
                i18n.send(player, NEGATIVE, "This painting is being used by another player.");
            }
            else if (playerPainting == null)
            {
                this.paintingChange.put(player.getUniqueId(), painting);
                i18n.send(player, POSITIVE, "You can now cycle through the paintings using your mousewheel.");
            }
            else
            {
                this.paintingChange.remove(player.getUniqueId());
                i18n.send(player, POSITIVE, "Painting locked");
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

    @Listener(order = Order.EARLY)
    public void onItemHeldChange(ChangeInventoryEvent.Held event, @First Player player)
    {
        if (!this.paintingChange.isEmpty())
        {
            Painting painting = this.paintingChange.get(player.getUniqueId());

            if (painting != null)
            {
                final int maxDistanceSquared = this.module.getConfig().fix.paintingSwitcherMaxDistance * this.module.getConfig().fix.paintingSwitcherMaxDistance;

                if (painting.getLocation().getPosition()
                            .distanceSquared(player.getLocation().getPosition()) > maxDistanceSquared)
                {
                    this.paintingChange.remove(player.getUniqueId());
                    i18n.send(player, POSITIVE, "Painting locked");
                    return;
                }

                List<Art> arts = new ArrayList<>(Sponge.getRegistry().getAllOf(Art.class));
                int artNumber = arts.indexOf(painting.art().get());
                int change = 1; // TODO this.compareSlots(event.getPreviousSlot(), event.getNewSlot());
                artNumber += change;
                if (artNumber >= arts.size())
                {
                    artNumber = 0;
                }
                else if(artNumber < 0)
                {
                    artNumber = arts.size() - 1;
                }
                for (Art art : arts)
                {
                    if (painting.offer(Keys.ART, arts.get(artNumber)).isSuccessful())
                    {
                        return;
                    }
                    artNumber += change;
                    if (artNumber >= arts.size())
                    {
                        artNumber = 0;
                    }
                    if (artNumber == -1)
                    {
                        artNumber = arts.size() - 1;
                    }
                }
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onPaintingBreakEvent(DestructEntityEvent event)
    {
        if (!(event.getTargetEntity() instanceof Painting))
        {
            return;
        }

        Painting painting = (Painting)event.getTargetEntity();

        Iterator<Entry<UUID, Painting>> paintingIterator = this.paintingChange.entrySet().iterator();
        while(paintingIterator.hasNext())
        {
            Entry<UUID, Painting> entry = paintingIterator.next();
            if(entry.getValue().equals(painting))
            {
                Optional<Player> player = Sponge.getServer().getPlayer(entry.getKey());
                if (player.isPresent())
                {
                    i18n.send(player.get(), NEGATIVE, "The painting broke");
                }
                paintingIterator.remove();
            }
        }
    }
}
