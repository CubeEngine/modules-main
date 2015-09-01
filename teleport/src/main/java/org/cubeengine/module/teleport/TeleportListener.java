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
package org.cubeengine.module.teleport;

import com.google.common.base.Optional;
import org.cubeengine.module.core.util.LocationUtil;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.item.ItemTypes.COMPASS;

public class TeleportListener
{
    private final Teleport module;
    private UserManager um;

    public TeleportListener(Teleport module, UserManager um)
    {
        this.module = module;
        this.um = um;
    }

    @Listener
    public void onTeleport(DisplaceEntityEvent.Teleport.TargetPlayer event)
    {
        User user = um.getExactUser(event.getTargetEntity().getUniqueId());
        // TODO limit cause
        user.attachOrGet(TeleportAttachment.class, module).setLastLocation(event.getOldTransform().getLocation()); // TODO transform
    }

    @Listener
    public void onDeath(DestructEntityEvent event)
    {
        if (!(event.getTargetEntity() instanceof Player))
        {
            return;
        }
        User user = um.getExactUser(event.getTargetEntity().getUniqueId());
        if (user.hasPermission(module.perms().COMMAND_BACK_ONDEATH.getId()))
        {
            user.attachOrGet(TeleportAttachment.class, module).setDeathLocation(user.asPlayer().getLocation());
        }
    }

    @Listener
    public void onClick(InteractBlockEvent.SourcePlayer event)
    {
        // TODO left click air is not handled
        if (event.getSourceEntity().getItemInHand().transform(ItemStack::getItem).orNull() != COMPASS)
        {
            return;
        }
        event.setCancelled(true);
        if (event instanceof InteractBlockEvent.Attack)
        {
            if (event.getSourceEntity().hasPermission(module.perms().COMPASS_JUMPTO_LEFT.getId()))
            {
                User user = um.getExactUser(event.getSourceEntity().getUniqueId());
                Location<World> loc;
                if (event.getTargetLocation().getBlockType().isSolidCube())
                {
                    loc = event.getTargetLocation().add(0.5, 1, 0.5);
                }
                else
                {
                    Optional<BlockRayHit<World>> end = BlockRay.from(user.asPlayer()).end();
                    if (!end.isPresent())
                    {
                        return;
                    }
                    loc = end.get().getLocation().add(0.5, 1, 0.5);
                }
                event.getSourceEntity().setLocation(loc);
                user.sendTranslated(NEUTRAL, "Poof!");
                event.setCancelled(true);
            }
        }
        else if (event instanceof InteractBlockEvent.Use)
        {
            if (event.getSourceEntity().hasPermission(module.perms().COMPASS_JUMPTO_RIGHT.getId()))
            {
                User user = um.getExactUser(event.getSourceEntity().getUniqueId());
                Location loc = LocationUtil.getBlockBehindWall(user, this.module.getConfig().navigation.thru.maxRange,
                                                               this.module.getConfig().navigation.thru.maxWallThickness);
                if (loc == null)
                {
                    user.sendTranslated(NEGATIVE, "Nothing to pass through!");
                    return;
                }
                loc = loc.add(0, 1, 0);
                event.getSourceEntity().setLocation(loc);
                user.sendTranslated(NEUTRAL, "You passed through a wall");
                event.setCancelled(true);
            }
        }
    }
}
