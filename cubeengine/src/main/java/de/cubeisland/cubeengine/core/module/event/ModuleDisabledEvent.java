package de.cubeisland.cubeengine.core.module.event;

import de.cubeisland.cubeengine.core.Core;
import de.cubeisland.cubeengine.core.module.Module;
import org.bukkit.event.HandlerList;

/**
 * This event is fired when a module got disabled.
 */
public class ModuleDisabledEvent extends ModuleEvent
{
    private static final HandlerList handlers = new HandlerList();

    public ModuleDisabledEvent(Core core, Module module)
    {
        super(core, module);
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}