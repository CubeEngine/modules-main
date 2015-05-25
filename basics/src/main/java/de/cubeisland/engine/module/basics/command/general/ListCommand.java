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
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.result.CommandResult;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.GRAY;
import static org.spongepowered.api.text.format.TextColors.WHITE;

public class ListCommand
{
    protected static final Comparator<User> USER_COMPARATOR = new UserComparator();
    private final Basics module;
    private UserManager um;
    private Game game;

    public ListCommand(Basics module, UserManager um, Game game)
    {
        this.module = module;
        this.um = um;
        this.game = game;
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

        for (User user : um.getOnlineUsers())
        {
            if (context instanceof User && !((User)context).canSee(user.getPlayer().get()))
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
                             game.getServer().getMaxPlayers());

        for (Entry<String, Set<User>> entry : grouped.entrySet())
        {
            Iterator<User> it = entry.getValue().iterator();
            if (!it.hasNext())
            {
                continue;
            }
            TextBuilder builder = Texts.of(entry.getKey(), WHITE, ":").builder();
            builder.append(formatUser(it.next()));
            while (it.hasNext())
            {
                builder.append(Texts.of(WHITE, ", "), formatUser(it.next()));
            }
            context.sendMessage(builder.build());
        }

        return null;
    }

    private Text formatUser(User user)
    {
        Text result = Texts.of(DARK_GREEN, user.getDisplayName());
        if (user.attachOrGet(BasicsAttachment.class, module).isAfk())
        {
            result = result.builder().append(Texts.of(WHITE, "(", GRAY, "afk", ")")).build();
        }
        return result;
    }

    private static final class UserComparator implements Comparator<User>
    {
        @Override
        public int compare(User user1, User user2)
        {
            return String.CASE_INSENSITIVE_ORDER.compare(Texts.toPlain(user1.getDisplayName()), Texts.toPlain(user2.getDisplayName()));
        }
    }
}
