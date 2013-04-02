package de.cubeisland.cubeengine.core.ban;

import java.util.Date;

public abstract class Ban
{
    private String source;
    private String reason;
    private Date created;
    private Date expires;

    protected Ban(String source, String reason, Date expires)
    {
        this(source, reason, new Date(System.currentTimeMillis()), expires);
    }

    protected Ban(String source, String reason, Date created, Date expires)
    {
        assert source != null: "The source must not be null";
        assert reason != null: "The reason must not be null";
        assert created != null: "The created must not be null";
        this.source = source;
        this.reason = reason;
        this.created = created;
        this.expires = expires;
    }

    /**
     * This method returns the source of this ban.
     * The source ban is usually a user or the console.
     *
     * @return the source as a string representation. never null!
     */
    public String getSource()
    {
        return source;
    }

    /**
     * This method sets the source of this command
     *
     * @param source the source as a string representation. may not be null!
     */
    public void setSource(String source)
    {
        assert source != null: "The source must not be null";
        this.source = source;
    }

    /**
     * This method returns the ban reason
     *
     * @return the ban reason. never null!
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * This method sets the ban reason.
     *
     * @param reason the ban reason. may not be null!
     */
    public void setReason(String reason)
    {
        assert reason != null: "The reason must not be null";
        this.reason = reason;
    }

    /**
     * This method returns the creation date of this ban
     *
     * @return the date of creation. never null!
     */
    public Date getCreated()
    {
        return created;
    }

    /**
     * This method set the date of creation.
     *
     * @param created the date. may not be null!
     */
    public void setCreated(Date created)
    {
        assert created != null: "The created must not be null";
        this.created = created;
    }

    /**
     * This method returns the expire date.
     * A null value represents no expire date (known as permanent bans)
     *
     * @return the expire date or null
     */
    public Date getExpires()
    {
        return expires;
    }

    /**
     * This method sets the expire date.
     * A null value represents no expire date (known as permanent bans)
     *
     * @param expires the expire date or null
     */
    public void setExpires(Date expires)
    {
        this.expires = expires;
    }

    /**
     * This method checks whether this ban is expired.
     *
     * @return true if the the ban is expired
     */
    public boolean isExpired()
    {
        Date expires = this.getExpires();
        if (expires != null)
        {
            return expires.getTime() <= System.currentTimeMillis();
        }
        return false;
    }

    /**
     * This method returns the string representation of the ban target
     *
     * @return the ban target as a string
     */
    @Override
    public String toString()
    {
        throw new UnsupportedOperationException();
    }
}
