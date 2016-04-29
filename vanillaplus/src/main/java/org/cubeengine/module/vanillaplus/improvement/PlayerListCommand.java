/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.vanillaplus.improvement;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.result.CommandResult;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.WHITE;

public class PlayerListCommand
{
    protected static final Comparator<Player> USER_COMPARATOR = new UserComparator();
    private I18n i18n;

    public PlayerListCommand(I18n i18n)
    {
        this.i18n = i18n;
    }

    protected SortedMap<String, Set<Player>> groupUsers(Set<Player> users)
    {
        SortedMap<String, Set<Player>> grouped = new TreeMap<>();
        for (Player player : users)
        {
            Subject subject = Sponge.getServiceManager().provideUnchecked(PermissionService.class).getUserSubjects().get(player.getUniqueId().toString());
            String listGroup = "&6Players";
            if (subject instanceof OptionSubject)
            {
                listGroup = ((OptionSubject)subject).getOption("list-group").orElse(listGroup);
            }
            Set<Player> assigned = grouped.get(listGroup);
            if (assigned == null)
            {
                assigned = new LinkedHashSet<>();
                grouped.put(listGroup, assigned);
            }
            assigned.add(player);
        }
        return grouped;
    }

    @Command(desc = "Displays all the online players.")
    public CommandResult list(CommandSource context)
    {
        final SortedSet<Player> users = new TreeSet<>(USER_COMPARATOR);

        for (Player user : Sponge.getServer().getOnlinePlayers())
        {
            // TODO can see
            /*if (context instanceof Player && !((Player)context).canSee(user))
            {
                continue;
            }
            */
            users.add(user);
        }

        if (users.isEmpty())
        {
            i18n.sendTranslated(context, NEGATIVE, "There are no players online at the moment!");
            return null;
        }

        SortedMap<String, Set<Player>> grouped = this.groupUsers(users);
        i18n.sendTranslated(context, POSITIVE, "Players online: {amount#online}/{amount#max}", users.size(), Sponge.getServer().getMaxPlayers());

        for (Entry<String, Set<Player>> entry : grouped.entrySet())
        {
            Iterator<Player> it = entry.getValue().iterator();
            if (!it.hasNext())
            {
                continue;
            }
            Text.Builder builder = Text.of(ChatFormat.fromLegacy(entry.getKey(), '&'), WHITE, ": ").toBuilder();
            builder.append(formatUser(it.next()));
            while (it.hasNext())
            {
                builder.append(Text.of(WHITE, ", "), formatUser(it.next()));
            }
            context.sendMessage(builder.build());
        }

        return null;
    }

    private Text formatUser(Player user)
    {
        Text result = Text.of(DARK_GREEN, user.getName());
        // TODO chat module pass info that player is afk
        /*
        if (user.attachOrGet(BasicsAttachment.class, module).isAfk())
        {
            result = result.builder().append(Texts.of(WHITE, "(", GRAY, "afk", ")")).build();
        }
        */
        return result;
    }

    private static final class UserComparator implements Comparator<Player>
    {
        @Override
        public int compare(Player user1, Player user2)
        {
            return String.CASE_INSENSITIVE_ORDER.compare(user1.getName(), user2.getName());
        }
    }
}
