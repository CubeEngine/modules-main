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
package org.cubeengine.module.roles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.util.ContextUtil.GLOBAL;

public class RolesUtil
{
    public static boolean debug = false;
    public static final Set<String> allPermissions = new HashSet<>();
    private static Map<String, Map<String, Map<Set<Context>, List<Subject>>>> cache = new HashMap<>();

    public static void invalidateCache()
    {
        cache.clear();
    }

    public static FoundPermission findPermission(PermissionService service, Subject subject, String permission, Set<Context> contexts)
    {
        FoundPermission found = findPermission(service, subject, permission, contexts, new HashSet<>());
        if (debug)
        {
            final String name =  subject.containingCollection().identifier() + ":" + subject.friendlyIdentifier().orElse(subject.identifier());
            if (found == null)
            {
                System.out.print("[PermCheck] " + name + " has not " + permission + "\n");
            }
            else
            {
                System.out.print("[PermCheck] " + name + " has " + permission + " set to " + found.value + " as " + found.permission + " in " + found.subject.identifier() + "\n");
            }
        }
        return found;
    }

    public static FoundPermission findPermission(PermissionService service, Subject subject, String permission, Set<Context> contexts, Set<Subject> checked)
    {
        checked.add(subject); // prevent checking a subject multiple times

        // remember permissions checked for tab-completion etc.
        allPermissions.add(permission);

        // First search in the subject and its parents
        FoundPermission found = findPermission0(service, subject, permission, contexts, true, checked);

        // If not found check in default
        if (found == null)
        {
            Subject defaults = subject.containingCollection().defaults();
            if (defaults == subject)
            {
                return null; // Stop recursion at global default
            }
            found = findPermission(service, defaults, permission, contexts, checked);
        }

        return found;
    }

    public static FoundPermission findPermission0(PermissionService service, Subject subject, String permission, Set<Context> contexts, boolean resolve, Set<Subject> checked)
    {
        SubjectData transientData = subject.transientSubjectData();
        SubjectData data = subject.subjectData();

        // Directly assigned transient?
        Boolean set = transientData.permissions(contexts).get(permission);
        if (set != null)
        {
            return new FoundPermission(subject, permission, set); // Great this is done already
        }
        // Directly assigned persistent?
        set = data.permissions(contexts).get(permission);
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
                FoundPermission found = findPermission0(service, subject, implicit, contexts, false, checked); // not recursive (we got all already)
                if (found != null)
                {
                    return found;
                }
            }

            // Attempt to find permission in parents
            List<Subject> list = new ArrayList<>(getParents(contexts, subject));
            list.sort(FileSubject::compare);

            for (Subject parentSubject : list)
            {
                if (checked.contains(parentSubject))
                {
                    continue;
                }
                checked.add(parentSubject);
                // Find permission in parent role - parents are ordered by priority
                FoundPermission found = findPermission(service, parentSubject, permission, contexts, checked);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<Subject> getParents(Set<Context> contexts, Subject subject)
    {
        Map<String, Map<Set<Context>, List<Subject>>> collectionMap = cache.computeIfAbsent(subject.containingCollection().identifier(), k -> new HashMap<>());
        Map<Set<Context>, List<Subject>> subjectMap = collectionMap.computeIfAbsent(subject.identifier(), k -> new HashMap<>());
        return subjectMap.computeIfAbsent(contexts, c -> {
            List<Subject> list = new ArrayList<>();
            list.addAll(subject.subjectData().parents(contexts).stream().map(sr -> sr.resolve().join()).collect(Collectors.toList()));
            list.addAll(subject.transientSubjectData().parents(contexts).stream().map(sr -> sr.resolve().join()).collect(Collectors.toList()));
            return list;
        });
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

    public static Optional<FoundOption> getOption(PermissionService service, Subject subject, SubjectData data, String key, Set<Context> contexts)
    {
        String result = data.options(contexts).get(key);
        if (result != null)
        {
            return Optional.of(new FoundOption(subject, result));
        }

        return Optional.empty();
    }

    public static Optional<FoundOption> getOption(PermissionService service, Subject subject, String key, Set<Context> contexts, boolean recurseDefault)
    {
        Optional<FoundOption> option = getOption(service, subject, subject.transientSubjectData(), key, contexts);
        if (!option.isPresent())
        {
            option = getOption(service, subject, subject.subjectData(), key, contexts);
        }

        if (!option.isPresent()) {

            final List<Subject> parents = getParents(contexts, subject);
            parents.sort(FileSubject::compare);
            for (Subject parent : parents)
            {
                // TODO do not check a parent multiple times
                option = getOption(service, parent, key, contexts, false);
                if (option.isPresent())
                {
                    return option;
                }
            }

        }

        if (recurseDefault && !option.isPresent())
        {
            Subject defaults = subject.containingCollection().defaults();
            if (defaults == subject)
            {
                return option; // Stop recursion at global default
            }
            option = getOption(service, defaults, key, contexts, true);
        }

        return option;
    }

    public static Component permText(CommandCause cmdSource, String permission, PermissionService service, I18n i18n)
    {
        Component permText = Component.text(permission);
        Optional<? extends PermissionDescription> permDesc = service.description(permission);
        if (permDesc.isPresent())
        {
            if (permDesc.get().description().isPresent())
            {
                permText = permText.hoverEvent(HoverEvent.showText(permDesc.get().description().get().color(NamedTextColor.YELLOW)));
            }
            // TODO else
        }
        else
        {

            permText = permText.hoverEvent(HoverEvent.showText(i18n.translate(cmdSource, NEGATIVE, "Permission not registered")));
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
        return GLOBAL.getKey().equals(name) ? GLOBAL : new Context(name, type);
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

    public static Map<String, Boolean> fillPermissions(Subject subject, Set<Context> contexts, Map<String, Boolean> data, PermissionService service)
    {
        for (Entry<String, Boolean> entry : subject.subjectData().permissions(contexts).entrySet())
        {
            data.putIfAbsent(entry.getKey(), entry.getValue());
        }

        for (Subject parent : getParents(contexts, subject))
        {
            fillPermissions(parent, contexts, data, service);
        }

        return data;
    }

    public static Map<String, FoundOption> fillOptions(Subject subject, Set<Context> contexts, Map<String, FoundOption> data, PermissionService service)
    {
        for (Entry<String, String> entry : subject.subjectData().options(contexts).entrySet())
        {
            data.putIfAbsent(entry.getKey(), new FoundOption(subject, entry.getValue()));
        }

        for (Subject parent : getParents(contexts, subject))
        {
            fillOptions(parent, contexts, data, service);
        }

        return data;
    }
}
