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
package org.cubeengine.module.vanillaplus.improvement.removal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.exception.SilentException;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.DefaultValue;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.command.exception.PermissionDeniedException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Ambient;
import org.spongepowered.api.entity.living.Hostile;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.Squid;
import org.spongepowered.api.entity.living.Villager;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.golem.Golem;
import org.spongepowered.api.entity.living.monster.Boss;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.text.format.TextFormat.NONE;

public class LivingFilterReader extends PermissionContainer<VanillaPlus> implements ArgumentReader<LivingFilter>, DefaultValue<LivingFilter>
{
    private I18n i18n;
    private StringMatcher sm;

    public LivingFilterReader(VanillaPlus module, I18n i18n, StringMatcher sm)
    {
        super(module);
        this.i18n = i18n;
        this.sm = sm;
    }

    private final PermissionDescription BASEPERM_FLAG = register("command.butcher.flag", "", null);
    public final PermissionDescription PERM_HOSTILE = register("hostile", "", BASEPERM_FLAG);
    public final PermissionDescription PERM_MONSTER = register("monster", "", BASEPERM_FLAG);
    public final PermissionDescription PERM_BOSS = register("boss", "", BASEPERM_FLAG);
    public final PermissionDescription PERM_ANIMAL = register("animal", "", BASEPERM_FLAG);
    public final PermissionDescription PERM_NPC = register("npc", "", BASEPERM_FLAG);
    public final PermissionDescription PERM_PET = register("pet", "", BASEPERM_FLAG);
    public final PermissionDescription PERM_GOLEM = register("golem", "", BASEPERM_FLAG);
    public final PermissionDescription PERM_AMBIENT = register("ambient", "", BASEPERM_FLAG);

    public final PermissionDescription PERM_ALLTYPE = register("command.butcher.alltypes", "", null);

    private final Predicate<Entity> FILTER_HOSTILE = entity -> entity instanceof Hostile;
    private final Predicate<Entity> FILTER_MONSTER = entity -> entity instanceof Hostile && !(entity instanceof Boss);
    private final Predicate<Entity> FILTER_BOSS = entity -> entity instanceof Boss;
    private final Predicate<Entity> FILTER_ANIMAL = entity -> (entity instanceof Animal && !entity.get(Keys.TAMED_OWNER).isPresent()) || entity instanceof Squid;
    private final Predicate<Entity> FILTER_NPC = entity -> entity instanceof Villager;
    private final Predicate<Entity> FILTER_PET = entity -> entity instanceof Animal && entity.get(Keys.TAMED_OWNER).isPresent();
    private final Predicate<Entity> FILTER_GOLEM = entity -> entity instanceof Golem;
    private final Predicate<Entity> FILTER_AMBIENT = entity -> entity instanceof Ambient;

    private Map<Predicate<Entity>, PermissionDescription> predicatePerms = new HashMap<>();

    {
        predicatePerms.put(FILTER_HOSTILE, PERM_HOSTILE);
        predicatePerms.put(FILTER_MONSTER, PERM_MONSTER);
        predicatePerms.put(FILTER_BOSS, PERM_BOSS);
        predicatePerms.put(FILTER_ANIMAL, PERM_ANIMAL);
        predicatePerms.put(FILTER_NPC, PERM_NPC);
        predicatePerms.put(FILTER_PET, PERM_PET);
        predicatePerms.put(FILTER_GOLEM, PERM_GOLEM);
        predicatePerms.put(FILTER_AMBIENT, PERM_AMBIENT);
    }

    private Map<EntityType, PermissionDescription> typePerms = new HashMap<>();

    {
        for (EntityType type : Sponge.getRegistry().getAllOf(EntityType.class))
        {
            Class<? extends Entity> eClass = type.getEntityClass();
            if (Living.class.isAssignableFrom(eClass))
            {
                if (Hostile.class.isAssignableFrom(eClass))
                {
                    if (Boss.class.isAssignableFrom(eClass))
                    {
                        typePerms.put(type, PERM_BOSS);
                    }
                    else
                    {
                        typePerms.put(type, PERM_MONSTER);
                    }
                }
                else if (Animal.class.isAssignableFrom(eClass) || Squid.class.isAssignableFrom(eClass))
                {
                    typePerms.put(type, PERM_ANIMAL);
                }
                else if (Golem.class.isAssignableFrom(eClass))
                {
                    typePerms.put(type, PERM_GOLEM);
                }
                else if (Villager.class.isAssignableFrom(eClass))
                {
                    typePerms.put(type, PERM_NPC);
                }
                else if (Ambient.class.isAssignableFrom(eClass))
                {
                    typePerms.put(type, PERM_AMBIENT);
                }
                else
                {
                    typePerms.put(type, PERM_ALLTYPE);
                }
            }
        }
    }

    @Override
    public LivingFilter read(Class aClass, CommandInvocation invocation) throws ReaderException
    {
        CommandSource source = (CommandSource)invocation.getCommandSource();
        String token = invocation.consume(1);
        if ("*".equals(token))
        {
            if (!source.hasPermission(PERM_ALLTYPE.getId()))
            {
                throw new PermissionDeniedException(PERM_ALLTYPE);
            }
            return new LivingFilter(emptyList());
        }

        List<Predicate<Entity>> list = new ArrayList<>();

        Map<String, Predicate<Entity>> groupMap = new HashMap<>();
        groupMap.put(i18n.getTranslation(source, NONE, "hostile").toPlain(), FILTER_HOSTILE);
        groupMap.put(i18n.getTranslation(source, NONE, "monster").toPlain(), FILTER_MONSTER);
        groupMap.put(i18n.getTranslation(source, NONE, "boss").toPlain(), FILTER_BOSS);
        groupMap.put(i18n.getTranslation(source, NONE, "animal").toPlain(), FILTER_ANIMAL);
        groupMap.put(i18n.getTranslation(source, NONE, "npc").toPlain(), FILTER_NPC);
        groupMap.put(i18n.getTranslation(source, NONE, "pet").toPlain(), FILTER_PET);
        groupMap.put(i18n.getTranslation(source, NONE, "golem").toPlain(), FILTER_GOLEM);
        groupMap.put(i18n.getTranslation(source, NONE, "ambient").toPlain(), FILTER_AMBIENT);

        Map<String, EntityType> map = Sponge.getRegistry().getAllOf(EntityType.class).stream().filter(
            type -> Living.class.isAssignableFrom(type.getEntityClass())).collect(
            toMap(t -> t.getTranslation().get(source.getLocale()), identity()));

        for (String part : StringUtils.explode(",", token))
        {
            String match = sm.matchString(part, groupMap.keySet());
            if (match == null)
            {
                match = sm.matchString(part, map.keySet());
                if (match == null)
                {
                    i18n.sendTranslated(source, NEGATIVE, "Could not find a living entity named {input}", part);
                    i18n.sendTranslated(source, NEUTRAL, "The following are valid entity groups:");
                    source.sendMessage(Text.of(StringUtils.implode(", ", groupMap.keySet())));
                    i18n.sendTranslated(source, NEUTRAL, "The following are valid entity types:");
                    source.sendMessage(Text.of(StringUtils.implode(", ", map.keySet())));
                    throw new SilentException();
                }
                EntityType type = map.get(match);
                PermissionDescription perm = typePerms.get(type);
                if (!source.hasPermission(perm.getId()))
                {
                    throw new PermissionDeniedException(perm);
                }

                list.add(entity -> entity.getType().equals(type) &&
                    entity.get(Keys.TAMED_OWNER).isPresent() &&
                    source.hasPermission(PERM_PET.getId()));
            }
            else
            {
                Predicate<Entity> predicate = groupMap.get(match);
                PermissionDescription perm = predicatePerms.get(predicate);
                if (!source.hasPermission(perm.getId()))
                {
                    throw new PermissionDeniedException(perm);
                }
                list.add(predicate);
            }
        }

        return new LivingFilter(list);
    }


    @Override
    public LivingFilter getDefault(CommandInvocation invocation)
    {
        CommandSource source = (CommandSource)invocation.getCommandSource();
        if (source.hasPermission(PERM_MONSTER.getId()))
        {
            return new LivingFilter(singletonList(FILTER_MONSTER));
        }
        throw new PermissionDeniedException(PERM_MONSTER);
    }
}
