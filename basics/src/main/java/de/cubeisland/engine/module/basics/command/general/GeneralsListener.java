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
package de.cubeisland.engine.module.basics.command.general;

import com.google.common.base.Optional;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.basics.BasicsUser;
import de.cubeisland.engine.module.basics.storage.BasicsUserEntity;
import de.cubeisland.engine.module.core.sponge.BukkitUtils;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.roles.RoleAppliedEvent;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.data.manipulator.entity.GameModeData;
import org.spongepowered.api.data.manipulator.entity.InvulnerabilityData;
import org.spongepowered.api.data.manipulator.entity.TameableData;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerChangeWorldEvent;
import org.spongepowered.api.event.entity.player.PlayerInteractEntityEvent;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;
import org.spongepowered.api.event.entity.player.PlayerPlaceBlockEvent;
import org.spongepowered.api.event.entity.player.PlayerQuitEvent;
import org.spongepowered.api.event.inventory.InventoryClickEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Texts;

import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
import static java.lang.Integer.MAX_VALUE;
import static org.spongepowered.api.event.Order.POST;

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
    public void onLeave(PlayerQuitEvent event)
    {
        BasicsUserEntity bUser = this.module.getBasicsUser(event.getUser()).getEntity();
        if (!module.perms().COMMAND_GOD_KEEP.isAuthorized(event.getUser()))
        {
            bUser.setValue(TABLE_BASIC_USER.GODMODE, false);
        }
        bUser.updateAsync();
        if (!module.perms().COMMAND_GAMEMODE_KEEP.isAuthorized(event.getUser()))
        {
            GameModeData mode = event.getUser().getOrCreate(GameModeData.class).get();
            mode.setGameMode(event.getUser().getWorld().getProperties().getGameMode());
            event.getUser().offer(mode); // reset gamemode to default on the server
        }
    }

    @Subscribe
    public void onWorldChange(PlayerChangeWorldEvent event)
    {
        BasicsUserEntity bUser = this.module.getBasicsUser(event.getUser()).getEntity();
        if (!module.perms().COMMAND_GOD_KEEP.isAuthorized(event.getUser()))
        {
            bUser.setValue(TABLE_BASIC_USER.GODMODE, false);
            event.getUser().offer(event.getUser().getOrCreate(InvulnerabilityData.class).get().setInvulnerableTicks(MAX_VALUE));
        }
        bUser.updateAsync();
        if (!module.perms().COMMAND_GAMEMODE_KEEP.isAuthorized(event.getUser()))
        {
            GameModeData mode = event.getUser().getOrCreate(GameModeData.class).get();
            mode.setGameMode(event.getUser().getWorld().getProperties().getGameMode());
            event.getUser().offer(mode); // reset gamemode to default on the server
        }
    }

    @Subscribe(order = POST)
    public void onAfterJoin(PlayerJoinEvent event)
    {
        BasicsUser bUser = this.module.getBasicsUser(event.getUser());
        int amount = bUser.countMail();
        if (amount > 0)
        {
            User user = um.getExactUser(event.getUser().getUniqueId());
            user.sendTranslatedN(POSITIVE, amount, "You have a new mail!", "You have {amount} of mail!", amount);
            user.sendTranslated(NEUTRAL, "Use {text:/mail read} to display them.");
        }
    }

    @Subscribe
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        User user = um.getExactUser(event.getUser().getUniqueId());
        BasicsUser bUser = this.module.getBasicsUser(event.getUser());
        if (bUser.getEntity().getValue(TABLE_BASIC_USER.GODMODE))
        {
            if (module.perms().COMMAND_GOD_KEEP.isAuthorized(user))
            {
                user.setInvulnerable(true);
            }
            else
            {
                bUser.getEntity().setValue(TABLE_BASIC_USER.GODMODE, false);
                bUser.getEntity().updateAsync();
            }
        }
    }

    @Subscribe
    public void onInteractWithTamed(PlayerInteractEntityEvent event)
    {
        Optional<TameableData> tameable = event.getTargetEntity().getData(TameableData.class);
        if (tameable.isPresent())
        {
            if (!event.getUser().equals(tameable.get().getOwner()))
            {
                User clicker = um.getExactUser(event.getUser().getUniqueId());
                clicker.sendTranslated(POSITIVE, "This {name#entity} belongs to {tamer}!",
                                       event.getEntity().getType().getName(), tameable.get().getOwner());
            }
        }
    }

    @Subscribe(order = POST)
    public void onPlayerJoin(RoleAppliedEvent event)
    {
        String meta = event.getAttachment().getCurrentMetadataString("tablist-prefix");
        if (meta != null)
        {
            String colored = ChatFormat.parseFormats(meta) + event.getUser().getDisplayName();
            if (colored.length() > 16)
            {
                colored = colored.substring(0,16);
            }
            event.getUser().getPlayer().get().getTabList().getPlayer(event.getUser().getUniqueId()).get().setDisplayName(Texts.of(colored));
        }
    }
}
