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

import java.util.Set;
import com.flowpowered.math.vector.Vector3d;
import de.cubeisland.engine.service.command.CommandSender;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.service.user.UserManager;
import de.cubeisland.engine.service.world.WorldManager;
import de.cubeisland.engine.module.travel.storage.TeleportPointModel;
import de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility;
import org.jooq.types.UInteger;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.world.Location;

import static de.cubeisland.engine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PUBLIC;

public abstract class TeleportPoint
{
    protected final TeleportPointModel model;
    private WorldManager wm;
    private UserManager um;
    protected final Travel module;
    protected final InviteManager iManager;

    protected PermissionDescription permission;
    protected Set<UInteger> invited;

    protected String ownerName = null;

    public TeleportPoint(TeleportPointModel model, Travel module, WorldManager wm, UserManager um)
    {
        this.model = model;
        this.wm = wm;
        this.um = um;
        this.iManager = module.getInviteManager();
        this.module = module;
    }

    public void update()
    {
        model.updateAsync();
    }

    public Location getLocation()
    {
        Location location = model.getLocation(wm);
        if (location.getExtent() == null)
        {
            module.getLog().warn("Tried to get location from TeleportPoint in deleted world!");
            return null;
        }
        return location;
    }

    public void setLocation(Location location, Vector3d rotation, WorldManager wm)
    {
        model.setLocation(location, rotation, wm);
    }

    public User getOwner()
    {
        return um.getUser(model.getValue(TABLE_TP_POINT.OWNER));
    }

    public void setOwner(User owner)
    {
        this.model.setValue(TABLE_TP_POINT.OWNER, owner.getEntity().getId());
    }

    public boolean isOwnedBy(CommandSender user)
    {
        if (user instanceof User)
        {
            return model.getValue(TABLE_TP_POINT.OWNER).equals(((User)user).getEntity().getId());
        }
        return false;
    }

    public void invite(User user)
    {
        if (this.invited == null)
        {
            this.invited = iManager.getInvited(model);
        }
        this.invited.add(user.getEntity().getId());
        iManager.invite(this.getModel(), user);
    }

    public void unInvite(User user)
    {
        if (this.invited == null)
        {
            this.invited = iManager.getInvited(model);
        }
        this.invited.remove(user.getEntity().getId());
        iManager.updateInvited(this.model, this.invited);
    }

    public boolean isInvited(User user)
    {
        return this.getInvited().contains(user.getEntity().getId()) || this.isPublic();
    }

    public void setVisibility(Visibility visibility)
    {
        model.setValue(TABLE_TP_POINT.VISIBILITY, visibility.value);
    }

    public Visibility getVisibility()
    {
        return Visibility.valueOf(model.getValue(TABLE_TP_POINT.VISIBILITY));
    }

    public Set<UInteger> getInvited()
    {
        if (this.invited == null)
        {
            this.invited = iManager.getInvited(model);
        }
        return this.invited;
    }

    public TeleportPointModel getModel()
    {
        return model;
    }

    public String getOwnerName()
    {
        if (this.ownerName == null)
        {
            this.ownerName = um.getUserName(model.getValue(TABLE_TP_POINT.OWNER));
        }
        return this.ownerName;
    }

    public String getName()
    {
        return model.getValue(TABLE_TP_POINT.NAME);
    }

    public void setName(String name)
    {
        model.setValue(TABLE_TP_POINT.NAME, name);
    }

    public String getWelcomeMsg()
    {
        String value = model.getValue(TABLE_TP_POINT.WELCOMEMSG);
        if (value == null || value.isEmpty())
        {
            return null;
        }
        return value;
    }

    public void setWelcomeMsg(String welcomeMsg)
    {
        model.setValue(TABLE_TP_POINT.WELCOMEMSG, welcomeMsg);
    }

    public boolean isPublic()
    {
        return this.getVisibility() == PUBLIC;
    }

    public boolean canAccess(User user)
    {
        return this.isPublic() ? user.hasPermission(permission.getId()) : (this.isInvited(user) || this.isOwnedBy(user));
    }

    protected abstract PermissionDescription generatePublicPerm();
}
