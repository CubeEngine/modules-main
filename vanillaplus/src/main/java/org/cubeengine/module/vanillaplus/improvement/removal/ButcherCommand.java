package org.cubeengine.module.vanillaplus.improvement.removal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.StringUtils;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class ButcherCommand
{

    this.entityRemovals = new EntityRemovals(module);


    @Command(desc = "Gets rid of mobs close to you. Valid types are:\n" +
        "monster, animal, pet, golem, boss, other, creeper, skeleton, spider etc.")
    public void butcher(CommandSource context, @Label("types...") @Optional String types,
                        @Optional Integer radius,
                        @Named("in") World world,
                        @Flag boolean lightning, // die with style
                        @Flag boolean all) // infinite radius
    {
        User sender = null;
        if (context instanceof User)
        {
            sender = (User)context;
        }
        Location loc;
        radius = radius == null ? this.config.commands.butcherDefaultRadius : radius;
        if (radius < 0 && !(radius == -1 && module.perms().COMMAND_BUTCHER_FLAG_ALL.isAuthorized(context)))
        {
            i18n.sendTranslated(context, NEGATIVE, "The radius has to be a number greater than 0!");
            return;
        }
        int removed;
        if (sender == null)
        {
            radius = -1;
            loc = this.config.mainWorld.getWorld().getSpawnLocation();
        }
        else
        {
            loc = sender.getLocation();
        }
        if (all && module.perms().COMMAND_BUTCHER_FLAG_ALL.isAuthorized(context))
        {
            radius = -1;
        }
        lightning = lightning && module.perms().COMMAND_BUTCHER_FLAG_LIGHTNING.isAuthorized(context);
        Collection<Entity> list;
        if (context instanceof User && !(radius == -1))
        {
            list = ((User)context).getNearbyEntities(radius);
        }
        else
        {
            list = loc.getExtent().getEntities();
        }
        String[] s_types = { "monster" };
        boolean allTypes = false;
        if (types != null)
        {
            if ("*".equals(types))
            {
                allTypes = true;
                if (!module.perms().COMMAND_BUTCHER_FLAG_ALLTYPE.isAuthorized(context))
                {
                    i18n.sendTranslated(context, NEGATIVE, "You are not allowed to butcher all types of living entities at once!");
                    return;
                }
            }
            else
            {
                s_types = StringUtils.explode(",", types);
            }
        }
        List<Entity> remList = new ArrayList<>();
        if (!allTypes)
        {
            for (String s_type : s_types)
            {
                String match = stringMatcher.matchString(s_type, this.entityRemovals.GROUPED_ENTITY_REMOVAL.keySet());
                EntityType directEntityMatch = null;
                if (match == null)
                {
                    directEntityMatch = entityMatcher.mob(s_type);
                    if (directEntityMatch == null)
                    {
                        i18n.sendTranslated(context, NEGATIVE, "Unknown entity {input#entity}", s_type);
                        return;
                    }
                    if (this.entityRemovals.DIRECT_ENTITY_REMOVAL.get(directEntityMatch) == null) throw new IllegalStateException("Missing Entity? " + directEntityMatch);
                }
                EntityRemoval entityRemoval;
                if (directEntityMatch != null)
                {
                    entityRemoval = this.entityRemovals.DIRECT_ENTITY_REMOVAL.get(directEntityMatch);
                }
                else
                {
                    entityRemoval = this.entityRemovals.GROUPED_ENTITY_REMOVAL.get(match);
                }
                remList.addAll(list.stream().filter(entity -> entityRemoval.doesMatch(entity)
                    && entityRemoval.isAllowed(context)).collect(toList()));
            }
        }
        else
        {
            remList.addAll(list);
        }
        list = new ArrayList<>();
        for (Entity entity : remList)
        {
            if (entity instanceof Living)
            {
                list.add(entity);
            }
        }
        removed = this.removeEntities(list, loc, radius, lightning);
        if (removed == 0)
        {
            i18n.sendTranslated(context, NEUTRAL, "Nothing to butcher!");
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "You just slaughtered {amount} living entities!", removed);
        }

    }


    //eLoc.getExtent().spawnEntity(eLoc.getExtent().createEntity(EntityTypes.LIGHTNING, eLoc.getPosition()).get(), Cause.of(context));
}
