package org.cubeengine.module.worlds;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;

import java.util.Optional;

public class WorldGeneratorModifierReader implements ArgumentReader<WorldGeneratorModifier>
{
    @Override
    public WorldGeneratorModifier read(Class aClass, CommandInvocation invocation) throws ReaderException
    {
        String token = invocation.consume(1);
        Optional<WorldGeneratorModifier> generator = Sponge.getRegistry().getType(WorldGeneratorModifier.class, token);
        if (generator.isPresent())
        {
            return generator.get();
        }
        throw new ReaderException("No Generator found for {}", token);
    }
}
