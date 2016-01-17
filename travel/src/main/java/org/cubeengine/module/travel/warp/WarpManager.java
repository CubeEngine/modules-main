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

import org.cubeengine.module.travel.InviteManager;
import org.cubeengine.module.travel.TelePointManager;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.storage.TableTeleportPoint;
import org.cubeengine.module.travel.storage.TeleportPointModel;
import org.cubeengine.module.travel.storage.TeleportPointModel.TeleportType;
import org.cubeengine.module.travel.storage.TeleportPointModel.Visibility;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

public class WarpManager extends TelePointManager<Warp>
{
    private PermissionManager pm;

    public WarpManager(Travel module, InviteManager iManager, Database db, PermissionManager pm)
    {
        super(module, iManager, db);
        this.pm = pm;
    }

    @Override
    public void load()
    {
        for (TeleportPointModel teleportPoint : this.dsl.selectFrom(TableTeleportPoint.TABLE_TP_POINT).where(
            TableTeleportPoint.TABLE_TP_POINT.TYPE.eq(TeleportType.WARP.value)).fetch())
        {
            this.addPoint(new Warp(teleportPoint, this.module, pm));
        }
        module.getLog().info("{} Homes loaded", this.getCount());
    }

    @Override
    public Warp create(Player owner, String name, Transform<World> transform, boolean publicVisibility)
    {
        if (this.has(owner, name))
        {
            throw new IllegalArgumentException("Tried to create duplicate warp!");
        }
        TeleportPointModel model = this.dsl.newRecord(TableTeleportPoint.TABLE_TP_POINT).newTPPoint(transform, name, owner.getUniqueId(), null, TeleportType.WARP, publicVisibility ? Visibility.PUBLIC : Visibility.PRIVATE);
        Warp warp = new Warp(model, this.module, pm);
        model.insertAsync();
        this.addPoint(warp);
        return warp;
    }
}
