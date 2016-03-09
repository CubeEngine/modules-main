package org.cubeengine.module.travel;

import org.cubeengine.butler.exception.CommandException;
import org.cubeengine.module.travel.config.TeleportPoint;

public class MissingWorldException extends CommandException
{
    private TeleportPoint point;

    public MissingWorldException(org.cubeengine.module.travel.config.TeleportPoint point)
    {
        this.point = point;
    }

    public TeleportPoint getPoint()
    {
        return point;
    }
}
