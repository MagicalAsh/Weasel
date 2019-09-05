package us.magicalash.weasel.provider.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import us.magicalash.weasel.plugin.PluginLoader;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProviderPluginLoader extends PluginLoader<ProviderPlugin> {
    @Autowired
    public ProviderPluginLoader(Environment environment) {
        super(ProviderPlugin.class, environment);
    }

    public List<ProviderPlugin> getLoadedPluginsForRepo(String repoName) {
        return this.getLoadedPlugins().stream().filter(p -> p.canRefresh(repoName)).collect(Collectors.toList());
    }
}
