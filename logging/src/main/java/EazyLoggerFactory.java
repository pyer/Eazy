
package ab.eazy.logging;
// Example: private static final Logger LOG = LoggerFactory.getLogger(Server.class);

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
     * Get a {@link EazyLogger} instance, creating if not yet existing.
     *
     * @param name the name of the logger
     * @return the EazyLogger instance
     */
    public EazyLogger getEazyLogger(String name)
    {
        return loggerMap.computeIfAbsent(name, this::createLogger);
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
        return getEazyLogger(name);
    }

    private EazyLogger createLogger(String name)
    {
        EazyAppender appender = new EazyAppender(this.configuration);
        Level level = this.configuration.getLevel(name);
        boolean hideStacks = this.configuration.getHideStacks(name);
        return new EazyLogger(this, name, appender, level, hideStacks);
    }
}
