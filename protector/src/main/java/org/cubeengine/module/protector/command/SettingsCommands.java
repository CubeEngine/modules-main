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
package org.cubeengine.module.protector.command;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.protector.region.RegionConfig.setOrUnset;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.listener.SettingsListener;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionParser;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

@Command(name = "control", desc = "Manages the region settings")
public class SettingsCommands extends AbstractSettingsCommand
{

    private PermissionManager pm;

    public SettingsCommands(RegionManager manager, I18n i18n, PermissionService ps, PermissionManager pm, EventManager em, CommandManager cm)
    {
        super(cm, Protector.class, i18n, new SettingsListener(manager, pm.getBasePermission(Protector.class), pm, i18n), ps);
        this.pm = pm;
        cm.getProviders().register(this, new RegionParser(manager, i18n), Region.class);
        em.registerListener(Protector.class, this.psl);

        this.addCommand(new BlockDamageSettingsCommands(cm, i18n, psl, ps));
        this.addCommand(new EntityDamageSettingsCommands(cm, i18n, psl, ps));
        this.addCommand(new PlayerDamageSettingsCommands(cm, i18n, psl, ps));
    }

    @Command(desc = "Controls teleport movement")
    public void teleport(CommandSource context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        this.move(context, SettingsListener.MoveType.TELEPORT, set, region, role);
    }

    @Command(desc = "Controls movement")
    public void move(CommandSource context, SettingsListener.MoveType type, Tristate set,
            @Default @Named("in") Region region,
            @Named("bypass") String role) // TODO role completer/reader
    {
        if (role != null)
        {
            this.setPermission(context, set, region, role, psl.movePerms.get(type).getId());
            return;
        }
        //for (MoveListener.MoveType type : types)
        {
            setOrUnset(region.getSettings().move, type, set);
        }
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: Move Settings updated", region);
    }


    @Command(desc = "Controls player building")
    public void build(CommandSource context, Tristate set,
            @Default @Named("in") Region region,
            @Named("bypass") String role) // TODO role completer/reader
    {
        if (role != null)
        {
            this.setPermission(context, set, region, role, psl.buildPerm.getId());
            return;
        }
        region.getSettings().build = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: Build Settings updated", region);
    }

    @Command(desc = "Controls players interacting with blocks")
    public void useAll(CommandSource context, SettingsListener.UseType type, Tristate set,
            @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            this.setPermission(context, set, region, role, psl.usePermission.get(type).getId());
            return;
        }
        switch (type)
        {
            case ITEM:
                region.getSettings().use.all.item = set;
                break;
            case BLOCK:
                region.getSettings().use.all.block = set;
                break;
            case CONTAINER:
                region.getSettings().use.all.container = set;
                break;
            case OPEN:
                region.getSettings().use.all.open = set;
                break;
            case REDSTONE:
                region.getSettings().use.all.redstone = set;
                break;
        }
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: Use Settings updated", region);
    }

    @Command(desc = "Controls player interacting with blocks")
    public void useBlock(CommandSource context, BlockType type, Tristate set,
            @Default @Named("in") Region region,
            @Named("bypass") String role) // TODO role completer/reader
    {
        if (role != null)
        {
            this.setPermission(context, set, region, role, psl.useBlockPerm.getId());
            return;
        }
        setOrUnset(region.getSettings().use.block, type, set);
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: Use Block Settings updated", region);
    }

    @Command(desc = "Controls player interactive with items")
    public void useItem(CommandSource context, ItemType type, Tristate set,
            @Default @Named("in") Region region,
            @Named("bypass") String role) // TODO role completer/reader
    {
        if (role != null)
        {
            this.setPermission(context, set, region, role, psl.useItemPerm.getId());
            return;
        }
        setOrUnset(region.getSettings().use.item, type, set);
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: Use Item Settings updated", region);
    }

    @Command(desc = "Controls spawning of entities")
    // TODO completer for SpawnType & EntityType
    public void spawn(CommandSource context, SettingsListener.SpawnType type, EntityType what, Tristate set,
            @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            switch (type)
            {
                case PLAYER:
                    this.setPermission(context, set, region, role, psl.spawnEntityPlayerPerm.getId());
                    return;
                case NATURALLY:
                case PLUGIN:
                    i18n.send(context, NEGATIVE, "There is no bypass permission for natural or plugin only spawning.");
                    return;
            }
            throw new IllegalStateException("impossible!");
        }
        switch (type)
        {
            case NATURALLY:
                setOrUnset(region.getSettings().spawn.naturally, what, set);
                break;
            case PLAYER:
                setOrUnset(region.getSettings().spawn.player, what, set);
                break;
            case PLUGIN:
                setOrUnset(region.getSettings().spawn.plugin, what, set);
                break;
        }
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: Spawn Settings updated", region);
    }

    @Command(desc = "Controls executing commands")
    // TODO completer for commands
    public void command(CommandSource context, String command, Tristate set,
            @Default @Named("in") Region region, @Named("bypass") String role,
            @Flag boolean force)
    {
        CommandMapping mapping = Sponge.getGame().getCommandManager().get(command).orElse(null);
        boolean all = "*".equals(command);
        if (mapping == null && !all)
        {
            i18n.send(context, NEGATIVE, "The command {name} is not a registered command", command);
            if (!force)
            {
                i18n.send(context, NEUTRAL, "Use the -force Flag to block it anyways");
                return;
            }
        }

        if (role != null)
        {
            if (all)
            {
                this.setPermission(context, set, region, role, psl.command.getId());
            }
            else
            {
                Permission perm = pm.register(Protector.class, command, "Region bypass for using command: " + command , psl.command);
                this.setPermission(context, set, region, role, perm.getId());
            }
            return;
        }

        // Block primary alias instead of parameter if found
        setOrUnset(region.getSettings().blockedCommands, mapping == null ? command : mapping.getPrimaryAlias(), set);
        if (set == Tristate.UNDEFINED)
        {
            region.getSettings().blockedCommands.remove(command);
        }
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: Command Settings updated", region);
    }


    @Command(desc = "Controls redstone circuits commands")
    public void deadCircuit(CommandSource context, Tristate set, @Default @Named("in") Region region)
    {
        region.getSettings().deadCircuit = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: Dead Circuit Settings updated", region);
    }
}
