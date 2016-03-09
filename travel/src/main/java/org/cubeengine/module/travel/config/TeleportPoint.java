package org.cubeengine.module.travel.config;

import java.util.UUID;
import de.cubeisland.engine.reflect.Section;
import org.cubeengine.service.world.ConfigWorld;
import org.cubeengine.service.world.WorldTransform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.World;

public class TeleportPoint implements Section
{
    public String name;

    public UUID owner;
    public ConfigWorld world;
    public WorldTransform transform;

    public String welcomeMsg;

    public void setTransform(Transform<World> transform)
    {
        this.transform = new WorldTransform(transform.getLocation(), transform.getRotation());
        this.world = new ConfigWorld(transform.getExtent());
    }

    public User getOwner()
    {
        return Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(owner).orElse(null);
    }

    public boolean isOwner(CommandSource cmdSource)
    {
        return getOwner().getIdentifier().equals(cmdSource.getIdentifier());
    }
}
