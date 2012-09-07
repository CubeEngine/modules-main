package de.cubeisland.cubeengine.core.module;

import de.cubeisland.cubeengine.core.util.Validate;
import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 *
 * @author Phillip Schichtel
 */
public final class ModuleInfo
{
    private final File file;
    private final String name;
    private final int revision;
    private final String description;
    private final int minCoreVersion;
    private final Set<String> dependencies;
    private final Set<String> softDependencies;

    public ModuleInfo(File file, ModuleConfiguration config)
    {
        Validate.notNull(config, "The module configuration failed to loaded!");

        this.file               = file;
        this.name               = config.name.substring(0, 1).toUpperCase(Locale.ENGLISH) + config.name.substring(1).toLowerCase(Locale.ENGLISH);
        this.revision           = config.revision;
        this.description        = config.description;
        this.minCoreVersion     = config.minCoreRevision;
        this.dependencies       = Collections.unmodifiableSet(config.dependencies);
        this.softDependencies   = Collections.unmodifiableSet(config.softDependencies);
    }

    public File getFile()
    {
        return this.file;
    }

    public String getName()
    {
        return this.name;
    }

    public int getRevision()
    {
        return this.revision;
    }

    public String getDescription()
    {
        return this.description;
    }
    
    public int getMinimumCoreRevision()
    {
        return this.minCoreVersion;
    }

    public Set<String> getDependencies()
    {
        return this.dependencies;
    }

    public Set<String> getSoftDependencies()
    {
        return this.softDependencies;
    }
}