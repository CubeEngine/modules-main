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
package org.cubeengine.module.travel.warp;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.module.travel.InviteManager;
import org.cubeengine.module.travel.TelePointManager;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.storage.TableTeleportPoint;
import org.cubeengine.module.travel.storage.TeleportPointModel;
import org.cubeengine.module.travel.storage.TeleportPointModel.TeleportType;
import org.cubeengine.module.travel.storage.TeleportPointModel.Visibility;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.user.MultilingualPlayer;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldManager;
import org.spongepowered.api.world.Location;

public class WarpManager extends TelePointManager<Warp>
{
    private PermissionManager pm;
    private WorldManager wm;
    private UserManager um;

    public WarpManager(Travel module, InviteManager iManager, Database db, PermissionManager pm, WorldManager wm, UserManager um)
    {
        super(module, iManager, db, um);
        this.pm = pm;
        this.wm = wm;
        this.um = um;
    }

    @Override
    public void load()
    {
        for (TeleportPointModel teleportPoint : this.dsl.selectFrom(TableTeleportPoint.TABLE_TP_POINT).where(
            TableTeleportPoint.TABLE_TP_POINT.TYPE.eq(TeleportType.WARP.value)).fetch())
        {
            this.addPoint(new Warp(teleportPoint, this.module, pm, wm, um));
        }
        module.getLog().info("{} Homes loaded", this.getCount());
    }

    @Override
    public Warp create(MultilingualPlayer owner, String name, Location location, Vector3d rotation, boolean publicVisibility)
    {
        if (this.has(owner, name))
        {
            throw new IllegalArgumentException("Tried to create duplicate warp!");
        }
        TeleportPointModel model = this.dsl.newRecord(TableTeleportPoint.TABLE_TP_POINT).newTPPoint(location, rotation, wm, name, um.getByUUID(owner.getUniqueId()).getEntityId(), null, TeleportType.WARP, publicVisibility ? Visibility.PUBLIC : Visibility.PRIVATE);
        Warp warp = new Warp(model, this.module, pm, wm, um);
        model.insertAsync();
        this.addPoint(warp);
        return warp;
    }
}
