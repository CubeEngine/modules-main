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
package org.cubeengine.module.travel;

import java.util.Set;
import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.module.travel.storage.TeleportPointModel;
import org.cubeengine.module.travel.storage.TeleportPointModel.Visibility;
import org.cubeengine.service.user.CachedUser;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldManager;
import org.jooq.types.UInteger;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;

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

    public Transform<World> getTransform()
    {
        Transform<World> location = model.getLocation(wm);
        if (location.getExtent() == null)
        {
            module.getLog().warn("Tried to get location from TeleportPoint in deleted world!");
            return null;
        }
        return location;
    }

    public void setLocation(Transform<World> transform)
    {
        model.setTransform(transform, wm);
    }

    public CachedUser getOwner()
    {
        return um.getById(model.getValue(TABLE_TP_POINT.OWNER)).get();
    }

    public void setOwner(Player owner)
    {

        this.model.setValue(TABLE_TP_POINT.OWNER, um.getByUUID(owner.getUniqueId()).getEntity().getId());
    }

    public boolean isOwnedBy(CommandSource user)
    {
        if (user instanceof Player)
        {
            return model.getValue(TABLE_TP_POINT.OWNER).equals(um.getByUUID(
                ((Player)user).getUniqueId()).getEntity().getId());
        }
        return false;
    }

    public void invite(Player user)
    {
        if (this.invited == null)
        {
            this.invited = iManager.getInvited(model);
        }
        UInteger id = um.getByUUID(user.getUniqueId()).getEntity().getId();
        this.invited.add(id);
        iManager.invite(this.getModel(), id);
    }

    public void unInvite(Player user)
    {
        if (this.invited == null)
        {
            this.invited = iManager.getInvited(model);
        }
        this.invited.remove(um.getByUUID(user.getUniqueId()).getEntity().getId());
        iManager.updateInvited(this.model, this.invited);
    }

    public boolean isInvited(Player user)
    {
        return this.getInvited().contains(um.getByUUID(user.getUniqueId()).getEntity().getId()) || this.isPublic();
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
        return this.getVisibility() == Visibility.PUBLIC;
    }

    public boolean canAccess(Player user)
    {
        return this.isPublic() ? user.hasPermission(permission.getId()) : (this.isInvited(user) || this.isOwnedBy(user));
    }

    protected abstract PermissionDescription generatePublicPerm();
}
