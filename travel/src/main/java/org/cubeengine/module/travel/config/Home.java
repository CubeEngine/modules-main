package org.cubeengine.module.travel.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import de.cubeisland.engine.reflect.Section;
import org.spongepowered.api.entity.living.player.User;

public class Home extends TeleportPoint implements Section
{
    public List<UUID> invites = new ArrayList<>();

    public boolean isInvited(User user)
    {
        return invites.contains(user.getUniqueId());
    }
}
