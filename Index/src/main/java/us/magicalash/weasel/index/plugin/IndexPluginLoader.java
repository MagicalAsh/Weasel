package us.magicalash.weasel.index.plugin;

import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import us.magicalash.weasel.plugin.PluginLoader;

import java.util.List;
import java.util.stream.Collectors;


@Component
public class IndexPluginLoader extends PluginLoader<IndexPlugin> {

    @Autowired
    public IndexPluginLoader(Environment environment) {
        super(IndexPlugin.class, environment);
    }

    public List<IndexPlugin> getCapableLoadedPlugins(JsonObject indexable) {
        return this.getLoadedPlugins().stream().filter(p -> p.canIndex(indexable)).collect(Collectors.toList());
    }
}
