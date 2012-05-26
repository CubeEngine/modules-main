package de.cubeisland.cubeengine.conomy.account.user;

import de.cubeisland.cubeengine.conomy.account.AccountModel;
import de.cubeisland.cubeengine.core.user.User;

/**
 *
 * @author Faithcaio
 */
public class UserAccount extends AccountModel
{
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
    public int getId()
    {
        return this.user.getId();
    }

    @Override
    public void setId(int id)
    {
        throw new UnsupportedOperationException("UserID cannot be changed here!.");
    }
}
