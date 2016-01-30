package org.cubeengine.module.vanillaplus;

import java.util.UUID;
import com.google.common.base.Optional;
import org.spongepowered.api.data.key.Keys;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class TamedListener
{
    @Subscribe
    public void onInteractWithTamed(PlayerInteractEntityEvent event)
    {
        UUID uuid = event.getTargetEntity().get(Keys.TAMED_OWNER).or(Optional.<UUID>absent()).orNull();
        if (uuid != null)
        {

            if (!event.getUser().getUniqueId().equals(uuid))
            {
                User clicker = um.getExactUser(event.getUser().getUniqueId());
                User owner = um.getExactUser(uuid);
                clicker.sendTranslated(POSITIVE, "This {name#entity} belongs to {tamer}!",
                                       event.getEntity().getType().getName(), owner);
            }
        }
    }
}
