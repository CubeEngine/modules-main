package de.cubeisland.engine.module.roles.sponge.subject;

import java.util.Set;
import com.google.common.base.Optional;
import de.cubeisland.engine.module.roles.config.RoleConfig;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.data.RoleSubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.command.CommandSource;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;

public class RoleSubject extends BaseSubject implements Comparable<RoleSubject>
{
    public static final String SEPARATOR = "|";
    private final RoleSubjectData data;
    private final String roleName;
    private final Set<Context> contexts;

    public RoleSubject(RolesPermissionService service, RoleConfig config, Context context)
    {
        super(service.getGroupSubjects());
        this.contexts = context == null ? GLOBAL_CONTEXT : singleton(context);
        this.data = new RoleSubjectData(service, config, context);
        this.roleName = "role:" + (context == null ? "global" + SEPARATOR : context.getKey() + SEPARATOR + context.getValue() + SEPARATOR) + config.roleName;
    }

    @Override
    public OptionSubjectData getSubjectData()
    {
        return data;
    }

    @Override
    public String getIdentifier()
    {
        return roleName;
    }

    @Override
    public Optional<CommandSource> getCommandSource()
    {
        return Optional.absent();
    }

    @Override
    public Set<Context> getActiveContexts()
    {
        return unmodifiableSet(contexts);
    }

    @Override
    public int compareTo(RoleSubject o)
    {
        // Higher priority first
        return -Integer.compare(data.getConfig().priority.value, o.data.getConfig().priority.value);
    }

    public String getName()
    {
        return roleName.substring(roleName.lastIndexOf("|" + 1));
    }
}
