package de.cubeisland.cubeengine.core.util.convert.converter;

import de.cubeisland.cubeengine.core.config.node.BooleanNode;
import de.cubeisland.cubeengine.core.config.node.Node;
import de.cubeisland.cubeengine.core.util.convert.BasicConverter;
import de.cubeisland.cubeengine.core.util.convert.ConversionException;

public class BooleanConverter extends BasicConverter<Boolean>
{
    @Override
    public Boolean fromNode(Node node) throws ConversionException
    {
        if (node instanceof BooleanNode)
        {
            return ((BooleanNode)node).getValue();
        }
        String s = node.unwrap();
        try
        {
            if (s == null)
            {
                return null;
            }
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("1"))
            {
                return true;
            }
            if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("off") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("0"))
            {
                return false;
            }
            return null;
        }
        catch (NumberFormatException e)
        {
            throw new ConversionException("Invalid Node!" + node.getClass(), e);
        }
    }
}
