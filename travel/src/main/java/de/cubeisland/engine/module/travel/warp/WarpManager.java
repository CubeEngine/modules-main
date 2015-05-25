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
package de.cubeisland.engine.module.travel.warp;

import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.travel.InviteManager;
import de.cubeisland.engine.module.travel.TelePointManager;
import de.cubeisland.engine.module.travel.Travel;
import de.cubeisland.engine.module.travel.storage.TeleportPointModel;
import org.bukkit.Location;

import static de.cubeisland.engine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.TeleportType.WARP;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PRIVATE;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PUBLIC;

public class WarpManager extends TelePointManager<Warp>
{
    public WarpManager(Travel module, InviteManager iManager)
    {
        super(module, iManager);
    }

    @Override
    public void load()
    {
        for (TeleportPointModel teleportPoint : this.dsl.selectFrom(TABLE_TP_POINT).where(TABLE_TP_POINT.TYPE.eq(WARP.value)).fetch())
        {
            this.addPoint(new Warp(teleportPoint, this.module));
        }
        module.getLog().info("{} Homes loaded", this.getCount());
    }

    @Override
    public Warp create(User owner, String name, Location location, boolean publicVisibility)
    {
        if (this.has(owner, name))
        {
            throw new IllegalArgumentException("Tried to create duplicate warp!");
        }
        TeleportPointModel model = this.dsl.newRecord(TABLE_TP_POINT).newTPPoint(location, name, owner, null, WARP, publicVisibility ? PUBLIC : PRIVATE);
        Warp warp = new Warp(model, this.module);
        model.insertAsync();
        this.addPoint(warp);
        return warp;
    }
}
