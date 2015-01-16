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
package de.cubeisland.engine.module.basics.command.general;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.result.CommandResult;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import org.bukkit.Bukkit;

import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;

public class ListCommand
{
    protected static final Comparator<User> USER_COMPARATOR = new UserComparator();
    private final Basics module;

    public ListCommand(Basics module)
    {
        this.module = module;
    }

    protected SortedMap<String, Set<User>> groupUsers(Set<User> users)
    {
        SortedMap<String, Set<User>> grouped = new TreeMap<>();
        grouped.put(ChatFormat.GOLD + "Players", users);

        return grouped;
    }

    @Command(desc = "Displays all the online players.")
    public CommandResult list(CommandSender context)
    {
        final SortedSet<User> users = new TreeSet<>(USER_COMPARATOR);

        for (User user : module.getCore().getUserManager().getOnlineUsers())
        {
            if (context instanceof User && !((User)context).canSee(user))
            {
                continue;
            }
            users.add(user);
        }

        if (users.isEmpty())
        {
            context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
            return null;
        }

        SortedMap<String, Set<User>> grouped = this.groupUsers(users);
        context.sendTranslated(POSITIVE, "Players online: {amount#online}/{amount#max}", users.size(),
                              Bukkit.getMaxPlayers());

        for (Entry<String, Set<User>> entry : grouped.entrySet())
        {
            Iterator<User> it = entry.getValue().iterator();
            if (!it.hasNext())
            {
                continue;
            }
            StringBuilder group = new StringBuilder(entry.getKey()).append(ChatFormat.WHITE).append(": ");
            group.append(this.formatUser(it.next()));

            while (it.hasNext())
            {
                group.append(ChatFormat.WHITE).append(", ").append(this.formatUser(it.next()));
            }
            context.sendMessage(group.toString());
        }

        return null;
    }

    private String formatUser(User user)
    {
        String entry = ChatFormat.DARK_GREEN + user.getDisplayName();
        if (user.attachOrGet(BasicsAttachment.class, module).isAfk())
        {
            entry += ChatFormat.WHITE + "(" + ChatFormat.GREY + "afk" + ChatFormat.WHITE + ")";
        }
        return entry;
    }

    private static final class UserComparator implements Comparator<User>
    {
        @Override
        public int compare(User user1, User user2)
        {
            return String.CASE_INSENSITIVE_ORDER.compare(user1.getDisplayName(), user2.getDisplayName());
        }
    }
}
