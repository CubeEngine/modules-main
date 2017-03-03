package org.cubeengine.module.protector;

import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.reflect.Reflector;

import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;

@ModuleInfo(name = "Protector", description = "Protects your worlds")
public class Protector extends Module
{
    @Inject private Path modulePath;
    @Inject private Reflector reflector;

    private RegionManager manager;

    @Enable
    public void onEnable() throws IOException
    {
        manager = new RegionManager(modulePath, reflector);
    }
}
