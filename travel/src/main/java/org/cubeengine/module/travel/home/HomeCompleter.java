package org.cubeengine.module.travel.home;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.module.travel.config.Home;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.List;

public class HomeCompleter implements Completer
{
    private HomeManager manager;

    public HomeCompleter(HomeManager manager)
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
            for (Home home : manager.list(((Player) invocation.getCommandSource()), true, true))
            {
                if (home.name.startsWith(token))
                {
                    list.add(home.name);
                }
            }
        }
        return list;
    }
}
