package de.cubeisland.cubeengine.conomy.account.user;

import de.cubeisland.cubeengine.conomy.account.AccountModel;
import de.cubeisland.cubeengine.core.storage.database.AttrType;
import de.cubeisland.cubeengine.core.storage.database.Attribute;
import de.cubeisland.cubeengine.core.storage.database.Entity;
import de.cubeisland.cubeengine.core.storage.database.Key;
import de.cubeisland.cubeengine.core.user.User;

/**
 *
 * @author Anselm Brehme
 */
@Entity(name = "useraccount")
public class UserAccount extends AccountModel
{
    @Key
    @Attribute(type= AttrType.INT)
    private final User user;
    
    public UserAccount(User user, double start)
    {
        this.user = user;
        this.set(start);
    }
    
    public UserAccount(User user)
    {
        this.user = user;
        this.reset();
    }

    public User getUser()
    {
        return this.user;
    }

    @Override
    public String getName()
    {
        return this.user.getName();
    }

    @Override
    public Integer getKey()
    {
        return this.user.getId();
    }

    @Override
    public void setKey(Integer id)
    {
        throw new UnsupportedOperationException("UserID cannot be changed here!.");
    }
}
