package de.cubeisland.engine.module.netherportals;

import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.netherportals.NetherportalsConfig.WorldSection;
import org.cubeengine.service.filesystem.ModuleConfig;
import org.cubeengine.service.world.ConfigWorld;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.teleport.TeleportCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportType;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.World;

@ModuleInfo(name = "Netherportals", description = "Modifies Vanilla Portal behaviours")
public class Netherportals extends Module
{
    @ModuleConfig private NetherportalsConfig config;

    @Listener
    public void onPortal(DisplaceEntityEvent.Teleport event, @First TeleportCause cause)
    {
        WorldSection section = config.worldSettings.get(new ConfigWorld(event.getFromTransform().getExtent()));
        if (section != null && section.enablePortalRouting)
        {
            Transform<World> to = event.getToTransform();
            TeleportType type = cause.getTeleportType();
            if (type == TeleportTypes.NETHER_PORTAL)
            {
                if (section.netherTarget != null)
                {
                    to = to.setExtent(section.netherTarget.getWorld());
                    // TODO netherPortalScale

                    event.setToTransform(to);
                    // TODO PortalCreation?
                }
            }
            else if (type == TeleportTypes.END_PORTAL)
            {
                if (section.endTarget != null)
                {
                    to = to.setExtent(section.endTarget.getWorld());
                    // TODO endPortalTargetLocation?

                    event.setToTransform(to);
                    // TODO cancel PortalCreation if not in end?
                }
            }
        }
    }
}