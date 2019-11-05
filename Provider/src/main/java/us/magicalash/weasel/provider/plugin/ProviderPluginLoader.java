package us.magicalash.weasel.provider.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import us.magicalash.weasel.plugin.PluginLoader;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Plugin loader for ProviderPlugin instances.
 */
@Component
public class ProviderPluginLoader extends PluginLoader<ProviderPlugin> {
    @Autowired
    public ProviderPluginLoader(Environment environment) {
        super(ProviderPlugin.class, environment);
    }

    @Override
    public List<ProviderPlugin> getApplicablePlugins(Object testing) {
        if (testing instanceof String) {
            String repoName = (String) testing;
            return this.getLoadedPlugins().stream().filter(p -> p.canRefresh(repoName)).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
