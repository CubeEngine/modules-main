package de.cubeisland.engine.module.vanillaplus;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.Disable;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.core.filesystem.FileManager;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import de.cubeisland.engine.module.service.user.UserManager;
import de.cubeisland.engine.module.vanillaplus.spawnmob.SpawnMobCommand;
import org.spongepowered.api.Game;

public class VanillaPlus extends Module
{
    @Inject private CommandManager cm;
    @Inject private UserManager um;
    @Inject private Game game;
    @Inject private FileManager fm;
    @Inject private PermissionManager pm;
    private VanillaPlusConfig config;

    @Enable
    public void onEnable()
    {
        config = fm.loadConfig(this, VanillaPlusConfig.class);
        cm.addCommands(this, new ListCommand(this, um, game));
        cm.addCommands(this, new VanillaCommands(this, game, pm));
        cm.addCommands(this, new SpawnMobCommand(this));
    }

    @Disable
    public void onDisable()
    {
        cm.removeCommands(this);
    }

    public VanillaPlusConfig getConfig()
    {
        return config;
    }
}
