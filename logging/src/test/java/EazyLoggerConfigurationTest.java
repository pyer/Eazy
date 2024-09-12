
package ab.eazy.logging;

import java.util.Properties;
import org.slf4j.event.Level;

import nut.annotations.Test;
import static nut.Assert.*;

public class EazyLoggerConfigurationTest
{
    private static final Level DEFAULT_LEVEL = Level.INFO;

    @Test
    public void testConfig()
    {
        Properties props = new Properties();
        props.setProperty(EazyAppender.MESSAGE_ESCAPE_KEY, "false");
        props.setProperty(EazyAppender.NAME_CONDENSE_KEY, "false");
        props.setProperty(EazyAppender.MESSAGE_ALIGN_KEY, "10");
        props.setProperty("com.dummy.LEVEL", "WARN");
        props.setProperty("com.dummy.STACKS", "true");

        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        EazyAppender appender = new EazyAppender(config);

        assertFalse(appender.isEscapedMessages());
        assertFalse(appender.isCondensedNames());
        assertEquals(appender.getMessageAlignColumn(), 10);

        assertEquals(config.getLevel("com.dummy"), Level.WARN);
        assertTrue(config.getHideStacks("com.dummy"));
    }

    @Test
    public void testDefaultConfig()
    {
        Properties props = new Properties();
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        EazyAppender appender = new EazyAppender(config);

        assertTrue(appender.isEscapedMessages());
        assertTrue(appender.isCondensedNames());
        assertEquals(appender.getMessageAlignColumn(), 0);
        assertEquals(config.getLevel("com.dummy"), DEFAULT_LEVEL);
        assertFalse(config.getHideStacks("com.dummy"));
    }

    @Test
    public void testGetLevelExact()
    {
        Properties props = new Properties();
        props.setProperty("com.dummy.LEVEL", "WARN");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        Level level = config.getLevel("com.dummy");
        assertEquals(Level.WARN, level);
    }

    @Test
    public void testGetLevelDotEnd()
    {
        Properties props = new Properties();
        props.setProperty("com.dummy.LEVEL", "WARN");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        // extra trailing dot "."
        Level level = config.getLevel("com.dummy.");
        assertEquals(Level.WARN, level);
    }

    @Test
    public void testGetLevelWithLevel()
    {
        Properties props = new Properties();
        props.setProperty("com.dummy.LEVEL", "WARN");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        // asking for name with ".LEVEL"
        Level level = config.getLevel("com.dummy.LEVEL");
        assertEquals(Level.WARN, level);
    }

    @Test
    public void testGetLevelChild()
    {
        Properties props = new Properties();
        props.setProperty("com.dummy.LEVEL", "WARN");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        Level level = config.getLevel("com.dummy.Foo");
        assertEquals(Level.WARN, level);
    }

    @Test
    public void testGetLevelDefault()
    {
        Properties props = new Properties();
        props.setProperty("com.dummy.LEVEL", "WARN");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        // asking for name that isn't configured, returns default value
        Level level = config.getLevel("ab.eazy");
        assertEquals(DEFAULT_LEVEL, level);
    }

    @Test
    public void testGetHideStacksExact()
    {
        Properties props = new Properties();
        props.setProperty("com.dummy.STACKS", "true");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        boolean val = config.getHideStacks("com.dummy");
        assertTrue(val);
    }

    @Test
    public void testGetHideStacksDotEnd()
    {
        Properties props = new Properties();
        props.setProperty("com.dummy.STACKS", "true");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        // extra trailing dot "."
        boolean val = config.getHideStacks("com.dummy.");
        assertTrue(val);
    }

    @Test
    public void testGetHideStacksWithStacks()
    {
        Properties props = new Properties();
        props.setProperty("com.dummy.STACKS", "true");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        // asking for name with ".STACKS"
        boolean val = config.getHideStacks("com.dummy.Bar.STACKS");
        assertTrue(val);
    }

    @Test
    public void testGetHideStacksChild()
    {
        Properties props = new Properties();
        props.setProperty("com.dummy.STACKS", "true");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        boolean val = config.getHideStacks("com.dummy.Foo");
        assertTrue(val);
    }

    @Test
    public void testGetHideStacksDefault()
    {
        Properties props = new Properties();
        props.setProperty("com.dummy.STACKS", "true");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);
        // asking for name that isn't configured, returns default value
        boolean val = config.getHideStacks("ab.eazy");
        assertFalse(val);
    }

    @Test
    public void testGetLoggingLevelBad()
    {
        Properties props = new Properties();
        props.setProperty("ab.eazy.bad.LEVEL", "EXPECTED_BAD_LEVEL");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);

        // Default Level (because of bad level value)
        assertEquals(DEFAULT_LEVEL, config.getLevel("ab.eazy.bad"));
    }

    @Test
    public void testGetLoggingLevelUppercase()
    {
        Properties props = new Properties();
        props.setProperty("ab.eazy.util.LEVEL", "DEBUG");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);

        // Default Level
        assertEquals(DEFAULT_LEVEL, config.getLevel("ab.eazy"));
        // Specific Level
        assertEquals(Level.DEBUG, config.getLevel("ab.eazy.util"));
    }

    @Test
    public void testGetLoggingLevelLowercase()
    {
        Properties props = new Properties();
        props.setProperty("ab.eazy.util.LEVEL", "debug");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);

        // Default Level
        assertEquals(DEFAULT_LEVEL, config.getLevel("ab.eazy"));
        // Specific Level is default because LEVEL is case sensitive
        assertEquals(DEFAULT_LEVEL, config.getLevel("ab.eazy.util"));
    }

    @Test
    public void testGetLoggingLevelFQCN()
    {
        String name = EazyLoggerConfiguration.class.getName();
        Properties props = new Properties();
        props.setProperty(name + ".LEVEL", "TRACE");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);

        // Default Levels
        assertEquals(Level.INFO, config.getLevel(null));
        assertEquals(Level.INFO, config.getLevel(""));
        assertEquals(Level.INFO, config.getLevel("ab.eazy"));

        // Specified Level
        assertEquals(Level.TRACE, config.getLevel(name));
    }

    @Test
    public void testGetLoggingLevelUtilLevel()
    {
        Properties props = new Properties();
        props.setProperty("ab.eazy.util.LEVEL", "DEBUG");
        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);

        // Default Levels
        assertEquals(config.getLevel(null), DEFAULT_LEVEL);
        assertEquals(config.getLevel(""), DEFAULT_LEVEL);
        assertEquals(config.getLevel("ab.eazy"), DEFAULT_LEVEL);
        assertEquals(config.getLevel("ab.eazy.server.BogusObject"), DEFAULT_LEVEL);
        assertEquals(config.getLevel(EazyLoggerConfigurationTest.class.getName()), DEFAULT_LEVEL);

        // Configured Level
        assertEquals(config.getLevel("ab.eazy.util"), Level.DEBUG);
        assertEquals(config.getLevel("ab.eazy.util.Bogus"), Level.DEBUG);
        assertEquals(config.getLevel("ab.eazy.util.resource.PathResource"), Level.DEBUG);
    }

    @Test
    public void testGetLoggingLevelMixedLevels()
    {
        Properties props = new Properties();
        props.setProperty("ab.eazy.util.LEVEL", "WARN");
        props.setProperty("ab.eazy.util.ConcurrentHashMap.LEVEL", "TRACE");

        EazyLoggerConfiguration config = new EazyLoggerConfiguration(props);

        // Default Levels
        assertEquals(config.getLevel(null), DEFAULT_LEVEL);
        assertEquals(config.getLevel(""), DEFAULT_LEVEL);
        assertEquals(config.getLevel("ab.eazy"), DEFAULT_LEVEL);
        assertEquals(config.getLevel("ab.eazy.server.BogusObject"), DEFAULT_LEVEL);
        assertEquals(config.getLevel(EazyLoggerConfigurationTest.class.getName()), DEFAULT_LEVEL);

        // Configured Level
        assertEquals(Level.WARN, config.getLevel("ab.eazy.util.MagicUtil"));
        assertEquals(Level.WARN, config.getLevel("ab.eazy.util"));
        assertEquals(Level.WARN, config.getLevel("ab.eazy.util.resource.PathResource"));

        assertEquals(Level.TRACE, config.getLevel("ab.eazy.util.ConcurrentHashMap"));
    }
}
