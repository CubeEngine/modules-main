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
package de.cubeisland.engine.module.locker.storage;

import org.bukkit.Location;
import org.jooq.impl.UpdatableRecordImpl;

import static de.cubeisland.engine.core.CubeEngine.getCore;
import static de.cubeisland.engine.module.locker.storage.TableLockLocations.TABLE_LOCK_LOCATION;
import static de.cubeisland.engine.module.locker.storage.TableLocks.TABLE_LOCK;

public class LockLocationModel extends UpdatableRecordImpl<LockLocationModel>
{
    public LockLocationModel()
    {
        super(TABLE_LOCK_LOCATION);
    }

    public LockLocationModel newLocation(LockModel model, Location location)
    {
        this.setLocation(location);
        this.setValue(TABLE_LOCK_LOCATION.LOCK_ID, model.getValue(TABLE_LOCK.ID));
        return this;
    }

    private void setLocation(Location location)
    {
        this.setValue(TABLE_LOCK_LOCATION.WORLD_ID, getCore().getWorldManager().getWorldId(location.getWorld()));
        this.setValue(TABLE_LOCK_LOCATION.X, location.getBlockX());
        this.setValue(TABLE_LOCK_LOCATION.Y, location.getBlockY());
        this.setValue(TABLE_LOCK_LOCATION.Z, location.getBlockZ());
        this.setValue(TABLE_LOCK_LOCATION.CHUNKX, location.getChunk().getX());
        this.setValue(TABLE_LOCK_LOCATION.CHUNKZ, location.getChunk().getZ());
    }
}
