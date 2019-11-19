package us.magicalash.weasel.common;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import us.magicalash.weasel.common.dummyplugin.ITestPlugin;
import us.magicalash.weasel.common.dummyplugin.TestPlugin;
import us.magicalash.weasel.common.dummyplugin.TestPluginLoader;
import us.magicalash.weasel.plugin.PluginLoader;

import java.util.List;

import static org.junit.Assert.*;

@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class PluginLoaderTest {
    @Before
    public void before() {
    }

    // this test sets the loader or fails, at which point all other tests should fail
    @Test
    public void testLoader() {
        new TestPluginLoader();
    }

    @Test
    public void testPluginCount(){
        PluginLoader<ITestPlugin> loader;
        try {
            loader = new TestPluginLoader();
        } catch (Throwable t) {
            fail("Plugin loader failed to initialize. The test cannot be run.");
            return;
        }

        // it should pick up the plugin inside the test classpath, i.e. in dummyplugin
        assertEquals(1, loader.getLoadedPlugins().size());
    }

    @Test
    public void testApplicablePlugins() {
        PluginLoader<ITestPlugin> loader;
        try {
            loader = new TestPluginLoader();
        } catch (Throwable t) {
            fail("Plugin loader failed to initialize. The test cannot be run.");
            return;
        }

        List<ITestPlugin> plugins = loader.getApplicablePlugins(null);
        assertNotNull(plugins);
        assertEquals(1, plugins.size());
        assertEquals(TestPlugin.class, plugins.get(0).getClass());
    }
}
