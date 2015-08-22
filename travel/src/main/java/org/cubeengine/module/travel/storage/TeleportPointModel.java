/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.travel.storage;

import javax.persistence.Transient;
import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.service.database.AsyncRecord;
import org.cubeengine.service.user.User;
import org.cubeengine.service.world.WorldManager;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;

public class TeleportPointModel extends AsyncRecord<TeleportPointModel>
{
    @Transient
    private transient Location location;

    public TeleportPointModel()
    {
        super(TABLE_TP_POINT);
    }

    public TeleportPointModel newTPPoint(Location location, Vector3d rotation, WorldManager wm, String name, User owner, String welcomeMsg, TeleportType type,
                                         Visibility visibility)
    {
        this.setLocation(location, rotation, wm);

        this.setValue(TABLE_TP_POINT.NAME, name);
        this.setValue(TABLE_TP_POINT.OWNER, owner.getEntity().getId());
        this.setValue(TABLE_TP_POINT.TYPE, type.value);
        this.setValue(TABLE_TP_POINT.VISIBILITY, visibility.value);


        this.setValue(TABLE_TP_POINT.WELCOMEMSG, welcomeMsg);

        return this;
    }

    public Location getLocation(WorldManager wm)
    {
        if (this.location == null)
        {
            this.location = new Location(wm.getWorld(getValue(TABLE_TP_POINT.WORLD)),
                                         getValue(TABLE_TP_POINT.X),
                                         getValue(TABLE_TP_POINT.Y),
                                         getValue(TABLE_TP_POINT.Z));
            // TODO rotation
            //getValue(TABLE_TP_POINT.YAW).floatValue(),
            //getValue(TABLE_TP_POINT.PITCH).floatValue()
        }
        return this.location;
    }

    public void setLocation(Location location, Vector3d rotation, WorldManager wm)
    {
        this.location = location;
        this.setValue(TABLE_TP_POINT.WORLD, wm.getWorldId((World)location.getExtent()));
        this.setValue(TABLE_TP_POINT.X, location.getX());
        this.setValue(TABLE_TP_POINT.Y, location.getY());
        this.setValue(TABLE_TP_POINT.Z, location.getZ());

        // TODO rotation
        this.setValue(TABLE_TP_POINT.YAW, 0d);
        this.setValue(TABLE_TP_POINT.PITCH, 0d);
    }

    public enum TeleportType
    {
        HOME(0),
        WARP(1);
        public final short value;

        TeleportType(int value)
        {
            this.value = (short)value;
        }
    }

    public enum Visibility
    {
        PUBLIC(0),
        PRIVATE(1);

        public final short value;

        Visibility(int value)
        {
            this.value = (short)value;
        }

        public static Visibility valueOf(Short value)
        {
            switch (value)
            {
                case 0: return PUBLIC;
                case 1: return PRIVATE;
            }
            return null;
        }
    }
}
