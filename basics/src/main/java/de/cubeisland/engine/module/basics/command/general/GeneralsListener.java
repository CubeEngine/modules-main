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

import org.bukkit.Bukkit;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

import de.cubeisland.engine.core.bukkit.AfterJoinEvent;
import de.cubeisland.engine.core.bukkit.BukkitUtils;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.util.matcher.Match;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.basics.BasicsUser;
import de.cubeisland.engine.module.basics.storage.BasicsUserEntity;
import de.cubeisland.engine.module.roles.RoleAppliedEvent;

import static de.cubeisland.engine.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;

public class GeneralsListener implements Listener
{
    private final Basics module;

    public GeneralsListener(Basics basics)
    {
        this.module = basics;
    }

    @EventHandler
    public void blockplace(final BlockPlaceEvent event)
    {
        User user = module.getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
        if (user.get(BasicsAttachment.class).hasUnlimitedItems())
        {
            ItemStack itemInHand = event.getPlayer().getItemInHand();
            itemInHand.setAmount(itemInHand.getAmount() + 1);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event)
    {
        BasicsUserEntity bUser = this.module.getBasicsUser(event.getPlayer()).getbUEntity();
        if (!module.perms().COMMAND_GOD_KEEP.isAuthorized(event.getPlayer()))
        {
            bUser.setValue(TABLE_BASIC_USER.GODMODE, false);
        }
        bUser.updateAsync();
        if (!module.perms().COMMAND_GAMEMODE_KEEP.isAuthorized(event.getPlayer()))
        {
            event.getPlayer().setGameMode(Bukkit.getServer().getDefaultGameMode()); // reset gamemode to default on the server
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event)
    {
        BasicsUserEntity bUser = this.module.getBasicsUser(event.getPlayer()).getbUEntity();
        if (!module.perms().COMMAND_GOD_KEEP.isAuthorized(event.getPlayer()))
        {
            bUser.setValue(TABLE_BASIC_USER.GODMODE, false);
            BukkitUtils.setInvulnerable(event.getPlayer(), false);
        }
        bUser.updateAsync();
        if (!module.perms().COMMAND_GAMEMODE_KEEP.isAuthorized(event.getPlayer()))
        {
            event.getPlayer().setGameMode(Bukkit.getServer().getDefaultGameMode()); // reset gamemode to default on the server
        }
    }

    @EventHandler
    public void onAfterJoin(AfterJoinEvent event)
    {
        User user = this.module.getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
        BasicsUser bUser = this.module.getBasicsUser(user);
        int amount = bUser.countMail();
        if (amount > 0)
        {
            user.sendTranslatedN(POSITIVE, amount, "You have a new mail!", "You have {amount} of mail!", amount);
            user.sendTranslated(NEUTRAL, "Use {text:/mail read} to display them.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        User user = this.module.getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
        BasicsUser bUser = this.module.getBasicsUser(user);
        if (bUser.getbUEntity().getValue(TABLE_BASIC_USER.GODMODE))
        {
            if (module.perms().COMMAND_GOD_KEEP.isAuthorized(user))
            {
                user.setInvulnerable(true);
            }
            else
            {
                bUser.getbUEntity().setValue(TABLE_BASIC_USER.GODMODE, false);
                bUser.getbUEntity().updateAsync();
            }
        }
    }

    @EventHandler
    public void onInteractWithTamed(PlayerInteractEntityEvent event)
    {
        if (event.getRightClicked() != null && event.getRightClicked() instanceof Tameable)
        {
            Tameable tamed = (Tameable) event.getRightClicked();
            if (tamed.getOwner() != null && !event.getPlayer().equals(tamed.getOwner()))
            {
                User clicker = this.module.getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
                clicker.sendTranslated(POSITIVE, "This {name#entity} belongs to {tamer}!", Match.entity().getNameFor(event.getRightClicked().getType()), tamed.getOwner());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
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
            event.getUser().setPlayerListName(colored);
        }
    }

    @EventHandler
    public void onPlayerInventoryClick(InventoryClickEvent event)
    {
        if (this.module.getConfiguration().preventOverstackedItems && !module.perms().OVERSTACKED_ANVIL_AND_BREWING.isAuthorized(event.getWhoClicked()))
        {

            if (event.getView().getTopInventory() instanceof AnvilInventory
                || event.getView().getTopInventory() instanceof BrewerInventory)
            {
                boolean topClick = event.getRawSlot() < event.getView().getTopInventory().getSize();
                switch (event.getAction())
                {
                case PLACE_ALL:
                case PLACE_SOME:
                    if (!topClick) return;
                    if (event.getCursor().getAmount() > event.getCursor().getMaxStackSize())
                    {
                        event.setCancelled(true);
                    }
                    break;
                case MOVE_TO_OTHER_INVENTORY:
                    if (topClick) return;
                    if (event.getCurrentItem().getAmount() > event.getCurrentItem().getMaxStackSize())
                    {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
