package us.magicalash.weasel.index.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@Component
public class PluginLoader {
    private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);

    private static final String PLUGIN_FOLDER_PROPERTY = "weasel.provider.plugin.location";
    private static final String DEFAULT_PLUGIN_FOLDER = "plugins/";

    private ServiceLoader<IndexPlugin> loader;

    private List<IndexPlugin> loadedPlugins;
    private Environment environment;


    @Autowired
    public PluginLoader(Environment environment) {
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

            classLoader = new URLClassLoader(pluginUrls);
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to load plugins", e);
        }

        loader = ServiceLoader.load(IndexPlugin.class, classLoader);

        this.environment = environment;
        loadPlugins();
    }

    private synchronized void loadPlugins() {
        this.loadedPlugins = new ArrayList<>();
        for (IndexPlugin plugin : loader){
            Properties properties = new Properties();
            for (String property : plugin.requestProperties()) {
                // copy properties from spring into a properties object, because Spring can't.
                properties.put(property, environment.getProperty(property));
            }
            plugin.load(properties);

            logger.info("Loaded plugin: {}", plugin.getName());
            this.loadedPlugins.add(plugin);
        }
    }

    public List<IndexPlugin> getLoadedPlugins() {
        return Collections.unmodifiableList(this.loadedPlugins);
    }
}
