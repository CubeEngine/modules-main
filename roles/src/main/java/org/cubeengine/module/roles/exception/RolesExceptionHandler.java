package org.cubeengine.module.roles.exception;

import org.cubeengine.butler.CommandBase;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.exception.PriorityExceptionHandler;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.i18n.formatter.MessageType;
import org.spongepowered.api.util.command.CommandSource;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;

public class RolesExceptionHandler implements PriorityExceptionHandler
{
    private I18n i18n;

    public RolesExceptionHandler(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Override
    public boolean handleException(Throwable t, CommandBase command, CommandInvocation invocation)
    {
        if (t instanceof InvocationTargetException || t instanceof ExecutionException)
        {
            t = t.getCause();
        }

        CommandSource sender = (CommandSource) invocation.getCommandSource();


        if (t instanceof CircularRoleDependencyException)
        {
            int depth = ((CircularRoleDependencyException) t).getDepth();
            if (depth == 0)
            {
                i18n.sendTranslated(sender, NEGATIVE, "Cannot assign role to itself");
            }
            else
            {
                i18n.sendTranslated(sender, NEGATIVE, "Circular Dependency detected! Depth: {}", depth);
            }
        }
        return false;
    }

    @Override
    public int priority()
    {
        return 1;
    }
}
