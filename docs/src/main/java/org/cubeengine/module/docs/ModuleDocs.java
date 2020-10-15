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

import org.cubeengine.logscribe.Log;
import org.cubeengine.butler.CommandBase;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.reflect.Reflected;
import org.cubeengine.reflect.ReflectedFile;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.plugin.PluginContainer;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public class ModuleDocs
{
    private final PluginContainer pc;
    private final String name;
    private final Info config;
    private final Set<PermissionDescription> permissions = new HashSet<>();
    private final Set<CommandBase> commands = new HashSet<>();
    private final String id;
    private final Permission basePermission;
    private final String moduleName;
    private final String moduleId;
    private final Reflector reflector;

    public String getModuleName()
    {
        return moduleName;
    }

    public String getName()
    {
        return name;
    }

    public String getId()
    {
        return id;
    }

    public ModuleDocs(PluginContainer plugin, Class module, Reflector reflector, PermissionManager pm, PermissionService ps, CommandManager cm, ModuleManager mm)
    {
        this.reflector = reflector;
        this.pc = plugin;
        this.name = plugin.getName();
        this.moduleName = mm.getModuleName(module).get();
        this.id = plugin.getId();
        this.moduleId = mm.getModuleID(module).get();
        InputStream is = plugin.getClass().getResourceAsStream("/assets/cubeengine/"+ moduleId + "-info.yml");
        if (is == null)
        {
            this.config = reflector.create(Info.class);
        }
        else
        {
            this.config = reflector.load(Info.class, new InputStreamReader(is));
        }
        this.basePermission = pm.getBasePermission(module);
        for (PermissionDescription perm : ps.getDescriptions())
        {
            if (perm.getId().startsWith(basePermission.getId() + ".") || perm.getId().equals(basePermission.getId()))
            {
                this.permissions.add(perm);
            }
        }

        for (CommandBase base : cm.getCommands())
        {
            if (base.getDescriptor().getOwner().equals(module))
            {
                this.commands.add(base);
            }
        }
    }

    public void generate(Path modulePath, DocType docType, Log log)
    {
        String generated = docType.getGenerator()
                .generate(log, this.id, this.name, this.pc, this.config, this.permissions, this.commands, this.basePermission);

        Path file = modulePath.resolve(this.id + docType.getFileExtension());
        try
        {
            Files.write(file, generated.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

            // Copy Markdown pages
            for (String pageName : this.config.pages.values())
            {
                InputStream is = this.pc.getClass().getResourceAsStream("/assets/cubeengine/" + this.moduleId + "-" + pageName + ".md");
                Path pageFileTarget = modulePath.resolve(this.id + "-" + pageName + ".md");
                Files.copy(is, pageFileTarget);
            }

            for (Class configClass : this.config.config) {
                ReflectedFile conf = ((ReflectedFile) reflector.create(configClass));
                StringWriter sw = new StringWriter();
                conf.save(sw);
                StringBuilder sb = new StringBuilder();
                sb.append("# ").append(configClass.getSimpleName()).append("\n\n");
                String configInfo = sw.toString();
                if (conf instanceof ReflectedYaml)
                {
                    sb.append("```yaml\n");
                }
                else
                {
                    sb.append("```\n");
                }
                sb.append(configInfo);
                sb.append("\n```\n");

                Files.write(modulePath.resolve(this.id + "-config-" + configClass.getSimpleName().toLowerCase() + ".md"), sb.toString().getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            }


        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean isWIP() {
        return this.config.workInProgress;
    }

    public boolean isOnOre()
    {
        return this.config.onOre != null;
    }
}
