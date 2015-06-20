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
package de.cubeisland.engine.module.teleport;

import com.google.common.base.Optional;
import de.cubeisland.engine.module.core.util.LocationUtil;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.entity.EntityInteractionType;
import org.spongepowered.api.entity.EntityInteractionTypes;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.EntityTeleportEvent;
import org.spongepowered.api.event.entity.player.PlayerDeathEvent;
import org.spongepowered.api.event.entity.player.PlayerInteractBlockEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.block.BlockTypes.AIR;
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

    @Subscribe
    public void onTeleport(EntityTeleportEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            User user = um.getExactUser(event.getEntity().getUniqueId());
            // TODO limit cause
            user.attachOrGet(TeleportAttachment.class, module).setLastLocation(event.getOldLocation());
        }
    }

    @Subscribe
    public void onDeath(PlayerDeathEvent event)
    {
        User user = um.getExactUser(event.getEntity().getUniqueId());
        if (module.perms().COMMAND_BACK_ONDEATH.isAuthorized(user))
        {
            user.attachOrGet(TeleportAttachment.class, module).setDeathLocation(user.asPlayer().getLocation());
        }
    }

    @Subscribe
    public void onClick(PlayerInteractBlockEvent event)
    {
        // TODO left click air is not handled
        if (event.getUser().getItemInHand().transform(ItemStack::getItem).orNull() != COMPASS)
        {
            return;
        }
        event.setCancelled(true);
        EntityInteractionType type = event.getInteractionType();
        if (type == EntityInteractionTypes.ATTACK)
        {
            if (module.perms().COMPASS_JUMPTO_LEFT.isAuthorized(event.getUser()))
            {
                User user = um.getExactUser(event.getUser().getUniqueId());
                Location loc;
                if (event.getBlock().getBlockType().isSolidCube())
                {
                    loc = event.getBlock().add(0.5, 1, 0.5);
                }
                else
                {
                    Optional<BlockRayHit> end = BlockRay.from(user.asPlayer()).end();
                    if (!end.isPresent())
                    {
                        return;
                    }
                    loc = end.get().getLocation().add(0.5, 1, 0.5);
                }
                event.getUser().setLocation(loc);
                user.sendTranslated(NEUTRAL, "Poof!");
                event.setCancelled(true);
            }
        }
        else if (type == EntityInteractionTypes.USE)
        {
            if (module.perms().COMPASS_JUMPTO_RIGHT.isAuthorized(event.getUser()))
            {
                User user = um.getExactUser(event.getUser().getUniqueId());
                Location loc = LocationUtil.getBlockBehindWall(user, this.module.getConfig().navigation.thru.maxRange,
                                                               this.module.getConfig().navigation.thru.maxWallThickness);
                if (loc == null)
                {
                    user.sendTranslated(NEGATIVE, "Nothing to pass through!");
                    return;
                }
                loc = loc.add(0, 1, 0);
                event.getUser().setLocation(loc);
                user.sendTranslated(NEUTRAL, "You passed through a wall");
                event.setCancelled(true);
            }
        }
    }
}
