package us.magicalash.weasel.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Loads plugins from a directory that conform to the Weasel Plugin interface.
 *
 * Plugins will be loaded through the ServiceLoader class. As such, each plugin should
 * be wrapped as a jar with the appropriate services files for each plugin.
 *
 * Note that the dependencies for plugins should be placed along side the plugin, in order
 * to be loaded under the proper classloader.
 *
 * @param <T> The type of plugin to load.
 */
public abstract class PluginLoader<T extends WeaselPlugin> {
    private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);
    private static final String PLUGIN_FOLDER_PROPERTY = "weasel.provider.plugin.location";
    private static final String DEFAULT_PLUGIN_FOLDER = "plugins/";

    private ServiceLoader<T> loader;
    private Class<T> clazz;
    private boolean hasLoaded;

    /**
     * Setting special requests before calling loadPlugin() allows you to request special actions
     * when a specific property is found, such as obtaining a Spring Bean or requesting information
     * from the database. The key is a regular expression matching the requested key, while the
     * value is a function taking the plugin and matching key, and returning the specified object.
     */
    protected Map<String, BiFunction<T, String, Object>> specialRequestProperties;

    private List<T> loadedPlugins;
    private Environment environment;


    /**
     * Creates a plugin loader for the given class.
     * @param clazz         the class to load plugins for
     * @param environment   the environment to pull configuration options from
     */
    public PluginLoader(Class<T> clazz, Environment environment) {
        this.specialRequestProperties = new HashMap<>();
        this.clazz = clazz;
        this.hasLoaded = false;
        this.environment = environment;
    }

    @PostConstruct
    public void loadPlugins() {
        String pluginFolder = environment.getProperty(PLUGIN_FOLDER_PROPERTY);
        if (pluginFolder == null) {
            pluginFolder = DEFAULT_PLUGIN_FOLDER;
        }


        URLClassLoader classLoader = null;
        File folder = new File(pluginFolder);
        File[] plugins = folder.listFiles();
        if (plugins == null) {
            throw new IllegalStateException("Failed to load plugins: configured location is not a folder");
        }

        try {
            URL[] pluginUrls = new URL[plugins.length];
            for (int i = 0; i < plugins.length; i++) {
                pluginUrls[i] = plugins[i].toURI().toURL();
            }

            classLoader = new URLClassLoader(pluginUrls, this.getClass().getClassLoader());
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to load plugins", e);
        }

        loader = ServiceLoader.load(clazz, classLoader);
        loadProperties();
    }

    /**
     * Loads the configurations for all of the plugins that
     * the service loader found.
     */
    private void loadProperties() {
        this.loadedPlugins = new ArrayList<>();
        for (T plugin : loader) {
            Properties properties = new Properties();
            for (String property : plugin.requestProperties()) {
                // copy properties from spring into a properties object, because Spring can't.
                Object value = null;

                for (String pattern : specialRequestProperties.keySet()) {
                    if (Pattern.matches(pattern, property)){
                        value = specialRequestProperties.get(pattern).apply(plugin, property);
                    }
                }

                if (value == null) {
                    if (property.endsWith("[*]")) {
                        value = toList(property.replace("[*]", ""));
                    } else {
                        value = environment.getProperty(property);
                    }
                }

                properties.put(property, value == null ? "" : value);
            }
            plugin.load(properties);

            this.loadedPlugins.add(plugin);
        }

        logger.info("Loaded {} plugins.", loadedPlugins.size());
        hasLoaded = true;
    }

    /**
     * Converts a property into a list. The list element <i>must</i> be the
     * last property namespace.
     *
     * @param property property name to convert to a list
     * @return         a list of the properties
     */
    private List<String> toList(String property) {
        List<String> list = new ArrayList<>();
        int i = 0;
        boolean shouldContinue = true;

        while (shouldContinue) {
            String propertyName = property + "[" + i + "]";
            String value = environment.getProperty(propertyName);
            if (value != null) {
                list.add(value);
                i++;
            } else {
                shouldContinue = false;
            }
        }

        return list;
    }

    /**
     * Gets  all of the currently loaded plugins
     * @return the currently loaded plugins, as an unmodified list.
     */
    public List<T> getLoadedPlugins() {
        if (!hasLoaded) {
            loadPlugins();
        }

        return Collections.unmodifiableList(this.loadedPlugins);
    }

    public abstract List<T> getApplicablePlugins(Object obj);
}