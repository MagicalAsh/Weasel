package us.magicalash.weasel.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;
import us.magicalash.weasel.provider.configuration.SendingProperties;
import us.magicalash.weasel.provider.plugin.ProviderPluginLoader;
import us.magicalash.weasel.provider.web.WebRefreshController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebControllerTests {
    WebRefreshController controller;

    @Before
    public void before() {
        ProviderPluginLoader providerPluginLoader = mock(ProviderPluginLoader.class);
        RestTemplate template = mock(RestTemplate.class);
        SendingProperties properties = new SendingProperties();
        controller = new WebRefreshController(providerPluginLoader, template, properties);
        controller.setEnabled(true);

        // we don't want this returning null, so give it a mock
        when(providerPluginLoader.getLoadedPlugins()).thenReturn(new ArrayList<>());
    }


    @Test
    public void testWebRefreshFails() {
        controller.setEnabled(false);
        HttpServletResponse response = mock(HttpServletResponse.class);
        try {
            JsonObject object = controller.refresh("foobar", response);
            assertEquals(object.get("status").getAsString(), "failed");
            assertEquals(object.get("reason").getAsString(), "Web refresh disabled.");
            Mockito.verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        } finally {
            controller.setEnabled(true);
        }
    }

    @Test
    public void testWebRefreshReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonObject object = controller.refresh("foobar", response);
        assertEquals(new JsonArray(), object.get("processed_by"));
        assertEquals("success", object.get("status").getAsString());
    }
}
