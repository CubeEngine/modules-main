package de.cubeisland.cubeengine.war.user;

import de.cubeisland.cubeengine.war.CubeWar;
import de.cubeisland.cubeengine.war.CubeWarConfiguration;

public class Rank
{
    private String name;
    private int killmodifier;
    private int deathmodifier;
    private int killpointlimit;
    private double influencemod;

    public Rank(String name, int killMod, int deathMod, int kplimit, double influence)
    {
        this.name = name;
        this.deathmodifier = deathMod;
        this.killmodifier = killMod;
        this.killpointlimit = kplimit;
        this.influencemod = influence;
    }

    public Rank newRank(WarUser user)
    {
        final CubeWarConfiguration config = CubeWar.getInstance().getConfiguration();
        Integer kp = null;
        for (int i = user.getKillpoints(); kp == null; --i)
        {
            if (config.cubewar_ranks.containsKey(i))
            {
                kp = i;
            }
        }
        return config.cubewar_ranks.get(kp);
    }

    public static Rank newRank(int userkp)
    {
        final CubeWarConfiguration config = CubeWar.getInstance().getConfiguration();
        Integer kp = null;
        for (int i = userkp; kp == null; --i)
        {
            if (config.cubewar_ranks.containsKey(i))
            {
                kp = i;
            }
        }
        return config.cubewar_ranks.get(kp);
    }

    public int getKmod()
    {
        return this.killmodifier;
    }

    public int getDmod()
    {
        return this.deathmodifier;
    }

    public double getImod()
    {
        return this.influencemod;
    }

    public String getName()
    {
        return this.name;
    }
}