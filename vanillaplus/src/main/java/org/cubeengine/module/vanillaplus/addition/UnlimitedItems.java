package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.basics.BasicsAttachment;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class UnlimitedItems
{
    private I18n i18n;

    public UnlimitedItems(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Grants unlimited items")
    @Restricted(Player.class)
    public void unlimited(Player context, @Optional boolean unlimited)
    {
        if (unlimited)
        {
            i18n.sendTranslated(context, POSITIVE, "You now have unlimited items to build!");
        }
        else
        {
            i18n.sendTranslated(context, NEUTRAL, "You no longer have unlimited items to build!");
        }
        context.get(BasicsAttachment.class).setUnlimitedItems(unlimited);
    }

    @Listener
    public void blockplace(final ChangeBlockEvent.Place event, @First Player player)
    {
        if (player.get(BasicsAttachment.class).hasUnlimitedItems())
        {
            java.util.Optional<ItemStack> itemInHand = player.getItemInHand();
            if (itemInHand.isPresent())
            {
                itemInHand.get().setQuantity(itemInHand.get().getQuantity() + 1);
            }
        }
    }
}
