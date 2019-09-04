package us.magicalash.weasel.provider.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import us.magicalash.weasel.provider.configuration.SendingProperties;
import us.magicalash.weasel.provider.plugin.PluginLoader;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;

import java.nio.charset.Charset;
import java.util.List;

@RestController
@RequestMapping("/provider")
public class WebRefreshController {
    /**
     * Web Refreshing enabled
     */
    @Value("${weasel.provider.refresh.web.enabled}")
    private boolean enabled;

    private final PluginLoader pluginLoader;
    private final RestTemplate template;
    private final SendingProperties sendingProperties;

    public WebRefreshController(PluginLoader pluginLoader, RestTemplate template, SendingProperties sendingProperties) {
        this.pluginLoader = pluginLoader;
        this.template = template;
        this.sendingProperties = sendingProperties;
    }

    @GetMapping("/refresh/{repoName}")
    public JsonObject refresh(@PathVariable String repoName) {
        JsonObject response = new JsonObject();
        if(!enabled) {
            response.addProperty("status", "failed");
            response.addProperty("reason", "Web refresh disabled.");
            throw HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Web Refresh Disabled",
                    new HttpHeaders(), response.toString().getBytes(), Charset.defaultCharset());
        }

        List<ProviderPlugin> plugins = pluginLoader.getLoadedPluginsForRepo(repoName);

        for (ProviderPlugin plugin : plugins) {
            JsonElement output = plugin.refresh(repoName);

            ResponseEntity<String> indexResponse =
                    template.postForEntity(sendingProperties.getAddress(), output, String.class);
        }
        return response;
    }
}
