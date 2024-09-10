//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
// Example: private static final Logger LOG = LoggerFactory.getLogger(Server.class);

package ab.eazy.logging;

import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.event.Level;

public class EazyLoggerFactory implements ILoggerFactory
{
    private final EazyLoggerConfiguration configuration;
    private final ConcurrentMap<String, EazyLogger> loggerMap;

    public EazyLoggerFactory(EazyLoggerConfiguration config)
    {
        configuration = Objects.requireNonNull(config, "EazyLoggerConfiguration");
        loggerMap = new ConcurrentHashMap<>();
        EazyAppender appender = new EazyAppender(configuration);
    }


    /**
     * Main interface for {@link ILoggerFactory}
     *
     * @param name the name of the logger
     * @return the Slf4j Logger
     */
    @Override
    public Logger getLogger(String name)
    {
        return loggerMap.computeIfAbsent(name, this::createLogger);
    }

    void walkChildrenLoggers(String parentName, Consumer<EazyLogger> childConsumer)
    {
        String prefix = parentName;
        if (parentName.length() > 0 && !prefix.endsWith("."))
            prefix += ".";

        for (EazyLogger logger : loggerMap.values())
        {
            // Skip self.
            if (logger.getName().equals(parentName))
                continue;

            // It is a child, and is not itself.
            if (logger.getName().startsWith(prefix))
                childConsumer.accept(logger);
        }
    }

    private EazyLogger createLogger(String name)
    {
        EazyAppender appender = new EazyAppender(this.configuration);
        Level level = this.configuration.getLevel(name);
        boolean hideStacks = this.configuration.getHideStacks(name);
        return new EazyLogger(this, name, appender, level, hideStacks);
    }

    static <T> T walkParentLoggerNames(String startName, Function<String, T> nameFunction)
    {
        if (startName == null)
            return null;

        // Checking with FQCN first, then each package segment from longest to shortest.
        String nameSegment = startName;
        while (nameSegment.length() > 0)
        {
            T ret = nameFunction.apply(nameSegment);
            if (ret != null)
                return ret;

            // Trim and try again.
            int idx = nameSegment.lastIndexOf('.');
            if (idx >= 0)
                nameSegment = nameSegment.substring(0, idx);
            else
                break;
        }

        return nameFunction.apply(Logger.ROOT_LOGGER_NAME);
    }

/*
    public String[] getLoggerNames()
    {
        TreeSet<String> names = new TreeSet<>(loggerMap.keySet());
        return names.toArray(new String[0]);
    }

    public int getLoggerCount()
    {
        return loggerMap.size();
    }

    public String getLoggerLevel(String loggerName)
    {
        return walkParentLoggerNames(loggerName, key ->
        {
            EazyLogger logger = loggerMap.get(key);
            if (logger != null)
                return logger.getLevel().name();
            return null;
        });
    }

    public boolean setLoggerLevel(String loggerName, String levelName)
    {
        Level level = EazyLoggerConfiguration.toEazyLevel(loggerName, levelName);
        if (level == null)
        {
            return false;
        }
        EazyLogger eazyLogger = getEazyLogger(loggerName);
        eazyLogger.setLevel(level);
        return true;
    }
*/
}
