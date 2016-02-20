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
import java.util.UUID;
import org.cubeengine.module.travel.storage.TeleportPointModel;
import org.cubeengine.module.travel.storage.TeleportPointModel.Visibility;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;

public abstract class TeleportPoint
{
    protected final TeleportPointModel model;
    protected final Travel module;
    protected final InviteManager iManager;

    protected PermissionDescription permission;
    protected Set<UUID> invited;

    protected String ownerName = null;

    public TeleportPoint(TeleportPointModel model, Travel module)
    {
        this.model = model;
        this.iManager = module.getInviteManager();
        this.module = module;
    }

    public void update()
    {
        model.updateAsync();
    }

    public Transform<World> getTransform()
    {
        Transform<World> location = model.getLocation();
        if (location.getExtent() == null)
        {
            module.getLog().warn("Tried to get location from TeleportPoint in deleted world!");
            return null;
        }
        return location;
    }

    public void setLocation(Transform<World> transform)
    {
        model.setTransform(transform);
    }

    public User getOwner()
    {
        return Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(model.getValue(TABLE_TP_POINT.OWNER)).get();
    }

    public void setOwner(Player owner)
    {
        this.model.setValue(TABLE_TP_POINT.OWNER, owner.getUniqueId());
    }

    public boolean isOwnedBy(CommandSource user)
    {
        if (user instanceof Player)
        {
            return model.getValue(TABLE_TP_POINT.OWNER).equals(((Player)user).getUniqueId());
        }
        return false;
    }

    public void invite(Player user)
    {
        if (this.invited == null)
        {
            this.invited = iManager.getInvited(model);
        }
        this.invited.add(user.getUniqueId());
        iManager.invite(this.getModel(), user.getUniqueId());
    }

    public void unInvite(Player user)
    {
        if (this.invited == null)
        {
            this.invited = iManager.getInvited(model);
        }
        this.invited.remove(user.getUniqueId());
        iManager.updateInvited(this.model, this.invited);
    }

    public boolean isInvited(Player user)
    {
        return this.getInvited().contains(user.getUniqueId()) || this.isPublic();
    }

    public void setVisibility(Visibility visibility)
    {
        model.setValue(TABLE_TP_POINT.VISIBILITY, visibility.value);
    }

    public Visibility getVisibility()
    {
        return Visibility.valueOf(model.getValue(TABLE_TP_POINT.VISIBILITY));
    }

    public Set<UUID> getInvited()
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
            this.ownerName = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(model.getValue(TABLE_TP_POINT.OWNER)).get().getName();
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
