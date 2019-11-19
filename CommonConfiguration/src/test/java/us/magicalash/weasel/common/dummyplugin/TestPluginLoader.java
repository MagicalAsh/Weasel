package us.magicalash.weasel.common.dummyplugin;

import org.springframework.core.env.Environment;
import us.magicalash.weasel.plugin.PluginLoader;

import java.util.List;

import static org.mockito.Mockito.mock;

public class TestPluginLoader extends PluginLoader<ITestPlugin> {
    public TestPluginLoader() {
        super(ITestPlugin.class, mock(Environment.class));
    }

    @Override
    public List<ITestPlugin> getApplicablePlugins(Object obj) {
        return super.getLoadedPlugins();
    }
}