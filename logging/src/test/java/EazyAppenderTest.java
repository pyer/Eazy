
package ab.eazy.logging;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import nut.annotations.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

public class EazyAppenderTest
{

    private class CapturedStream extends PrintStream
    {
      private final ByteArrayOutputStream test;

      private CapturedStream()
      {
        super(new ByteArrayOutputStream(), true, UTF_8);
        test = (ByteArrayOutputStream)super.out;
      }

      @Override
      public String toString()
      {
        return new String(test.toByteArray());
      }
    }

    @Test(enabled=true)
    public void testEazyLogFormat()
    {
        Properties props = new Properties();
        props.setProperty(EazyAppender.ZONEID_KEY, "UTC");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        EazyLoggerFactory factory = new EazyLoggerFactory(config);
        CapturedStream output = new CapturedStream();
        EazyAppender appender = (EazyAppender)factory.getEazyLogger("ROOT").getAppender();
        appender.setStream(output);
        EazyLogger logger = factory.getEazyLogger("ab.eazy.logging.LogTest");

        String threadName = "tname";
        // Feb 17th, 2020 at 19:11:35 UTC (with 563 millis)
        long timestamp = 1581966695563L;
        appender.emit(logger, Level.INFO, timestamp, threadName, null, "testing:{},{}", "test log", "format");
        nut.Assert.assertEquals(output.toString(),"2020-02-17 19:11:35.563:INFO :ael.LogTest:tname: testing:test log,format\n");
    }

//    @Test(expectedExceptions = IllegalArgumentException.class)
    @Test
    public void testCircularThrowable()
    {
        EazyLoggerConfiguration config = new EazyLoggerConfiguration();
        EazyLoggerFactory factory = new EazyLoggerFactory(config);
        CapturedStream output = new CapturedStream();
        EazyAppender appender = new EazyAppender(config);
        appender.setStream(output);
        EazyLogger logger = factory.getEazyLogger("ab.eazy.logging.LogTest");

        // Build an exception with circular refs.
        IllegalArgumentException commonCause = new IllegalArgumentException();
        Throwable thrown = new Throwable(commonCause);
        RuntimeException suppressed = new RuntimeException(thrown);
        thrown.addSuppressed(suppressed);

        appender.emit(logger, Level.INFO, System.currentTimeMillis(), "tname", thrown, "the message");
        nut.Assert.assertTrue(output.toString().contains("CIRCULAR REFERENCE"));
    }

    @Test
    public void testCondensedNames()
    {
        Properties props = new Properties();
        props.setProperty(EazyAppender.NAME_CONDENSE_KEY, "false");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        EazyAppender appender = new EazyAppender(config);
        nut.Assert.assertFalse(appender.isCondensedNames());

        // Default value
        config = new EazyLoggerConfiguration();
        appender = new EazyAppender(config);
        nut.Assert.assertTrue(appender.isCondensedNames());
    }

    @Test
    public void testEscapedMessages()
    {
        Properties props = new Properties();
        props.setProperty(EazyAppender.MESSAGE_ESCAPE_KEY, "false");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        EazyAppender appender = new EazyAppender(config);
        nut.Assert.assertFalse(appender.isEscapedMessages());

        // Default value
        config = new EazyLoggerConfiguration();
        appender = new EazyAppender(config);
        nut.Assert.assertTrue(appender.isEscapedMessages());
    }

    @Test
    public void testMessageAlignColumn()
    {
        Properties props = new Properties();
        props.setProperty(EazyAppender.MESSAGE_ALIGN_KEY, "42");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        EazyAppender appender = new EazyAppender(config);
        nut.Assert.assertEquals(appender.getMessageAlignColumn(), 42);

        // Default value
        config = new EazyLoggerConfiguration();
        appender = new EazyAppender(config);
        nut.Assert.assertEquals(appender.getMessageAlignColumn(), 0);
    }

    @Test
    public void testPrintStream()
    {
        PrintStream output = new PrintStream(new ByteArrayOutputStream(), true, UTF_8);
        EazyLoggerConfiguration config = new EazyLoggerConfiguration();
        EazyAppender appender = new EazyAppender(config);
        nut.Assert.assertNotSame(output, appender.getStream());
        appender.setStream(output);
        nut.Assert.assertSame(output, appender.getStream());
    }

}
