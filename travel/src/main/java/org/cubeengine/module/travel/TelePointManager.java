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

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.module.core.util.math.Cuboid;
import org.cubeengine.module.core.util.math.Vector3;
import org.cubeengine.service.database.Database;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.module.travel.storage.TeleportPointModel;
import org.jooq.DSLContext;
import org.jooq.types.UInteger;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;

public abstract class TelePointManager<T extends TeleportPoint>
{
    protected final Travel module;
    protected final DSLContext dsl;
    protected final InviteManager iManager;
    protected final Map<String, Map<String, T>> points = new HashMap<>();

    public TelePointManager(Travel module, InviteManager iManager, Database db)
    {
        this.dsl = db.getDSL();
        this.module = module;
        this.iManager = iManager;
    }

    public abstract void load();

    protected void addPoint(T point)
    {
        Map<String, T> map = points.get(point.getName());
        if (map == null)
        {
            map = new HashMap<>();
            points.put(point.getName(), map);
        }
        map.put(point.getOwnerName().toLowerCase(), point);
    }

    public TeleportPointModel get(Long key)
    {
        return this.dsl.selectFrom(TABLE_TP_POINT).where(TABLE_TP_POINT.KEY.eq(UInteger.valueOf(key))).fetchOne();
    }

    /**
     * Returns a TeleportPoint given user has access to
     * <p>Favouring owned over public over invited TeleportPoints
     *
     * @param user
     * @param name
     * @return
     */
    public T findOne(Player user, String name)
    {
        Map<String, T> map = this.points.get(name);
        if (map == null)
        {
            for (String found : findIn(name, this.points.keySet()))
            {
                map = this.points.get(found);
                break;
            }
            if (map == null)
            {
                return null;
            }
        }
        for (T point : map.values())
        {
            if (point.isOwnedBy(user))
            {
                return point;
            }
        }
        for (T point : map.values())
        {
            if (point.isPublic())
            {
                return point;
            }
        }
        T match = null;
        for (T point : map.values())
        {
            if (point.isInvited(user))
            {
                return point;
            }
            match = point;
        }
        return match;
    }

    public T getExact(Player user, String name)
    {
        Map<String, T> map = this.points.get(name);
        return map == null ? null : map.get(user.getName().toLowerCase());
    }

    public boolean has(Player user, String name)
    {
        Map<String, T> map = this.points.get(name);
        if (map != null)
        {
            for (T value : map.values())
            {
                if (value.isOwnedBy(user))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public int getCount(Player user)
    {

        return dsl.fetchCount(dsl.selectFrom(TABLE_TP_POINT).where(TABLE_TP_POINT.OWNER.eq(user.getUniqueId())));
    }

    public abstract T create(Player owner, String name, Transform<World> location, boolean publicVisibility);

    public void delete(T point)
    {
        point.getModel().deleteAsync();
        this.removePoint(point);
    }

    private void removePoint(T point)
    {
        this.points.get(point.getName()).remove(point.getOwnerName().toLowerCase());
    }

    public Set<T> list(boolean privates, boolean publics)
    {
        if (!privates && !publics)
        {
            privates = true;
            publics = true;
        }
        HashSet<T> set = new HashSet<>();
        for (Map<String, T> map : this.points.values())
        {
            for (T point : map.values())
            {
                if (publics && point.isPublic())
                {
                    set.add(point);
                }
                if (privates && !point.isPublic())
                {
                    set.add(point);
                }
            }
        }
        return set;
    }

    public Set<T> list(Player user, boolean owned, boolean publics, boolean invited)
    {
        if (!owned && !publics && !invited)
        {
            owned = true;
            publics = true;
            invited = true;
        }
        Set<T> set = new HashSet<>();
        for (Map<String, T> map : this.points.values())
        {
            set.addAll(findIn(user, owned, publics, invited, map));
        }
        return set;
    }

    public Set<T> find(Player user, String token, boolean owned, boolean publics, boolean invited)
    {
        if (!owned && !publics && !invited)
        {
            owned = true;
            publics = true;
            invited = true;
        }
        Set<T> set = new LinkedHashSet<>();
        for (String name : findIn(token, this.points.keySet()))
        {
            set.addAll(findIn(user, owned, publics, invited, this.points.get(name)));
        }
        return set;
    }

    private Set<T> findIn(Player user, boolean owned, boolean publics, boolean invited, Map<String, T> map)
    {
        Set<T> set = new LinkedHashSet<>();
        for (T value : map.values())
        {
            if (owned && value.isOwnedBy(user))
            {
                set.add(value);
            }
            if (publics && value.isPublic() && !value.isOwnedBy(user))
            {
                set.add(value);
            }
            if (invited && value.isInvited(user))
            {
                set.add(value);
            }
        }
        return set;
    }

    public void massDelete(boolean privates, boolean publics)
    {
        for (T point : this.list(privates, publics))
        {
            this.delete(point);
        }
    }

    public void massDelete(Player user, boolean privates, boolean publics)
    {
        if (user == null)
        {
            this.massDelete(privates, publics);
            return;
        }
        if (!privates && !publics)
        {
            privates = true;
            publics = true;
        }
        for (T point : this.list(user, true, false, false))
        {
            if (privates && !point.isPublic())
            {
                this.delete(point);
            }
            if (publics && point.isPublic())
            {
                this.delete(point);
            }
        }
    }

    public void massDelete(Player user, boolean priv, boolean pub, Location firstPoint, Location secondPoint)
    {
        Set<T> points = (user == null) ? this.list(priv, pub) : this.list(user, true, false, false);
        Cuboid cuboid = new Cuboid(new Vector3(firstPoint.getX(), firstPoint.getY(), firstPoint.getZ()), new Vector3(
            secondPoint.getX(), secondPoint.getY(), secondPoint.getZ()));
        for (T point : points)
        {
            Vector3d chp = point.getTransform().getPosition();
            if (point.getTransform().getExtent().equals(firstPoint.getExtent()) && cuboid.contains(new Vector3(chp.getX(), chp.getY(), chp.getZ())))
            {
                if (priv && !point.isPublic())
                {
                    this.delete(point);
                }
                if (pub && point.isPublic())
                {
                    this.delete(point);
                }
            }
        }
    }

    public boolean rename(T point, String name)
    {
        Map<String, T> map = this.points.get(name);
        if (map != null && map.containsKey(point.getOwnerName()))
        {
            return false;
        }
        this.removePoint(point);

        point.setName(name);
        point.model.updateAsync();

        this.addPoint(point);
        return true;
        // There already is a point named like that for this user!
    }

    public int getCount()
    {
        return this.list(true, true).size();
    }

    public static TreeSet<String> findIn(String pName, Set<String> strings)
    {
        TreeSet<String> result = new TreeSet<>(new StringLengthComparator());
        for (String s : strings)
        {
            if (StringUtils.startsWithIgnoreCase(s, pName))
            {
                result.add(s);
            }
        }
        return result;
    }

    private static class StringLengthComparator implements Comparator<String>
    {
        @Override
        public int compare(String o1, String o2)
        {
            int dif = o1.length() - o2.length();
            if (dif == 0)
            {
                dif = 1;
            }
            return dif;
        }
    }
}
