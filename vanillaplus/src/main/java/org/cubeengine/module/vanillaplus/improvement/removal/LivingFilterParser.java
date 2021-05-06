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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.DefaultParameterProvider;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Ambient;
import org.spongepowered.api.entity.living.Hostile;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.aquatic.Squid;
import org.spongepowered.api.entity.living.golem.Golem;
import org.spongepowered.api.entity.living.monster.boss.Boss;
import org.spongepowered.api.entity.living.trader.Villager;
import org.spongepowered.api.registry.RegistryTypes;

import static java.util.Collections.emptyList;
import static java.util.Collections.rotate;
import static java.util.Collections.singletonList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;

public class LivingFilterParser extends PermissionContainer implements ValueParser<LivingFilter>, DefaultParameterProvider<LivingFilter>, ValueCompleter
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
    private final Predicate<Entity> FILTER_ANIMAL = entity -> (entity instanceof Animal && !entity.get(Keys.TAMER).isPresent()) || entity instanceof Squid;
    private final Predicate<Entity> FILTER_NPC = entity -> entity instanceof Villager;
    private final Predicate<Entity> FILTER_PET = entity -> entity instanceof Animal && entity.get(Keys.TAMER).isPresent();
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



    @Override
    public LivingFilter apply(CommandCause commandCause)
    {
        if (PERM_MONSTER.check(commandCause))
        {
            return new LivingFilter(singletonList(FILTER_MONSTER), commandCause.subject(), this);
        }
        return null;
        // TODO errormessage        throw new PermissionDeniedException(PERM_MONSTER);
    }

    @Override
    public List<CommandCompletion> complete(CommandContext context, String currentInput)
    {
        List<String> types = Arrays.asList(
            i18n.getTranslation(context.cause(), "hostile"),
            i18n.getTranslation(context.cause(), "monster"),
            i18n.getTranslation(context.cause(), "boss"),
            i18n.getTranslation(context.cause(), "animal"),
            i18n.getTranslation(context.cause(), "npc"),
            i18n.getTranslation(context.cause(), "pet"),
            i18n.getTranslation(context.cause(), "golem"),
            i18n.getTranslation(context.cause(), "ambient"));
        final Set<String> closeMatches = new HashSet<>(sm.getBestMatches(currentInput, types, 2));
        for (String type : types)
        {
            if (type.startsWith(currentInput))
            {
                closeMatches.add(type);
            }
        }
        return closeMatches.stream().map(CommandCompletion::of).collect(Collectors.toList());
    }

    @Override
    public Optional<? extends LivingFilter> parseValue(Key<? super LivingFilter> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        final CommandCause source = context.cause();
        final String token = reader.parseString();
        if ("*".equals(token))
        {
            if (!source.hasPermission(PERM_ALLTYPE.getId()))
            {
                throw reader.createException(Component.text("Missing permission"));
            }
            return Optional.of(new LivingFilter(emptyList(), context.subject(), this));
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

        Map<String, EntityType<?>> map = new HashMap<>();
        Sponge.game().registries().registry(RegistryTypes.ENTITY_TYPE).streamEntries().forEach(entry -> {
            map.put(entry.key().asString(), entry.value());
            if ("minecraft".equals(entry.key().namespace()))
            {
                map.put(entry.key().value(), entry.value());
            }
        });

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
                    List<Component> groups = groupMap.keySet().stream().map(s -> Component.text(s, NamedTextColor.GRAY)).collect(Collectors.toList());
                    final TextComponent delimiter = Component.text(", ", NamedTextColor.WHITE);
                    source.sendMessage(Identity.nil(), Component.join(delimiter, groups));
                    i18n.send(source, NEUTRAL, "The following are valid entity types:");
                    List<Component> types = map.keySet().stream().map(s -> Component.text(s, NamedTextColor.GRAY)).collect(Collectors.toList());
                    source.sendMessage(Identity.nil(), Component.join(delimiter, types));
                    return Optional.empty();
                }
                EntityType type = map.get(match);

                list.add(entity -> entity.type().equals(type) && (!entity.get(Keys.TAMER).isPresent() || source.hasPermission(PERM_PET.getId())));
            }
            else
            {
                Predicate<Entity> predicate = groupMap.get(match);
                Permission perm = predicatePerms.get(predicate);
                if (!source.hasPermission(perm.getId()))
                {
                    throw reader.createException(Component.text("Missing permission"));
                }
                list.add(predicate);
            }
        }

        return Optional.of(new LivingFilter(list, context.subject(), this));
    }


}
