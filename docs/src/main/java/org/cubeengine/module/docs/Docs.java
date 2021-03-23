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
package org.cubeengine.module.docs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.plugin.PluginContainer;

import static org.cubeengine.module.docs.DocType.MARKDOWN;

@Singleton
@Module
public class Docs
{
    @Inject private Logger logger;
    @Inject private Reflector reflector;
    @InjectService private PermissionService ps;
    @Inject private PermissionManager pm;
    private Path modulePath;
    @Inject private ModuleManager mm;
    @Inject private FileManager fm;
    @ModuleCommand private DocsCommands docsCommands;

    @Listener(order = Order.LAST)
    public void onStartedServer(StartedEngineEvent<Server> event)
    {
        this.modulePath = fm.getModulePath(Docs.class);
        this.generateDocumentation();
        if ("true".equals(System.getenv("CUBEENGINE_DOCS_SHUTDOWN")))
        {
            Sponge.server().shutdown();
        }
    }

    public void generateDocumentation()
    {
        Map<String, ModuleDocs> docs = new TreeMap<>();
        for (Map.Entry<Class<?>, PluginContainer> entry : mm.getModulePlugins().entrySet())
        {
            docs.put(entry.getValue().getMetadata().getId(), new ModuleDocs(entry.getValue(), entry.getKey(), reflector, pm, ps, mm));
        }

        logger.info("Generating Module Docs...");

        try
        {
            String generated = MARKDOWN.getGenerator().generateList(docs, modulePath, mm);

            Path file = modulePath.resolve("README" + MARKDOWN.getFileExtension());
            try
            {
                Files.write(file, generated.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            Path moduleDocsPath = modulePath.resolve("modules");
            if (Files.exists(moduleDocsPath))
            {
                Files.walk(moduleDocsPath).sorted(Comparator.reverseOrder()).forEach(Docs::deleteFile);
            }

            Files.createDirectories(moduleDocsPath);


            for (Map.Entry<String, ModuleDocs> entry : docs.entrySet())
            {
                logger.info("Generating docs for " + entry.getKey());
                entry.getValue().generate(moduleDocsPath, MARKDOWN, logger);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }


        logger.info("Done Generating Module Docs!");
        logger.info(modulePath.toAbsolutePath().toString());
    }

    private static void deleteFile(Path path)
    {
        try
        {
            Files.delete(path);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

}
