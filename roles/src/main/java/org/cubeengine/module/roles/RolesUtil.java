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
package org.cubeengine.module.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.text.Text;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.spongepowered.api.service.permission.PermissionService.SUBJECTS_ROLE_TEMPLATE;
import static org.spongepowered.api.text.action.TextActions.showText;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

public class RolesUtil
{
    public static final Context GLOBAL = new Context("global", "");
    public static final String PERMISSION_TEMPLATE_PREFIX = "permission:";

    public static FoundPermission findPermission(PermissionService service, Subject subject, String permission, Set<Context> contexts)
    {
        // First search in transient data
        FoundPermission foundTransient = findPermission(service, subject, subject.getTransientSubjectData(), permission, contexts, true);
        // Then search in persistent data
        return foundTransient != null ? foundTransient : findPermission(service, subject, subject.getSubjectData(), permission, contexts, true);
    }

    public static FoundPermission findPermission(PermissionService service, Subject subject, SubjectData data, String permission, Set<Context> contexts, boolean resolve)
    {
        // Directly assigned?
        Boolean set = data.getPermissions(contexts).get(permission);
        if (set != null)
        {
            return new FoundPermission(subject, permission, set); // Great this is done already
        }

        if (resolve) // Do we want to resolve the permission?
        {
            // Resolving...
            // Get implicit parent permissions
            List<String> implicits = getImplicitParents(permission);
            // Attempt to find explicit parent permissions
            PermissionDescription permDesc = service.getDescription(permission).orElse(null);
            if (permDesc != null)
            {
                // Get explicit parent permissions
                List<String> explicits = getExplicitParents(service, data, permission);
                for (String explicit : explicits)
                {
                    // Find the explicit permission-parent value
                    // TODO prevent from looking up implicit permissions multiple times
                    FoundPermission found = findPermission(service, subject, data, explicit, contexts, true); // recursive
                    if (found != null)
                    {
                        return found;
                    }
                }
            }
            // else no explicit parents defined
            // Attempt to find implicit parent permissions
            for (String implicit : implicits)
            {
                FoundPermission found = findPermission(service, subject, data, implicit, contexts, false); // not recursive (we got all already)
                if (found != null)
                {
                    return found;
                }
            }

            // Attempt to find permission in parents
            for (Subject parentSubject : data.getParents(contexts))
            {
                FoundPermission found = findPermission(service, parentSubject, permission, contexts);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<String> getExplicitParents(PermissionService service, SubjectData startingData, String permission)
    {
        return stream(service.getSubjects(SUBJECTS_ROLE_TEMPLATE).getAllSubjects().spliterator(), false)
                        .filter(s -> s.getSubjectData() != startingData)
                        .filter(s -> s.getIdentifier().startsWith(PERMISSION_TEMPLATE_PREFIX))
                        .map(s -> s.getIdentifier().substring(PERMISSION_TEMPLATE_PREFIX.length()))
                        .filter(i -> i.equals(permission))
                        .collect(toList());
    }

    public static List<String> getImplicitParents(String permission)
    {
        List<String> implicits = new ArrayList<>();

        String perm = permission;
        // Search for implicit parents first...
        int lastDot = perm.lastIndexOf(".");
        while (lastDot != -1)
        {
            perm = perm.substring(0, lastDot);
            implicits.add(perm);
            lastDot = perm.lastIndexOf(".");
        }
        return implicits;
    }

    public static Optional<FoundOption> getOption(OptionSubject subject, OptionSubjectData data, String key, Set<Context> contexts)
    {
        String result = data.getOptions(contexts).get(key);
        if (result != null)
        {
            return Optional.of(new FoundOption(subject, result));
        }
        for (Subject parent : data.getParents(contexts))
        {
            if (parent instanceof OptionSubject)
            {
                Optional<FoundOption> option = getOption(((OptionSubject)parent), key, contexts);
                if (option.isPresent())
                {
                    return option;
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<FoundOption> getOption(OptionSubject subject, String key, Set<Context> contexts)
    {
        Optional<FoundOption> option = getOption(subject, subject.getTransientSubjectData(), key, contexts);
        if (!option.isPresent())
        {
            option = getOption(subject, subject.getSubjectData(), key, contexts);
        }
        return option;
    }

    public static Text permText(CommandSource cmdSource, String permission, PermissionService service, I18n i18n)
    {
        Text permText = Text.of(permission);
        Optional<PermissionDescription> permDesc = service.getDescription(permission);
        if (permDesc.isPresent())
        {
            permText = permText.toBuilder().onHover(showText(permDesc.get().getDescription().toBuilder().color(YELLOW).build())).build();
        }
        else
        {
            permText = permText.toBuilder().onHover(showText(i18n.getTranslation(cmdSource, NEGATIVE, "Permission not registered"))).build();
        }
        return permText;
    }

    public static Context contextOf(String ctx)
    {
        String name = ctx;
        String type = Context.WORLD_KEY;
        if (name.contains("|"))
        {
            type = name.substring(0, name.indexOf("|"));
            name = name.substring(name.indexOf("|") + 1);
        }
        return "global".equals(name) ? GLOBAL : new Context(name, type);
    }

    public static final class FoundOption
    {
        public FoundOption(Subject subject, String value)
        {
            this.subject = subject;
            this.value = value;
        }

        public final Subject subject;
        public final String value;
    }

    public static final class FoundPermission
    {
        public FoundPermission(Subject subject, String permission, boolean value)
        {
            this.subject = subject;
            this.permission = permission;
            this.value = value;
        }

        public final Subject subject;
        public final String permission;
        public final boolean value;
    }

    public static void fillPermissions(Subject subject, Set<Context> contexts, Map<String, Boolean> data)
    {
        for (Entry<String, Boolean> entry : subject.getSubjectData().getPermissions(contexts).entrySet())
        {
            data.putIfAbsent(entry.getKey(), entry.getValue());
        }

        for (Subject parent : subject.getParents())
        {
            fillPermissions(parent, contexts, data);
        }
    }

    public static void fillOptions(OptionSubject subject, Set<Context> contexts, Map<String, String> data)
    {
        for (Entry<String, String> entry : subject.getSubjectData().getOptions(contexts).entrySet())
        {
            data.putIfAbsent(entry.getKey(), entry.getValue());
        }

        for (Subject parent : subject.getParents())
        {
            if (parent instanceof OptionSubject)
            {
                fillOptions(((OptionSubject)parent), contexts, data);
            }
        }
    }
}
