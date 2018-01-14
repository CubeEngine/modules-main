/*
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
package org.cubeengine.module.locker.storage;

import org.cubeengine.module.sql.database.AsyncRecord;
import org.spongepowered.api.world.Location;

import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCKS;

public class LockLocationModel extends AsyncRecord<LockLocationModel>
{
    public LockLocationModel()
    {
        super(TableLockLocations.TABLE_LOCK_LOCATIONS);
    }

    public LockLocationModel newLocation(LockModel model, Location location)
    {
        this.setLocation(location);
        this.setValue(TableLockLocations.TABLE_LOCK_LOCATIONS.LOCK_ID, model.getValue(TABLE_LOCKS.ID));
        return this;
    }

    private void setLocation(Location location)
    {
        this.setValue(TableLockLocations.TABLE_LOCK_LOCATIONS.WORLD_ID, location.getExtent().getUniqueId());
        this.setValue(TableLockLocations.TABLE_LOCK_LOCATIONS.X, location.getBlockX());
        this.setValue(TableLockLocations.TABLE_LOCK_LOCATIONS.Y, location.getBlockY());
        this.setValue(TableLockLocations.TABLE_LOCK_LOCATIONS.Z, location.getBlockZ());
        this.setValue(TableLockLocations.TABLE_LOCK_LOCATIONS.CHUNKX, location.getBlockX() >> 4);
        this.setValue(TableLockLocations.TABLE_LOCK_LOCATIONS.CHUNKZ, location.getBlockZ() >> 4);
    }
}
