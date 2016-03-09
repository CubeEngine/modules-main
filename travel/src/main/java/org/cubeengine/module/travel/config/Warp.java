package org.cubeengine.module.travel.config;

import de.cubeisland.engine.reflect.Section;
import org.spongepowered.api.entity.living.player.User;

public class Warp extends TeleportPoint implements Section
{
    public boolean isAllowed(User user)
    {
        return user.hasPermission("cubeengine.travel.warps.access." + name);
    }

}
