package us.magicalash.weasel.plugin;

import java.util.Properties;

public interface WeaselPlugin {
    /**
     * Gets the plugin name.
     * @return the plugin name.
     */
    String getName();

    /**
     * Lists the properties that this plugin requires.
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
