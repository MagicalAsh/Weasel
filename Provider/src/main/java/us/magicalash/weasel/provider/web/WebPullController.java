package us.magicalash.weasel.provider.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;
import us.magicalash.weasel.provider.plugin.ProviderPluginLoader;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/pull")
public class WebPullController {
    /**
     * Web Refreshing enabled
     */
    @Setter
    @Value("${weasel.provider.pull.enabled}")
    private boolean enabled;

    private final ProviderPluginLoader pluginLoader;

    public WebPullController(ProviderPluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
    }

    @GetMapping("/pull/{repoName}")
    public JsonObject refresh(@PathVariable String repoName, HttpServletResponse servletResponse) {
        JsonObject response = new JsonObject();
        if(!enabled) {
            response.addProperty("status", "failed");
            response.addProperty("reason", "Web pull disabled.");
            servletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return response;
        }

        List<ProviderPlugin> plugins = pluginLoader.getApplicablePlugins(repoName);

        JsonArray pluginResponses = new JsonArray();
        for (ProviderPlugin plugin : plugins) {
            JsonObject provider = new JsonObject();
            JsonArray output = plugin.refresh(repoName);

            provider.addProperty("processed_by", plugin.getName());
            provider.add("result", output);
            pluginResponses.add(provider);
        }

        response.addProperty("status", "success");
        response.add("results", pluginResponses);
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
}
