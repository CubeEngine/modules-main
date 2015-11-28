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
package org.cubeengine.module.travel.home;

import org.cubeengine.module.travel.InviteManager;
import org.cubeengine.module.travel.TelePointManager;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.storage.TableTeleportPoint;
import org.cubeengine.module.travel.storage.TeleportPointModel;
import org.cubeengine.module.travel.storage.TeleportPointModel.TeleportType;
import org.cubeengine.module.travel.storage.TeleportPointModel.Visibility;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldManager;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.travel.storage.TeleportPointModel.TeleportType.HOME;
import static org.cubeengine.module.travel.storage.TeleportPointModel.Visibility.PRIVATE;
import static org.cubeengine.module.travel.storage.TeleportPointModel.Visibility.PUBLIC;

public class HomeManager extends TelePointManager<Home>
{
    private PermissionManager pm;
    private WorldManager wm;
    private UserManager um;

    public HomeManager(Travel module, InviteManager iManager, Database db, PermissionManager pm, WorldManager wm, UserManager um)
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
            TableTeleportPoint.TABLE_TP_POINT.TYPE.eq(HOME.value)).fetch())
        {
            this.addPoint(new Home(teleportPoint, this.module, pm, wm, um));
        }
        module.getLog().info("{} Homes loaded", this.getCount());
    }

    @Override
    public Home create(Player owner, String name, Transform<World> transform, boolean publicVisibility)
    {
        if (this.has(owner, name))
        {
            throw new IllegalArgumentException("Tried to create duplicate home!");
        }
        TeleportPointModel model = this.dsl.newRecord(TableTeleportPoint.TABLE_TP_POINT)
                .newTPPoint(transform, wm, name, um.getByUUID(owner.getUniqueId()).getEntityId(), null, HOME, publicVisibility ? PUBLIC : PRIVATE);
        Home home = new Home(model, this.module, pm, wm, um);
        model.insertAsync().exceptionally(this::handle);
        this.addPoint(home);
        return home;
    }

    private Integer handle(Throwable throwable)
    {
        module.getLog().error(throwable, "Could not store Home");
        return 0;
    }
}
