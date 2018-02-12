package org.cubeengine.module.protector.command.parser;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.Completer;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.List;

public class TristateParser implements ArgumentParser<Tristate>, Completer
{
    @Override
    public Tristate parse(Class aClass, CommandInvocation commandInvocation) throws ParserException
    {
        String token = commandInvocation.consume(1);
        switch (token.toLowerCase())
        {
            case "allow":
                return Tristate.TRUE;
            case "deny":
                return Tristate.FALSE;
            case "reset":
                return Tristate.UNDEFINED;
        }
        throw new ParserException(token);
    }

    @Override
    public List<String> suggest(Class aClass, CommandInvocation commandInvocation) {
        String token = commandInvocation.currentToken().toLowerCase();
        List<String> list = new ArrayList<>();
        if ("allow".startsWith(token))
        {
            list.add("allow");
        }
        if ("deny".startsWith(token))
        {
            list.add("deny");
        }
        if ("reset".startsWith(token))
        {
            list.add("reset");
        }
        return list;
    }
}
