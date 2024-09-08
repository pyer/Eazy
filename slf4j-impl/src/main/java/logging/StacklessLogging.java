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
//

package ab.eazy.logging;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A try-with-resources compatible layer for {@link EazyLogger#setHideStacks(boolean) hiding stacktraces} within the scope of the <code>try</code> block when
 * logging with {@link EazyLogger} implementation.
 * <p>
 * Use of other logging implementation cause no effect when using this class
 * <p>
 * Example:
 *
 * <pre>
 * try (StacklessLogging scope = new StacklessLogging(EventDriver.class,Noisy.class))
 * {
 *     doActionThatCausesStackTraces();
 * }
 * </pre>
 */
public class StacklessLogging implements AutoCloseable
{
    private static final Logger LOG = LoggerFactory.getLogger(StacklessLogging.class);
    private static final EazyLoggerFactory loggerFactory;

    static
    {
        EazyLoggerFactory eazyLoggerFactory = null;
        ILoggerFactory activeLoggerFactory = LoggerFactory.getILoggerFactory();
        if (activeLoggerFactory instanceof EazyLoggerFactory)
        {
            eazyLoggerFactory = (EazyLoggerFactory)activeLoggerFactory;
        }
        else
        {
            LOG.warn("Unable to squelch stacktraces ({} is not a {})",
                activeLoggerFactory.getClass().getName(),
                EazyLoggerFactory.class.getName());
        }
        loggerFactory = eazyLoggerFactory;
    }

    private final Set<EazyLogger> squelched = new HashSet<>();

    public StacklessLogging(Class<?>... classesToSquelch)
    {
        this(Stream.of(classesToSquelch)
            .map(Class::getName)
            .toArray(String[]::new));
    }

    public StacklessLogging(Package... packagesToSquelch)
    {
        this(Stream.of(packagesToSquelch)
            .map(Package::getName)
            .toArray(String[]::new));
    }

    public StacklessLogging(String... loggerNames)
    {
        this(Stream.of(loggerNames)
            .map(loggerFactory::getEazyLogger)
            .toArray(Logger[]::new)
        );
    }

    public StacklessLogging(Logger... logs)
    {
        if (loggerFactory == null)
            return;

        for (Logger log : logs)
        {
            if (log instanceof EazyLogger eazyLogger && !eazyLogger.isDebugEnabled())
            {
                if (!eazyLogger.isHideStacks())
                {
                    eazyLogger.setHideStacks(true);
                    squelched.add(eazyLogger);
                }
            }
        }
    }

    @Override
    public void close()
    {
        for (EazyLogger log : squelched)
        {
            log.setHideStacks(false);
        }
    }
}
