package org.cubeengine.module.conomy;

import javax.inject.Inject;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.asm.marker.ServiceProvider;
import org.spongepowered.api.service.economy.EconomyService;

@ServiceProvider(EconomyService.class)
public class EconomyServiceProvider implements Provider<EconomyService>
{
    @Inject private Conomy module;

    @Override
    public EconomyService get()
    {
        return module.getService();
    }
}
