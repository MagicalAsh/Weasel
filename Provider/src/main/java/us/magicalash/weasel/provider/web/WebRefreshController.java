package us.magicalash.weasel.provider.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import us.magicalash.weasel.provider.configuration.SendingProperties;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;
import us.magicalash.weasel.provider.plugin.ProviderPluginLoader;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/provider")
public class WebRefreshController {
    /**
     * Web Refreshing enabled
     */
    @Setter
    @Value("${weasel.provider.refresh.web.enabled}")
    private boolean enabled;

    private final ProviderPluginLoader pluginLoader;
    private final RestTemplate template;
    private final SendingProperties sendingProperties;

    public WebRefreshController(ProviderPluginLoader pluginLoader, RestTemplate template, SendingProperties sendingProperties) {
        this.pluginLoader = pluginLoader;
        this.template = template;
        this.sendingProperties = sendingProperties;
    }

    @GetMapping("/refresh/{repoName}")
    public JsonObject refresh(@PathVariable String repoName, HttpServletResponse servletResponse) {
        JsonObject response = new JsonObject();
        if(!enabled) {
            response.addProperty("status", "failed");
            response.addProperty("reason", "Web refresh disabled.");
            servletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return response;
        }

        List<ProviderPlugin> plugins = pluginLoader.getLoadedPluginsForRepo(repoName);

        JsonArray processed = new JsonArray();

        for (ProviderPlugin plugin : plugins) {
            JsonElement output = plugin.refresh(repoName);
            processed.add(plugin.getName());

            if(output.isJsonArray()) {
                for(JsonElement e : output.getAsJsonArray()) {
                    send(e.toString());
                }
            } else {
                send(output.toString());
            }
        }

        response.addProperty("status", "success");
        response.add("processed_by", processed);
        return response;
    }

    @PostMapping("/refresh")
    public JsonArray refresh(@RequestBody JsonArray body, HttpServletResponse servletResponse) {
        JsonArray response = new JsonArray();
        for (JsonElement element : body) {
            response.add(refresh(element.getAsString(), servletResponse));
        }

        return response;
    }

    private void send(String output) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity request = new HttpEntity<>(output, headers);

        try {
            template.exchange(sendingProperties.getAddress(), HttpMethod.POST, request, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
