package us.magicalash.weasel.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.springframework.web.client.RestTemplate;
import us.magicalash.weasel.plugin.PluginTaskService;
import us.magicalash.weasel.provider.configuration.SendingProperties;
import us.magicalash.weasel.provider.plugin.ProviderPluginLoader;
import us.magicalash.weasel.provider.web.WebRefreshController;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProviderWebControllerTests {
    WebRefreshController controller;

    @Before
    public void before() {
        ProviderPluginLoader providerPluginLoader = mock(ProviderPluginLoader.class);
        RestTemplate template = mock(RestTemplate.class);
        PluginTaskService taskService = mock(PluginTaskService.class);
        SendingProperties properties = new SendingProperties();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        controller = new WebRefreshController(providerPluginLoader, template, properties, taskService, gson);
        controller.setEnabled(true);

        // we don't want this returning null, so give it a mock
        when(providerPluginLoader.getLoadedPlugins()).thenReturn(new ArrayList<>());
    }


//    @Test
//    public void testWebRefreshFails() {
//        controller.setEnabled(false);
//        HttpServletResponse response = mock(HttpServletResponse.class);
//        try {
//            JsonObject object = controller.refresh("foobar", response);
//            assertEquals(object.get("status").getAsString(), "failed");
//            assertEquals(object.get("reason").getAsString(), "Web refresh disabled.");
//            Mockito.verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
//        } finally {
//            controller.setEnabled(true);
//        }
//    }
//
//    @Test
//    public void testWebRefreshReturns() {
//        HttpServletResponse response = mock(HttpServletResponse.class);
//        JsonObject object = controller.refresh("foobar", response);
//        assertEquals(new JsonArray(), object.get("scheduled_for"));
//    }
}
