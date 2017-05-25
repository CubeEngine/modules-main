/*
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
package org.cubeengine.module.vanillaplus.improvement.removal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.exception.SilentException;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.ReaderException;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.text.Text;

import static java.util.Arrays.asList;
import static org.cubeengine.libcube.util.ChatFormat.YELLOW;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.entity.EntityTypes.*;

public class EntityFilterParser implements ArgumentParser<EntityFilter>
{
    private I18n i18n;
    private EntityMatcher em;
    private MaterialMatcher mm;

    public EntityFilterParser(I18n i18n, EntityMatcher em, MaterialMatcher mm)
    {
        this.i18n = i18n;
        this.em = em;
        this.mm = mm;
    }

    @Override
    public EntityFilter parse(Class aClass, CommandInvocation invocation) throws ReaderException
    {
        CommandSource cmdSource = (CommandSource)invocation.getCommandSource();
        String token = invocation.consume(1);
        List<Predicate<Entity>> filters = new ArrayList<>();
        for (String entityString : StringUtils.explode(",", token))
        {
            EntityType type;
            ItemType itemType = null;
            if (entityString.contains(":"))
            {
                type = em.any(entityString.substring(0, entityString.indexOf(":")), invocation.getContext(Locale.class));
                if (!ITEM.equals(type))
                {
                    i18n.sendTranslated(cmdSource, NEGATIVE, "You can only specify data for removing items!");
                    throw new SilentException();
                }
                String itemString = entityString.substring(entityString.indexOf(":") + 1);
                itemType = mm.material(itemString);
                if (itemType == null)
                {
                    i18n.sendTranslated(cmdSource, NEGATIVE, "Cannot find itemtype {input}", itemString);
                    throw new SilentException();
                }
            }
            else
            {
                type = em.any(entityString, invocation.getContext(Locale.class));
            }
            if (type == null)
            {
                i18n.sendTranslated(cmdSource, NEGATIVE, "Invalid entity-type!");
                i18n.sendTranslated(cmdSource, NEUTRAL, "Try using one of those instead:");

                cmdSource.sendMessage(Text.joinWith(Text.of(YELLOW, ", "), asList(ITEM, /*,TODO ARROW */ RIDEABLE_MINECART,
                                                                                  PAINTING, ITEM_FRAME, EXPERIENCE_ORB).stream().map(
                    EntityType::getTranslation).map(t -> Text.of(t)).iterator()));
                throw new SilentException();
            }
            if (Living.class.isAssignableFrom(type.getEntityClass()))
            {
                i18n.sendTranslated(cmdSource, NEGATIVE, "To kill living entities use the {text:/butcher} command!");
                throw new SilentException();
            }
            final ItemType item = itemType;
            filters.add(entity -> entity.getType().equals(type) && (item == null || entity.get(Keys.REPRESENTED_ITEM).get().getType().equals(item)));
        }
        return new EntityFilter(filters);
    }
}
