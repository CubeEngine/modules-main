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
package org.cubeengine.module.mail.storage;

import java.util.Optional;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.cubeengine.libcube.service.database.AsyncRecord;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import static org.cubeengine.module.mail.storage.TableMail.TABLE_MAIL;
import static org.spongepowered.api.text.format.TextColors.*;

@Entity
@Table(name = "mail")
public class Mail extends AsyncRecord<Mail>
{
    public Mail()
    {
        super(TABLE_MAIL);
    }

    public Mail newMail(UUID userId, UUID senderId, String message)
    {
        this.setValue(TABLE_MAIL.MESSAGE, message);
        this.setValue(TABLE_MAIL.USERID, userId);
        this.setValue(TABLE_MAIL.SENDERID, senderId);
        return this;
    }

    public Text readMail()
    {
        UUID value = this.getValue(TABLE_MAIL.SENDERID);
        String msg = getValue(TABLE_MAIL.MESSAGE);
        if (value == null)
        {
            return Text.of(RED, "CONSOLE", WHITE, ": ", msg);
        }
        Optional<User> user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(getValue(TABLE_MAIL.SENDERID));
        return Text.of(DARK_GREEN, user.get().getName(), WHITE, ": ", msg);
    }
}
