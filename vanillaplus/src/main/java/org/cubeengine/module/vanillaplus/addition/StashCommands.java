package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class StashCommands
{
    private I18n i18n;

    public StashCommands(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Stashes or unstashes your inventory to reuse later")
    @Restricted(value = Player.class, msg = "Yeah you better put it away!")
    public void stash(Player context)
    {
        ItemStack[] stashedInv = context.get(BasicsAttachment.class).getStashedInventory();
        ItemStack[] stashedArmor = context.get(BasicsAttachment.class).getStashedArmor();
        ItemStack[] invToStash = context.getInventory().getContents().clone();
        ItemStack[] armorToStash = context.getInventory().getArmorContents().clone();
        if (stashedInv != null)
        {
            context.getInventory().setContents(stashedInv);
        }
        else
        {
            context.getInventory().clear();
        }

        context.get(BasicsAttachment.class).setStashedInventory(invToStash);
        if (stashedArmor != null)
        {
            context.getInventory().setBoots(stashedArmor[0]);
            context.getInventory().setLeggings(stashedArmor[1]);
            context.getInventory().setChestplate(stashedArmor[2]);
            context.getInventory().setHelmet(stashedArmor[3]);
        }
        else
        {
            context.getInventory().setBoots(null);
            context.getInventory().setLeggings(null);
            context.getInventory().setChestplate(null);
            context.getInventory().setHelmet(null);
        }
        context.get(BasicsAttachment.class).setStashedArmor(armorToStash);
        i18n.sendTranslated(context, POSITIVE, "Swapped stashed Inventory!");
    }
}
