package de.cubeisland.cubeengine.core.module;

import de.cubeisland.cubeengine.core.persistence.filesystem.config.Configuration;
import de.cubeisland.cubeengine.core.persistence.filesystem.config.annotations.Option;
import de.cubeisland.cubeengine.core.persistence.filesystem.config.annotations.Codec;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Phillip Schichtel
 */
@Codec("yml")
public class ModuleConfiguration extends Configuration
{
    @Option("name")
    public String name;

    @Option("revision")
    public int revision;

    @Option("description")
    public String description;

    @Option(value = "dependencies", genericType = String.class)
    public Set<String> dependencies = new HashSet<String>();

    @Option(value = "soft-dependencies", genericType = String.class)
    public Set<String> softDependencies = new HashSet<String>();

    @Override
    public void onLoaded()
    {
        this.dependencies.remove(this.name);
        for (String dep : this.dependencies)
        {
            dep = dep.toLowerCase();
        }
        this.softDependencies.remove(this.name);
        for (String dep : this.softDependencies)
        {
            dep = dep.toLowerCase();
        }
    }
}
