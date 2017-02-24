package org.cubeengine.module.travel.warp;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.module.travel.config.Home;
import org.cubeengine.module.travel.config.Warp;
import org.cubeengine.module.travel.home.HomeManager;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.List;

public class WarpCompleter implements Completer
{
    private WarpManager manager;

    public WarpCompleter(WarpManager manager)
    {
        this.manager = manager;
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        List<String> list = new ArrayList<>();
        if (invocation.getCommandSource() instanceof Player)
        {
            String token = invocation.currentToken();
            for (Warp warp : manager.list(((Player) invocation.getCommandSource())))
            {
                if (warp.name.startsWith(token))
                {
                    list.add(warp.name);
                }
            }
        }
        return list;
    }
}
