package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.basics.BasicsAttachment;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class UnlimitedItems
{
    @Command(desc = "Grants unlimited items")
    @Restricted(User.class)
    public void unlimited(User context, @Optional boolean unlimited)
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

    @Subscribe
    public void blockplace(final PlayerPlaceBlockEvent event)
    {
        User user = um.getExactUser(event.getUser().getUniqueId());
        if (user.get(BasicsAttachment.class).hasUnlimitedItems())
        {
            com.google.common.base.Optional<ItemStack> itemInHand = event.getUser().getItemInHand();
            if (itemInHand.isPresent())
            {
                itemInHand.get().setQuantity(itemInHand.get().getQuantity() + 1);
            }
        }
    }
}
