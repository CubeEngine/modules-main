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
package de.cubeisland.cubeengine.travel.storage;

import de.cubeisland.cubeengine.core.CubeEngine;
import de.cubeisland.cubeengine.core.logger.LogLevel;
import de.cubeisland.cubeengine.core.storage.TwoKeyStorage;
import de.cubeisland.cubeengine.core.storage.database.Database;
import de.cubeisland.cubeengine.core.storage.database.querybuilder.QueryBuilder;
import de.cubeisland.cubeengine.core.user.User;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.cubeisland.cubeengine.travel.Travel;

import static de.cubeisland.cubeengine.core.storage.database.querybuilder.ComponentBuilder.EQUAL;

public class InviteManager extends TwoKeyStorage<Long, Long, TeleportInvite>
{
    private static final int REVISION = 2;
    private final Travel module;
    private Collection<TeleportInvite> invites;

    public InviteManager(Database database, Travel module)
    {
        super(database, TeleportInvite.class, REVISION);
        this.initialize();
        this.module = module;
        this.invites = this.getAll();
    }

    public void initialize()
    {
        try
        {
            super.initialize();
            QueryBuilder builder = database.getQueryBuilder();
            this.database.storeStatement(this.modelClass, "getInvitedTo", builder.select().cols("teleportpoint")
                                                                                 .from(this.tableName).where()
                                                                                 .field("userkey").is(EQUAL).value()
                                                                                 .end().end());
            this.database
                .storeStatement(this.modelClass, "getInvited", builder.select().cols("userkey").from(this.tableName)
                                                                      .where().field("teleportpoint").is(EQUAL).value()
                                                                      .end().end());
        }
        catch (SQLException ex)
        {
            module.getLog().log(LogLevel.ERROR, "An error occurred while preparing the database statements for table " +
                this.tableName);
            module.getLog().log(LogLevel.WARNING, "The error was: {0}", ex.getMessage());
            module.getLog().log(LogLevel.DEBUG, "This is the stack: ", ex);
        }
    }

    public void invite(TeleportPoint tPP, User user)
    {
        TeleportInvite invite = new TeleportInvite(tPP.key, user.getId());
        this.invites.add(invite);
        this.store(invite);
    }

    /**
     * All users invited to a teleport point.
     *
     * @return A set of User objects invited to the home
     */
    public Set<User> getInvitedUsers(TeleportPoint tPP)
    {
        Set<User> invitedUsers = new HashSet<User>();
        for (String name : getInvited(tPP))
        {
            User user = CubeEngine.getUserManager().findOnlineUser(name);
            if (user != null)
            {
                invitedUsers.add(user);
            }
        }
        return invitedUsers;
    }

    /**
     * All users invited to a teleport point.
     *
     * @return A set of User names invited to the home
     */
    public Set<String> getInvited(TeleportPoint tPP)
    {
        Set<String> invitedUsers = new HashSet<String>();
        for (TeleportInvite tpI : getInvites(tPP))
        {
            User user = CubeEngine.getUserManager().getUser(tpI.userKey);
            if (user != null)
            {
                invitedUsers.add(user.getName());
            }
        }
        return invitedUsers;
    }

    /**
     * All teleport invites that contains the user.
     * This can be used to get all teleport points an user is invited to
     *
     * @return A set of TeleportInvites
     */
    public Set<TeleportInvite> getInvites(User user)
    {
        Set<TeleportInvite> invites = new HashSet<TeleportInvite>();
        for (TeleportInvite invite : this.invites)
        {
            if (invite.userKey.equals(user.getId()))
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
    public Set<TeleportInvite> getInvites(TeleportPoint tPP)
    {
        Set<TeleportInvite> invites = new HashSet<TeleportInvite>();
        for (TeleportInvite invite : this.invites)
        {
            if (invite.teleportPoint.equals(tPP.getId()))
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
    public void updateInvited(TeleportPoint tPP, Set<String> newInvited)
    {
        Set<TeleportInvite> invites = getInvites(tPP);
        Set<String> old = getInvited(tPP);
        Set<String> removed = old;
        removed.removeAll(newInvited);
        Set<String> added = newInvited;
        newInvited.removeAll(old);

        for (String user : added)
        {
            this.store(new TeleportInvite(tPP.getId(), CubeEngine.getUserManager().getUser(user, false).getId()));
        }
        for (String user : removed)
        {
            for (TeleportInvite invite : invites)
            {
                if (invite.semiEquals(new TeleportInvite(tPP.getId(), CubeEngine.getUserManager().getUser(user, false)
                                                                                 .getId())))
                {
                    this.delete(invite);
                }
            }
        }
    }
}
