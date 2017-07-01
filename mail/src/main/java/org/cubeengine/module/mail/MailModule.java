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
package org.cubeengine.module.mail;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.module.mail.storage.TableMail;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.database.ModuleTables;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;

@Singleton
@Module(id = "mail", name = "Mail", version = "1.0.0",
        description = "Send ingame Mails",
        dependencies = @Dependency("cubeengine-core"),
        url = "http://cubeengine.org",
        authors = {"Anselm 'Faithcaio' Brehme", "Phillip Schichtel"})
@ModuleTables(TableMail.class)
public class MailModule extends CubeEngineModule
{
    @Inject private Database db;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private TaskManager tm;
    @Inject private I18n i18n;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        MailCommand cmd = new MailCommand(cm, this, tm, db, i18n);
        cm.addCommand(cmd);
        em.registerListener(MailModule.class, cmd);
    }
}
