package de.cubeisland.cubeengine.core.config.codec;

import de.cubeisland.cubeengine.core.config.Configuration;
import de.cubeisland.cubeengine.core.config.InvalidConfigurationException;
import de.cubeisland.cubeengine.core.config.annotations.Revision;
import de.cubeisland.cubeengine.core.config.annotations.Updater;
import de.cubeisland.cubeengine.core.config.node.IntNode;
import de.cubeisland.cubeengine.core.config.node.MapNode;
import de.cubeisland.cubeengine.core.config.node.Node;

import java.io.File;
import java.io.InputStream;

/**
 * This abstract Codec can be implemented to read and write configurations.
 */
public abstract class ConfigurationCodec<Config extends Configuration>
{
    protected String COMMENT_PREFIX;
    protected String OFFSET;
    protected String LINE_BREAK;
    protected String QUOTE;
    protected final String PATH_SEPARATOR = ":";
    protected boolean first;

    /**
     * Loads in the given configuration using the InputStream
     *
     * @param config the config to load
     * @param is the InputStream to load from
     */
    public void load(Config config, InputStream is) throws InstantiationException, IllegalAccessException
    {
        CodecContainer container = new CodecContainer(this);
        container.fillFromInputStream(is);
        Revision a_revision = config.getClass().getAnnotation(Revision.class);
        if (a_revision != null)
        {
            if (config.getClass().isAnnotationPresent(Updater.class))
            {
                if (a_revision.value() > container.revision)
                {
                    Updater updater = config.getClass().getAnnotation(Updater.class);
                    updater.value().newInstance().update(container.values, container.revision);
                }
            }
        }
        container.dumpIntoFields(config, container.values);
    }

    /**
     * Returns the offset as String
     *
     * @param offset the offset
     * @return the offset
     */
    protected String offset(int offset)
    {
        StringBuilder off = new StringBuilder("");
        for (int i = 0; i < offset; ++i)
        {
            off.append(OFFSET);
        }
        return off.toString();
    }

    /**
     * Saves the configuration into given file
     *
     * @param config the configuration to save
     * @param file the file to save into
     */
    public void save(Config config, File file)
    {
        try
        {
            if (file == null)
            {
                throw new IllegalStateException("Tried to save config without File.");
            }
            CodecContainer container = new CodecContainer(this);
            container.values = MapNode.emptyMap();
            Revision a_revision = config.getClass().getAnnotation(Revision.class);
            if (a_revision != null)
            {
                container.values.setNodeAt("revision", PATH_SEPARATOR, new IntNode(a_revision.value()));
            }
            container.fillFromFields(config, container.values);
            container.saveIntoFile(config, file);
        }
        catch (Exception ex)
        {
            throw new InvalidConfigurationException("Error while saving Configuration!", ex);
        }
    }

    /**
     * Serializes the values in the map
     *
     * @param container the codec-container
     * @param values the values at given path
     * @param off the current offset
     * @param inCollection
     * @return  the serialized value
     */
    public abstract String convertMap(CodecContainer container, MapNode values, int off, boolean inCollection);

    /**
     * Serializes a single value
     *
     * @param container the codec-container
     * @param value the value at given path
     * @param off the current offset
     * @param inCollection
     * @return
     */
    public abstract String convertValue(CodecContainer container, Node value, int off, boolean inCollection);

    /**
     * Builds a the comment for given path
     *
     * @param path the path
     * @param off the current offset
     * @return the comment
     */
    public abstract String buildComment(CodecContainer container, String path, int off);

    /**
     * Returns the FileExtension as String
     *
     * @return the fileExtension
     */
    public abstract String getExtension();

    /**
     * Converts the inputStream into a readable Object
     * @param container the container to fill with values
     * @param is the InputStream
     */
    public abstract void loadFromInputStream(CodecContainer container, InputStream is);
}