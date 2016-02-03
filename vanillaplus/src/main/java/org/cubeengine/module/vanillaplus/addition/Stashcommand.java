package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.service.command.CommandContext;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class StashCommand
{

    @Command(desc = "Stashes or unstashes your inventory to reuse later")
    @Restricted(value = User.class, msg = "Yeah you better put it away!")
    public void stash(CommandContext context)
    {
        User sender = (User)context.getSource();
        ItemStack[] stashedInv = sender.get(BasicsAttachment.class).getStashedInventory();
        ItemStack[] stashedArmor = sender.get(BasicsAttachment.class).getStashedArmor();
        ItemStack[] invToStash = sender.getInventory().getContents().clone();
        ItemStack[] armorToStash = sender.getInventory().getArmorContents().clone();
        if (stashedInv != null)
        {
            sender.getInventory().setContents(stashedInv);
        }
        else
        {
            sender.getInventory().clear();
        }

        sender.get(BasicsAttachment.class).setStashedInventory(invToStash);
        if (stashedArmor != null)
        {
            sender.getInventory().setBoots(stashedArmor[0]);
            sender.getInventory().setLeggings(stashedArmor[1]);
            sender.getInventory().setChestplate(stashedArmor[2]);
            sender.getInventory().setHelmet(stashedArmor[3]);
        }
        else
        {
            sender.getInventory().setBoots(null);
            sender.getInventory().setLeggings(null);
            sender.getInventory().setChestplate(null);
            sender.getInventory().setHelmet(null);
        }
        sender.get(BasicsAttachment.class).setStashedArmor(armorToStash);
        i18n.sendTranslated(sender, POSITIVE, "Swapped stashed Inventory!");
    }

}
