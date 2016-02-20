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

import java.util.Optional;
import java.util.UUID;
import javax.persistence.Transient;
import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.service.database.AsyncRecord;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;
import static org.spongepowered.api.Sponge.getGame;

public class TeleportPointModel extends AsyncRecord<TeleportPointModel>
{
    @Transient
    private transient Transform<World> transform;

    public TeleportPointModel()
    {
        super(TABLE_TP_POINT);
    }

    public TeleportPointModel newTPPoint(Transform<World> transform, String name, UUID owner, String welcomeMsg,
                                         TeleportType type, Visibility visibility)
    {
        this.setTransform(transform);

        this.setValue(TABLE_TP_POINT.NAME, name);
        this.setValue(TABLE_TP_POINT.OWNER, owner);
        this.setValue(TABLE_TP_POINT.TYPE, type.value);
        this.setValue(TABLE_TP_POINT.VISIBILITY, visibility.value);

        this.setValue(TABLE_TP_POINT.WELCOMEMSG, welcomeMsg);

        return this;
    }

    public Transform<World> getLocation()
    {
        if (this.transform == null)
        {
            Optional<World> world = getGame().getServer().getWorld(getValue(TABLE_TP_POINT.WORLD));
            this.transform = new Transform<>(world.get(),
                    new Vector3d(getValue(TABLE_TP_POINT.X), getValue(TABLE_TP_POINT.Y), getValue(TABLE_TP_POINT.Z)),
                    new Vector3d(getValue(TABLE_TP_POINT.PITCH), getValue(TABLE_TP_POINT.YAW), 0d));
        }
        return this.transform;
    }

    public void setTransform(Transform<World> transform)
    {
        this.transform = transform;
        this.setValue(TABLE_TP_POINT.WORLD, this.transform.getExtent().getUniqueId());
        this.setValue(TABLE_TP_POINT.X, this.transform.getLocation().getX());
        this.setValue(TABLE_TP_POINT.Y, this.transform.getLocation().getY());
        this.setValue(TABLE_TP_POINT.Z, this.transform.getLocation().getZ());
        this.setValue(TABLE_TP_POINT.YAW, this.transform.getYaw());
        this.setValue(TABLE_TP_POINT.PITCH, this.transform.getPitch());
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
