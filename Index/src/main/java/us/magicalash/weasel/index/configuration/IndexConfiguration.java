package us.magicalash.weasel.index.configuration;

import org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import us.magicalash.weasel.index.plugin.IndexPluginLoader;

@Configuration
@Import(RestClientAutoConfiguration.class)
public class IndexConfiguration {
    private final IndexPluginLoader loader;

    public IndexConfiguration(IndexPluginLoader loader) {
        this.loader = loader;
    }
}
