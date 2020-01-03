package us.magicalash.weasel.index;

import com.google.gson.JsonObject;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.concurrent.ListenableFuture;
import us.magicalash.weasel.index.plugin.IndexPlugin;
import us.magicalash.weasel.index.plugin.IndexPluginLoader;
import us.magicalash.weasel.index.representation.ParsedIndexResponse;
import us.magicalash.weasel.index.web.WebIndexController;
import us.magicalash.weasel.plugin.PluginTask;
import us.magicalash.weasel.plugin.PluginTaskService;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static us.magicalash.weasel.index.plugin.IndexPlugin.DESTINATION;
import static us.magicalash.weasel.index.plugin.IndexPlugin.SOURCE_ID;

public class IndexWebControllerTests {
    private IndexPlugin plugin;
    private WebIndexController controller;
    private RestHighLevelClient client;
    private IndexPluginLoader loader;
    private PluginTaskService taskService;

    @Before
    public void before() {
        // WARNING: this rest client *MIGHT NOT BE* a full mock!
        // You need to be using a recent version of Mockito which
        // supports mocking final classes. The dependencies by default
        // should pull this in, but if you see errors regarding the
        // RestHighLevelClient, you may need to update Mockito.
        // See https://github.com/elastic/elasticsearch/issues/40534
        client = mock(RestHighLevelClient.class);
        loader = mock(IndexPluginLoader.class);
        taskService = mock(PluginTaskService.class);
        plugin = mock(IndexPlugin.class);

        controller = new WebIndexController(client, loader, taskService);

        when(plugin.canIndex(any())).thenReturn(true);
        when(loader.getLoadedPlugins()).thenReturn(Collections.singletonList(plugin));
        when(loader.getApplicablePlugins(any())).thenReturn(Collections.singletonList(plugin));
        when(loader.getApplicablePlugins(any())).thenCallRealMethod();

        // NOTE: this *WILL* fail if Mockito is not running a version supporting
        // final class mocking.
        when(taskService.submit(any())).then((invocation) -> {
            ((PluginTask) invocation.getArgument(0)).getTask().call();
            return mock(ListenableFuture.class);
        });

    }

    @Test
    public void testJobs() {
        JsonObject object = new JsonObject();
        object.addProperty(SOURCE_ID, "a");
        object.addProperty(DESTINATION, "a");

        when(plugin.index(any())).thenReturn(object);
        controller.index(object);

        verify(taskService, times(1)).submit(any());

        verify(plugin, times(1)).canIndex(any());
    }

    @Test
    public void testDryRun() {
        ParsedIndexResponse response = controller.dryRun(new JsonObject());

        assertEquals("success", response.getMetadata().getStatus());
        assertNotNull(response.getParsedResults());
        assertNotEquals(0, response.getProcessedBy().size());
    }
}
