package us.magicalash.weasel.index.configuration;

import org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import us.magicalash.weasel.index.plugin.PluginLoader;

@Configuration
@Import(RestClientAutoConfiguration.class)
public class IndexConfiguration {
    private final PluginLoader loader;

    public IndexConfiguration(PluginLoader loader) {
        this.loader = loader;
    }
}
