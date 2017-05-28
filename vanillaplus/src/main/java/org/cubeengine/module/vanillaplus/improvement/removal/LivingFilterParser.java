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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.exception.SilentException;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.DefaultValue;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.text.format.TextColors.WHITE;

public class LivingFilterParser extends PermissionContainer implements ArgumentParser<LivingFilter>, DefaultValue<LivingFilter>
{
    private I18n i18n;
    private StringMatcher sm;

    public LivingFilterParser(PermissionManager pm, I18n i18n, StringMatcher sm)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
        this.sm = sm;
    }

    private final Permission BASEPERM_FLAG = register("command.butcher.flag", "", null);
    public final Permission PERM_HOSTILE = register("hostile", "", BASEPERM_FLAG);
    public final Permission PERM_MONSTER = register("monster", "", BASEPERM_FLAG);
    public final Permission PERM_BOSS = register("boss", "", BASEPERM_FLAG);
    public final Permission PERM_ANIMAL = register("animal", "", BASEPERM_FLAG);
    public final Permission PERM_NPC = register("npc", "", BASEPERM_FLAG);
    public final Permission PERM_PET = register("pet", "", BASEPERM_FLAG);
    public final Permission PERM_GOLEM = register("golem", "", BASEPERM_FLAG);
    public final Permission PERM_AMBIENT = register("ambient", "", BASEPERM_FLAG);

    public final Permission PERM_ALLTYPE = register("command.butcher.alltypes", "", null);

    private final Predicate<Entity> FILTER_HOSTILE = entity -> entity instanceof Hostile;
    private final Predicate<Entity> FILTER_MONSTER = entity -> entity instanceof Hostile && !(entity instanceof Boss);
    private final Predicate<Entity> FILTER_BOSS = entity -> entity instanceof Boss;
    private final Predicate<Entity> FILTER_ANIMAL = entity -> (entity instanceof Animal && !entity.get(Keys.TAMED_OWNER).isPresent()) || entity instanceof Squid;
    private final Predicate<Entity> FILTER_NPC = entity -> entity instanceof Villager;
    private final Predicate<Entity> FILTER_PET = entity -> entity instanceof Animal && entity.get(Keys.TAMED_OWNER).isPresent();
    private final Predicate<Entity> FILTER_GOLEM = entity -> entity instanceof Golem;
    private final Predicate<Entity> FILTER_AMBIENT = entity -> entity instanceof Ambient;

    private Map<Predicate<Entity>, Permission> predicatePerms = new HashMap<>();

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

    private Map<EntityType, Permission> typePerms = new HashMap<>();

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
    public LivingFilter parse(Class aClass, CommandInvocation invocation) throws ParserException
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
        groupMap.put(i18n.getTranslation(source, "hostile"), FILTER_HOSTILE);
        groupMap.put(i18n.getTranslation(source, "monster"), FILTER_MONSTER);
        groupMap.put(i18n.getTranslation(source, "boss"), FILTER_BOSS);
        groupMap.put(i18n.getTranslation(source, "animal"), FILTER_ANIMAL);
        groupMap.put(i18n.getTranslation(source, "npc"), FILTER_NPC);
        groupMap.put(i18n.getTranslation(source, "pet"), FILTER_PET);
        groupMap.put(i18n.getTranslation(source, "golem"), FILTER_GOLEM);
        groupMap.put(i18n.getTranslation(source, "ambient"), FILTER_AMBIENT);

        Map<String, EntityType> map = Sponge.getRegistry().getAllOf(EntityType.class).stream().filter(
            type -> Living.class.isAssignableFrom(type.getEntityClass())).distinct().collect(
            toMap(t -> t.getTranslation().get(source.getLocale()), identity()));

        for (String part : StringUtils.explode(",", token))
        {
            String match = sm.matchString(part, groupMap.keySet());
            if (match == null)
            {
                match = sm.matchString(part, map.keySet());
                if (match == null)
                {
                    i18n.send(source, NEGATIVE, "Could not find a living entity named {input}", part);
                    i18n.send(source, NEUTRAL, "The following are valid entity groups:");
                    List<Text> groups = groupMap.keySet().stream().map(s -> Text.of(TextColors.GRAY, s)).collect(Collectors.toList());
                    source.sendMessage(Text.joinWith(Text.of(WHITE, ", "), groups));
                    i18n.send(source, NEUTRAL, "The following are valid entity types:");
                    List<Text> types = map.keySet().stream().map(s -> Text.of(TextColors.GRAY, s)).collect(Collectors.toList());
                    source.sendMessage(Text.joinWith(Text.of(WHITE, ", "),types));
                    throw new SilentException();
                }
                EntityType type = map.get(match);
                Permission perm = typePerms.get(type);
                if (!source.hasPermission(perm.getId()))
                {
                    throw new PermissionDeniedException(perm);
                }

                list.add(entity -> entity.getType().equals(type) &&
                        (!entity.get(Keys.TAMED_OWNER).isPresent() ||
                    source.hasPermission(PERM_PET.getId())));
            }
            else
            {
                Predicate<Entity> predicate = groupMap.get(match);
                Permission perm = predicatePerms.get(predicate);
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
    public LivingFilter provide(CommandInvocation invocation)
    {
        CommandSource source = (CommandSource)invocation.getCommandSource();
        if (source.hasPermission(PERM_MONSTER.getId()))
        {
            return new LivingFilter(singletonList(FILTER_MONSTER));
        }
        throw new PermissionDeniedException(PERM_MONSTER);
    }
}
