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
package de.cubeisland.engine.module.travel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.cubeisland.engine.core.storage.database.Database;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.travel.storage.TeleportInvite;
import de.cubeisland.engine.module.travel.storage.TeleportPointModel;
import org.jooq.DSLContext;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.module.travel.storage.TableInvite.TABLE_INVITE;
import static de.cubeisland.engine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;

public class InviteManager
{
    private final Travel module;
    private final DSLContext dsl;
    private final Collection<TeleportInvite> invites;
    private final Map<TeleportPointModel, Set<UInteger>> cachedInvites;

    public InviteManager(Database database, Travel module)
    {
        this.dsl = database.getDSL();
        this.module = module;
        this.cachedInvites = new HashMap<>();
        this.invites = this.dsl.selectFrom(TABLE_INVITE).fetch(); // TODO this can be a big query :S
    }

    public void invite(TeleportPointModel tPP, User user)
    {
        TeleportInvite invite = this.dsl.newRecord(TABLE_INVITE).newInvite(tPP.getValue(TABLE_TP_POINT.KEY), user.getEntity().getKey());
        this.invites.add(invite);
        invite.insertAsync();
    }

    /**
     * All users invited to a teleport point.
     *
     * @return A set of User names invited to the home
     */
    public Set<UInteger> getInvited(TeleportPointModel tPP)
    {
        if (this.cachedInvites.containsKey(tPP))
        {
            return this.cachedInvites.get(tPP);
        }
        Set<UInteger> keys = new HashSet<>();
        for (TeleportInvite tpI : getInvites(tPP))
        {
            keys.add(tpI.getValue(TABLE_INVITE.USERKEY));
        }
        this.cachedInvites.put(tPP, keys);
        return keys;
    }

    /**
     * All teleport invites that contains the user.
     * This can be used to get all teleport points an user is invited to
     *
     * @return A set of TeleportInvites
     */
    public Set<TeleportInvite> getInvites(User user)
    {
        Set<TeleportInvite> invites = new HashSet<>();
        for (TeleportInvite invite : this.invites)
        {
            if (invite.getValue(TABLE_INVITE.USERKEY).equals(user.getEntity().getKey()))
            {
                invites.add(invite);
            }
        }
        return invites;
    }

    /**
     * All teleport invites that contains the teleport point
     * This can be used to get all users that is invited to a teleport point
     *
     * @return A set of TeleportInvites
     */
    public Set<TeleportInvite> getInvites(TeleportPointModel tPP)
    {
        Set<TeleportInvite> invites = new HashSet<>();
        for (TeleportInvite invite : this.invites)
        {
            if (invite.getValue(TABLE_INVITE.TELEPORTPOINT).equals(tPP.getValue(TABLE_TP_POINT.KEY)))
            {
                invites.add(invite);
            }
        }
        return invites;
    }

    /**
     * Update the local changes to the database
     *
     * @param tPP        The local teleport point
     * @param newInvited The users that is currently invited to the teleportpoint locally
     */
    public void updateInvited(TeleportPointModel tPP, Set<UInteger> newInvited)
    {
        Set<TeleportInvite> invites = getInvites(tPP);
        Set<UInteger> invitedUsers = new HashSet<>();
        for (UInteger uid : newInvited)
        {
            invitedUsers.add(this.module.getCore().getUserManager().getUser(uid).getEntity().getKey());
        }
        for (TeleportInvite invite : invites)
        {
            if (invitedUsers.contains(invite.getValue(TABLE_INVITE.USERKEY)))
            {
                invitedUsers.remove(invite.getValue(TABLE_INVITE.USERKEY)); // already invited
            }
            else
            {
                invite.deleteAsync(); // no longer invited
            }
        }
        for (UInteger invitedUser : invitedUsers)
        {
            this.dsl.newRecord(TABLE_INVITE).newInvite(tPP.getValue(TABLE_TP_POINT.KEY), invitedUser).insertAsync(); // not yet invited
        }
    }

    public void removeInvites(TeleportPoint tPP)
    {
        this.updateInvited(tPP.getModel(), new HashSet<UInteger>());
    }
}
