package ab.eazy.logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import org.slf4j.event.Level;

/**
 * EazyLogger specific configuration:
 * <ul>
 *  <li>{@code <name>.LEVEL=(String:LevelName)}</li>
 *  <li>{@code <name>.STACKS=(boolean)}</li>
 * </ul>
 */
public class EazyLoggerConfiguration
{
    private static final Level DEFAULT_LEVEL = Level.INFO;
    private static final boolean DEFAULT_HIDE_STACKS = false;
    private static final String SUFFIX_LEVEL = ".LEVEL";
    private static final String SUFFIX_STACKS = ".STACKS";
    private static final String DEFAULT_PROPERTIES_FILE = "logging.properties";

    private final Properties properties = new Properties();

    /**
     * Default EazyLogger configuration (empty)
     * or the content of the file logging.properties if it exists
     */
    public EazyLoggerConfiguration()
    {
        try (InputStream input = new FileInputStream(DEFAULT_PROPERTIES_FILE)) {
            // load the properties file
            properties.load(input);
        } catch (IOException ex) {
            System.err.printf("[INFO] File '%s' not found.\n", DEFAULT_PROPERTIES_FILE);
        }
    }

    /**
     * EazyLogger configuration from provided Properties
     *
     * @param props A set of properties to base this configuration off of
     */
    public EazyLoggerConfiguration(Properties props)
    {
        if (props != null) {
            for (String name : props.stringPropertyNames()) {
                String val = props.getProperty(name);
                // Protect against application code insertion of non-String values (returned as null).
                if (val != null)
                    properties.setProperty(name, val);
            }
        }
    }


    public boolean getHideStacks(String name)
    {
        if (properties.isEmpty())
            return DEFAULT_HIDE_STACKS;

        String startName = name;

        // strip trailing dot
        while (startName.endsWith("."))
        {
            startName = startName.substring(0, startName.length() - 1);
        }

        // strip ".STACKS" suffix (if present)
        if (startName.endsWith(SUFFIX_STACKS))
            startName = startName.substring(0, startName.length() - SUFFIX_STACKS.length());

        Boolean hideStacks = EazyLoggerFactory.walkParentLoggerNames(startName, key ->
        {
            String stacksBool = properties.getProperty(key + SUFFIX_STACKS);
            if (stacksBool != null)
                return Boolean.parseBoolean(stacksBool);
            return null;
        });

        return hideStacks != null ? hideStacks : DEFAULT_HIDE_STACKS;
    }

    /**
     * <p>Returns the Logging Level for the provided log name.</p>
     * <p>Uses the FQCN first, then each package segment from longest to shortest.</p>
     *
     * @param name the name to get log for
     * @return the logging level
     */
    public Level getLevel(String name)
    {
        if (properties.isEmpty())
            return DEFAULT_LEVEL;

        String startName = name != null ? name : "";

        // Strip trailing dot.
        while (startName.endsWith("."))
        {
            startName = startName.substring(0, startName.length() - 1);
        }

        // Strip ".LEVEL" suffix (if present).
        if (startName.endsWith(SUFFIX_LEVEL))
            startName = startName.substring(0, startName.length() - SUFFIX_LEVEL.length());

        Level level = EazyLoggerFactory.walkParentLoggerNames(startName, key ->
        {
            String levelStr = properties.getProperty(key + SUFFIX_LEVEL);
            return strToLevel(levelStr);
        });

        return level != null ? level : DEFAULT_LEVEL;
    }

    static Level strToLevel(String levelStr)
    {
        if (levelStr == null) {
            return DEFAULT_LEVEL;
        }

        for (Level level : Level.values()) {
            if (level.name().equals(levelStr))
                return level;
        }

        System.err.printf("Unknown Logger/SLF4J Level [%s], expecting only %s as values.\n",
                levelStr, Arrays.toString(Level.values()));
        return DEFAULT_LEVEL;
    }

    public TimeZone getTimeZone(String key)
    {
        String zoneIdStr = properties.getProperty(key);
        if (zoneIdStr == null)
            return null;
        return TimeZone.getTimeZone(zoneIdStr);
    }


    public String getString(String key, String defValue)
    {
        return properties.getProperty(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue)
    {
        String val = properties.getProperty(key, Boolean.toString(defValue));
        return Boolean.parseBoolean(val);
    }

    public int getInt(String key, int defValue)
    {
        String val = properties.getProperty(key, Integer.toString(defValue));
        if (val == null)
            return defValue;
        try
        {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e)
        {
            return defValue;
        }
    }

}
