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
package de.cubeisland.engine.module.travel.storage;

import javax.persistence.Transient;

import org.bukkit.Location;

import de.cubeisland.engine.core.user.User;
import org.apache.commons.lang.Validate;
import org.jooq.impl.UpdatableRecordImpl;

import static de.cubeisland.engine.core.CubeEngine.getCore;
import static de.cubeisland.engine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;

public class TeleportPointModel extends UpdatableRecordImpl<TeleportPointModel>
{
    @Transient
    private transient Location location;

    public TeleportPointModel()
    {
        super(TABLE_TP_POINT);
    }

    public TeleportPointModel newTPPoint(Location location, String name, User owner, String welcomeMsg, TeleportType type,
                                         Visibility visibility)
    {
        Validate.notNull(location);
        Validate.notEmpty(name);
        Validate.notNull(owner);
        Validate.notNull(type);
        Validate.notNull(visibility);

        this.setLocation(location);

        this.setValue(TABLE_TP_POINT.NAME, name);
        this.setValue(TABLE_TP_POINT.OWNER, owner.getEntity().getKey());
        this.setValue(TABLE_TP_POINT.TYPE, type.value);
        this.setValue(TABLE_TP_POINT.VISIBILITY, visibility.value);


        this.setValue(TABLE_TP_POINT.WELCOMEMSG, welcomeMsg);

        return this;
    }

    public Location getLocation()
    {
        if (this.location == null)
        {
            this.location = new Location(getCore().getWorldManager().getWorld(getValue(TABLE_TP_POINT.WORLD)),
                                         getValue(TABLE_TP_POINT.X),
                                         getValue(TABLE_TP_POINT.Y),
                                         getValue(TABLE_TP_POINT.Z),
                                         getValue(TABLE_TP_POINT.YAW).floatValue(),
                                         getValue(TABLE_TP_POINT.PITCH).floatValue());
        }
        return this.location;
    }

    public void setLocation(Location location)
    {
        this.location = location;
        this.setValue(TABLE_TP_POINT.WORLD, getCore().getWorldManager().getWorldId(location.getWorld()));
        this.setValue(TABLE_TP_POINT.X, location.getX());
        this.setValue(TABLE_TP_POINT.Y, location.getY());
        this.setValue(TABLE_TP_POINT.Z, location.getZ());
        this.setValue(TABLE_TP_POINT.YAW, (double)location.getYaw());
        this.setValue(TABLE_TP_POINT.PITCH, (double)location.getPitch());
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
