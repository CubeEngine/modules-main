package org.cubeengine.module.locker.commands;

import org.cubeengine.module.locker.storage.Lock;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

interface LockAction
{
    void apply(Lock lock, Location<World> location, Entity entity);

    interface LockCreateAction extends LockAction {}
}
