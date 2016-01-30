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
package org.cubeengine.module.basics;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.module.fixes.FixListener;
import org.cubeengine.module.basics.command.moderation.UnlimitedListener;
import org.cubeengine.module.basics.command.general.InformationCommands;
import de.cubeisland.engine.module.basics.command.general.LagTimer;
import de.cubeisland.engine.module.basics.command.general.ListCommand;
import org.cubeengine.module.basics.command.PlayerCommands;
import de.cubeisland.engine.module.basics.command.general.RolesListCommand;
import org.cubeengine.module.basics.command.moderation.DoorCommand;
import org.cubeengine.module.basics.command.InventoryCommands;
import org.cubeengine.module.basics.command.ItemCommands;
import org.cubeengine.module.basics.command.moderation.PaintingListener;
import de.cubeisland.engine.module.basics.command.moderation.WeatherTimeCommands;
import de.cubeisland.engine.module.vanillaplus.removal.RemovalCommands;
import de.cubeisland.engine.module.vanillaplus.SpawnMobCommand;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.core.util.InventoryGuardFactory;
import org.cubeengine.module.core.util.matcher.MaterialMatcher;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.ban.BanService;

@ModuleInfo(name = "Basics", description = "Basic Functionality")
public class Basics extends Module
{
    private BasicsConfiguration config;
    private LagTimer lagTimer;


    public BasicsPerm perms()
    {
        return perms;
    }

    private BasicsPerm perms;
    @Inject private Database db;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private UserManager um;
    @Inject private FileManager fm;
    @Inject private WorldManager wm;
    @Inject private TaskManager taskManager;
    @Inject private BanService bs;
    @Inject private PermissionManager pm;
    @Inject private InventoryGuardFactory invGuard;
    @Inject private MaterialMatcher materialMatcher;
    @Inject private Game game;

    @Enable
    public void onEnable()
    {
        this.config = fm.loadConfig(this, BasicsConfiguration.class);
        perms = new BasicsPerm(this, wm, pm);
        um.addDefaultAttachment(BasicsAttachment.class, this);
        em.registerListener(this, new ColoredSigns(this));
        cm.addCommands(cm, this, new InformationCommands(this, wm, materialMatcher));
        cm.addCommands(cm, this, new PlayerCommands(this, um, em, taskManager, cm, bs));
        em.registerListener(this, new UnlimitedListener(this, um));
        cm.addCommands( this, new InventoryCommands(this, invGuard));
        cm.addCommands(this, new ItemCommands(this));
        cm.addCommands(cm, this, new WeatherTimeCommands(this, taskManager, wm));
        cm.addCommands(cm, this, new RemovalCommands(this));
        em.registerListener(this, new PaintingListener(this, um));
        em.registerListener(this, new FixListener(this));
        this.lagTimer = new LagTimer(this);
        cm.addCommands(cm, this, new DoorCommand(this));
    }

    public BasicsConfiguration getConfiguration()
    {
        return this.config;
    }

    public LagTimer getLagTimer()
    {
        return this.lagTimer;
    }
}
