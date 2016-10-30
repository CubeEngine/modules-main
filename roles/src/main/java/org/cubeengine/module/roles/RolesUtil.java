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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.service.subject.UserSubject;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.util.ContextUtil.GLOBAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.spongepowered.api.text.action.TextActions.showText;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

public class RolesUtil
{
    public static boolean debug = false;
    public static final Set<String> allPermissions = new HashSet<>();

    public static FoundPermission findPermission(PermissionService service, Subject subject, String permission, Set<Context> contexts)
    {
        // remember permissions checked for tab-completion etc.
        allPermissions.add(permission);
        // First search in transient data
        FoundPermission found = findPermission(service, subject, subject.getTransientSubjectData(), permission, contexts, true);
        // Then search in persistent data
        found = found != null ? found : findPermission(service, subject, subject.getSubjectData(), permission, contexts, true);
        // collection default
        Subject defaults = subject.getContainingCollection().getDefaults();
        // transient collection default data
        found = found != null ? found : findPermission(service, subject, defaults.getTransientSubjectData(), permission, contexts, true);
        // persistent collection default data
        found = found != null ? found : findPermission(service, subject, defaults.getSubjectData(), permission, contexts, true);
        // global default
        defaults = service.getDefaults();
        // transient global default data
        found = found != null ? found : findPermission(service, subject, defaults.getTransientSubjectData(), permission, contexts, true);
        // persistent global default data
        found = found != null ? found : findPermission(service, subject, defaults.getSubjectData(), permission, contexts, true);
        if (debug)
        {
            String name = subject.getIdentifier();
            if (subject instanceof UserSubject)
            {
                name = subject.getCommandSource().get().getName();
            }
            if (found == null)
            {
                System.out.print("[PermCheck] " + name + " has not " + permission + "\n");
            }
            else
            {
                System.out.print("[PermCheck] " + name + " has " + permission + " set to " + found.value + " as " + found.permission + " in " + found.subject.getIdentifier() + "\n");
            }
        }
        return found;
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
            // Attempt to find implicit parent permissions
            for (String implicit : getImplicitParents(permission))
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

    public static Optional<FoundOption> getOption(Subject subject, SubjectData data, String key, Set<Context> contexts)
    {
        String result = data.getOptions(contexts).get(key);
        if (result != null)
        {
            return Optional.of(new FoundOption(subject, result));
        }
        for (Subject parent : data.getParents(contexts))
        {
            Optional<FoundOption> option = getOption(parent, key, contexts);
            if (option.isPresent())
            {
                return option;
            }
        }
        return Optional.empty();
    }

    public static Optional<FoundOption> getOption(Subject subject, String key, Set<Context> contexts)
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
        return GLOBAL.getType().equals(name) ? GLOBAL : new Context(name, type);
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

    public static Map<String, Boolean> fillPermissions(Subject subject, Set<Context> contexts, Map<String, Boolean> data)
    {
        for (Entry<String, Boolean> entry : subject.getSubjectData().getPermissions(contexts).entrySet())
        {
            data.putIfAbsent(entry.getKey(), entry.getValue());
        }

        for (Subject parent : subject.getParents())
        {
            fillPermissions(parent, contexts, data);
        }

        return data;
    }

    public static Map<String, String> fillOptions(Subject subject, Set<Context> contexts, Map<String, String> data)
    {
        for (Entry<String, String> entry : subject.getSubjectData().getOptions(contexts).entrySet())
        {
            data.putIfAbsent(entry.getKey(), entry.getValue());
        }

        for (Subject parent : subject.getParents())
        {
            fillOptions(parent, contexts, data);
        }

        return data;
    }
}
