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
package org.cubeengine.module.conomy;

import org.cubeengine.module.conomy.storage.AccountModel;
import org.cubeengine.service.database.Database;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;

import java.util.UUID;

public class UserAccount extends BaseAccount implements UniqueAccount
{
    private final UUID uuid;
    private final Text display;

    public UserAccount(User user, ConomyService service, AccountModel account, Database db)
    {
        super(service, account, db);
        this.uuid = user.getUniqueId();
        this.display = Text.of(user.getName());
    }

    @Override
    public UUID getUUID()
    {
        return uuid;
    }

    @Override
    public Text getDisplayName()
    {
        return display;
    }

    @Override
    public String getIdentifier()
    {
        return uuid.toString();
    }
}
