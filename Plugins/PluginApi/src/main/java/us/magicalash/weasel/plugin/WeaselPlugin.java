package us.magicalash.weasel.plugin;

import java.util.Properties;

/**
 * A plugin for the Weasel search system.
 */
public interface WeaselPlugin {
    /**
     * Gets the plugin name.
     * @return the plugin name.
     */
    String getName();

    /**
     * Lists the properties that this plugin requires.
     *
     * Standard properties are listed directly by their namespace.
     *
     * Array properties are listed by their namespace, ending with "[*]".
     * As such, an array with namespace foo.bar would be requested as "foo.bar[*]".
     * Note: <b>only a single level of arrays are allowed, and it must be the final namespace.</b>
     *
     * @return All properties the plugin requires to configure itself.
     */
    String[] requestProperties();

    /**
     * Loads the configuration for this plugin.
     * @param properties the configuration properties for this plugin
     */
    void load(Properties properties);
}
