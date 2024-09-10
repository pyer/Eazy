package ab.eazy.logging;

/*
public interface EazyAppender
{
    void emit(EazyLogger logger, Level level, long timestamp, String threadName,
              Throwable throwable, String message, Object... argumentArray);
}
*/

import java.io.PrintStream;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class EazyAppender
{
    /**
     * Configuration keys specific to the EazyAppender
     */
    static final String NAME_CONDENSE_KEY = "ab.eazy.logging.appender.NAME_CONDENSE";
    static final String MESSAGE_ALIGN_KEY = "ab.eazy.logging.appender.MESSAGE_ALIGN";
    static final String MESSAGE_ESCAPE_KEY = "ab.eazy.logging.appender.MESSAGE_ESCAPE";
    static final String ZONEID_KEY = "ab.eazy.logging.appender.ZONE_ID";
    private static final String EOL = System.lineSeparator();

    private ZoneId zoneId;

    /**
     * True to have output show condensed logger names, false to use the as defined long names.
     */
    private final boolean condensedNames;

    /**
     * True to have messages escaped for control characters, false to leave messages alone.
     */
    private final boolean escapedMessages;

    /**
     * The column to align the start of all messages to
     */
    private final int messageAlignColumn;

    /**
     * The stream to write logging events to.
     */
    private PrintStream stream;

    public EazyAppender(EazyLoggerConfiguration config)
    {
        this(config, null);
    }

    public EazyAppender(EazyLoggerConfiguration config, PrintStream stream)
    {
        this(config, stream, null);
    }

    public EazyAppender(EazyLoggerConfiguration config, PrintStream stream, TimeZone timeZone)
    {
        Objects.requireNonNull(config, "EazyLoggerConfiguration");
        this.stream = stream;

        TimeZone tzone = timeZone;
        if (tzone == null)
        {
            tzone = config.getTimeZone(ZONEID_KEY);
            if (tzone == null)
            {
                tzone = TimeZone.getDefault();
            }
        }
        this.zoneId = tzone.toZoneId();

        this.condensedNames = config.getBoolean(NAME_CONDENSE_KEY, true);
        this.escapedMessages = config.getBoolean(MESSAGE_ESCAPE_KEY, true);
        this.messageAlignColumn = config.getInt(MESSAGE_ALIGN_KEY, 0);
    }

    public void emit(EazyLogger logger, Level level, long timestamp, String threadName, Throwable throwable, String message, Object... argumentArray)
    {
        StringBuilder builder = new StringBuilder(64);
        format(builder, logger, level, timestamp, threadName, throwable, message, argumentArray);
        if (stream != null)
        {
            stream.println(builder);
        }
        else
        {
            System.err.println(builder);
        }
    }

    public boolean isCondensedNames()
    {
        return condensedNames;
    }

    public boolean isEscapedMessages()
    {
        return escapedMessages;
    }

    public int getMessageAlignColumn()
    {
        return messageAlignColumn;
    }

    public PrintStream getStream()
    {
        return stream;
    }

    public void setStream(PrintStream stream)
    {
        this.stream = stream;
    }

    private void format(StringBuilder builder, EazyLogger logger, Level level, long timestamp, String threadName, Throwable throwable, String message, Object... argumentArray)
    {
        Throwable cause = throwable;

        // Timestamp
        formatNow(timestamp, builder);

        // Level
        builder.append(':').append(renderedLevel(level));

        // Logger Name
        builder.append(':');
        if (condensedNames)
        {
            builder.append(logger.getCondensedName());
        }
        else
        {
            builder.append(logger.getName());
        }

        // Thread Name
        builder.append(':');
        builder.append(threadName);
        builder.append(':');

        // Message
        int padAmount = messageAlignColumn - builder.length();
        if (padAmount > 0)
            builder.append(" ".repeat(padAmount));
        else
            builder.append(' ');

        FormattingTuple ft = MessageFormatter.arrayFormat(message, argumentArray);
        appendEscaped(builder, ft.getMessage());
        if (cause == null) {
            cause = ft.getThrowable();
        }

        // Throwable
        if (cause != null) {
            appendCause(builder, cause, "", Collections.newSetFromMap(new IdentityHashMap<>()));
        }
    }

    private String renderedLevel(Level level)
    {
        switch (level)
        {
            case ERROR:  // New for Eazy 10+
                return "ERROR";
            case WARN:
                return "WARN ";
            case INFO:
                return "INFO ";
            case DEBUG:
                return "DEBUG";
            case TRACE: // New for Eazy 10+
                return "TRACE";
            default:
                return "UNKNOWN";
        }
    }

    private void appendCause(StringBuilder builder, Throwable cause, String indent, Set<Throwable> visited)
    {
        builder.append(EOL).append(indent);
        if (visited.contains(cause))
        {
            builder.append("[CIRCULAR REFERENCE: ");
            appendEscaped(builder, cause.toString());
            builder.append("]");
            return;
        }
        visited.add(cause);

        appendEscaped(builder, cause.toString());
        StackTraceElement[] elements = cause.getStackTrace();
        for (int i = 0; elements != null && i < elements.length; i++)
        {
            builder.append(EOL).append(indent).append("\tat ");
            appendEscaped(builder, elements[i].toString());
        }

        for (Throwable suppressed : cause.getSuppressed())
        {
            builder.append(EOL).append(indent).append("Suppressed: ");
            appendCause(builder, suppressed, "\t|" + indent, visited);
        }

        Throwable by = cause.getCause();
        if (by != null && by != cause)
        {
            builder.append(EOL).append(indent).append("Caused by: ");
            appendCause(builder, by, indent, visited);
        }
    }

    private void appendEscaped(StringBuilder builder, String str)
    {
        if (str == null)
            return;

        if (escapedMessages)
        {
            for (int i = 0; i < str.length(); ++i)
            {
                char c = str.charAt(i);
                if (Character.isISOControl(c))
                {
                    if (c == '\n')
                    {
                        builder.append('|');
                    }
                    else if (c == '\r')
                    {
                        builder.append('<');
                    }
                    else
                    {
                        builder.append('?');
                    }
                }
                else
                {
                    builder.append(c);
                }
            }
        }
        else
            builder.append(str);
    }

    /**
     * Format a timestamp according to our stored formatter.
     * The passed time is expected to be close to the current time, so it is
     * compared to the last value passed and if it is within the same second,
     * the format is reused.  Otherwise a new cached format is created.
     *
     * @param now the milliseconds since unix epoch
     */
    private void formatNow(long now, StringBuilder builder)
    {
        long seconds = now / 1000;
        int ms = (int)(now % 1000);

        DateTimeFormatter tzFormatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(zoneId);

        String s = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zoneId).format(tzFormatter);
        builder.append(s);

        if (ms > 99) {
            builder.append('.');
        } else if (ms > 9) {
            builder.append(".0");
        } else {
            builder.append(".00");
        }
        builder.append(ms);
    }

}

