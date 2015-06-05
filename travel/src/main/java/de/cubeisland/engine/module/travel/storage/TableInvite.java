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

import de.cubeisland.engine.module.core.util.Version;
import de.cubeisland.engine.module.service.database.Database;
import de.cubeisland.engine.module.service.database.Table;
import de.cubeisland.engine.module.service.user.TableUser;
import org.jooq.TableField;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.module.service.user.TableUser.TABLE_USER;
import static de.cubeisland.engine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;

public class TableInvite extends Table<TeleportInvite>
{
    public static TableInvite TABLE_INVITE;
    public final TableField<TeleportInvite, UInteger> TELEPORTPOINT = createField("teleportpoint", U_INTEGER.nullable(false), this);
    public final TableField<TeleportInvite, UInteger> USERKEY = createField("userkey", U_INTEGER.nullable(false), this);

    public TableInvite(String prefix, Database db)
    {
        super(prefix + "teleportinvites", new Version(1), db);
        this.setPrimaryKey(USERKEY, TELEPORTPOINT);
        this.addForeignKey(TABLE_USER.getPrimaryKey(), USERKEY);
        this.addForeignKey(TABLE_TP_POINT.getPrimaryKey(), TELEPORTPOINT);
        this.addFields(TELEPORTPOINT, USERKEY);
        TABLE_INVITE = this;
    }

    @Override
    public Class<TeleportInvite> getRecordType()
    {
        return TeleportInvite.class;
    }
}
