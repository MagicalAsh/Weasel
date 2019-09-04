package us.magicalash.weasel.provider.configuration;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import us.magicalash.weasel.provider.plugin.PluginLoader;

@Configuration
public class ProviderConfiguration {
    @Autowired
    @Setter
    private PluginLoader loader;
}
