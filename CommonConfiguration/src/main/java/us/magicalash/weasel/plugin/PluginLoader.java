package us.magicalash.weasel.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class PluginLoader<T extends WeaselPlugin> {
    private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);
    private static final String PLUGIN_FOLDER_PROPERTY = "weasel.provider.plugin.location";
    private static final String DEFAULT_PLUGIN_FOLDER = "plugins/";

    private ServiceLoader<T> loader;

    private List<T> loadedPlugins;
    private Environment environment;


    public PluginLoader(Class<T> clazz, Environment environment) {
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
                pluginUrls[i] = plugins[i].toURL();
            }

            classLoader = new URLClassLoader(pluginUrls, this.getClass().getClassLoader());
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to load plugins", e);
        }

        loader = ServiceLoader.load(clazz, classLoader);

        this.environment = environment;
        loadPlugins();
    }

    private void loadPlugins() {
        this.loadedPlugins = new ArrayList<>();
        for (T plugin : loader) {
            Properties properties = new Properties();
            for (String property : plugin.requestProperties()) {
                // copy properties from spring into a properties object, because Spring can't.
                properties.put(property, environment.getProperty(property));
            }
            plugin.load(properties);

            this.loadedPlugins.add(plugin);
        }

        logger.info("Loaded {} plugins.", loadedPlugins.size());
    }

    public List<T> getLoadedPlugins() {
        return Collections.unmodifiableList(this.loadedPlugins);
    }
}