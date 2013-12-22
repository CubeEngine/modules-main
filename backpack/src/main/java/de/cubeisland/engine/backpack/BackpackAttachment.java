package de.cubeisland.engine.backpack;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;

import de.cubeisland.engine.core.user.UserAttachment;

public class BackpackAttachment extends UserAttachment
{
    protected Map<World, Map<String, BackpackData>> backpacks = new HashMap<>();
    protected Map<World, Map<String, BackpackData>> groupedBackpacks = new HashMap<>();
    protected Map<String, BackpackData> globalBackpacks = new HashMap<>();

    public void loadGlobalBackpacks()
    {
        Backpack module = (Backpack)this.getModule();
        this.loadBackpacks(module.globalDir, globalBackpacks);
    }
    // backpack/global/<playername>/backpackname

    // backpack/<worldname>/<playername>/backpackname
    // OR backpack/grouped/<mainworldname>/<playername>/<backpackname>
    // OR backpack/single/<worldname>/...
    // without worlds module groups work as default universe would build world world_nether & world_the_end are grouped
    public void loadBackpacks(World world)
    {
        Backpack module = (Backpack)this.getModule();
        File dir = new File(module.singleDir, world.getName());
        if (dir.exists() && dir.isDirectory())
        {
            Map<String, BackpackData> map = this.backpacks.get(world);
            if (map == null)
            {
                map = new HashMap<>();
                this.backpacks.put(world, map);
            }
            this.loadBackpacks(dir, map);
        }
        dir = new File(module.groupedDir, world.getName());
        if (dir.exists())
        {
            Map<String, BackpackData> map = this.groupedBackpacks.get(world);
            if (map == null)
            {
                map = new HashMap<>();
                this.groupedBackpacks.put(world, map);
            }
            this.loadBackpacks(dir, map);
        }
    }

    private File getGlobalBackpack(String name)
    {
        Backpack module = (Backpack)this.getModule();
        return new File(module.globalDir, name);
    }

    private File getSingleBackpack(String name, String worldName)
    {
        Backpack module = (Backpack)this.getModule();
        File dir = new File(module.singleDir, worldName);
        return new File(dir, name);
    }

    private File getGroupedBackpack(String name, String worldName)
    {
        Backpack module = (Backpack)this.getModule();
        File dir = new File(module.groupedDir, worldName);
        return new File(dir, name);
    }

    protected void loadBackpacks(File dir, Map<String, BackpackData> map)
    {
        File playerDir = new File(dir, this.getHolder().getName());
        if (playerDir.exists() && playerDir.isDirectory())
        {
            for (File file : playerDir.listFiles())
            {
                if (!file.isDirectory() && file.getName().endsWith(".dat"))
                {
                    BackpackData load = this.getModule().getCore().getConfigFactory().load(BackpackData.class, file);
                    map.put(file.getName().substring(0, file.getName().lastIndexOf(".dat")), load);
                }
            }
        }
    }

    public BackpackData getBackPack(String name, World world, boolean global)
    {
        if (global)
        {
            return this.globalBackpacks.get(name);
        }
        Map<String, BackpackData> map = this.backpacks.get(world);
        BackpackData backpackData = null;
        if (map != null)
        {
             backpackData = map.get(name);
        }
        if (backpackData != null)
        {
            return backpackData;
        }
        map = this.groupedBackpacks.get(world);
        if (map != null)
        {
            backpackData = map.get(name);
        }
        return backpackData;
    }

    public BackpackData createBackpack(String name, World forWorld)
    {
        File file = this.getSingleBackpack(name, forWorld.getName());
        BackpackData result = this.getModule().getCore().getConfigFactory().create(BackpackData.class);
        result.setFile(file);
        result.save();
        return result;
    }

    public BackpackData createGroupedBackpack(String name, World forWorld)
    {
        File file = this.getGroupedBackpack(name, forWorld.getName());
        BackpackData result = this.getModule().getCore().getConfigFactory().create(BackpackData.class);
        result.setFile(file);
        result.save();
        return result;
    }

    public BackpackData createGlobalBackpack(String name)
    {
        File file = this.getGlobalBackpack(name);
        BackpackData result = this.getModule().getCore().getConfigFactory().create(BackpackData.class);
        result.setFile(file);
        result.save();
        return result;
    }
}
