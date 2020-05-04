package us.magicalash.weasel.provider.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;
import us.magicalash.weasel.provider.plugin.ProviderPluginLoader;
import us.magicalash.weasel.provider.plugin.representations.ProvidedFile;
import us.magicalash.weasel.provider.representation.ProvidedRepository;
import us.magicalash.weasel.provider.representation.ProviderResponse;
import us.magicalash.weasel.provider.representation.PullResponse;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/pull")
public class WebPullController {
    /**
     * Web Refreshing enabled
     */
    @Setter
    @Value("#{${weasel.provider.pull.enabled}||false}")
    private boolean enabled;

    private final ProviderPluginLoader pluginLoader;

    public WebPullController(ProviderPluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
    }

    @GetMapping("/pull/{repoName}")
    public ProviderResponse refresh(@PathVariable String repoName, HttpServletResponse servletResponse) {
        PullResponse response = new PullResponse();
        if(!enabled) {
            response.getMetadata().setMessage("Web pull disabled.");
            response.getMetadata().setResponseCode(HttpServletResponse.SC_FORBIDDEN);
            return response;
        }

        List<ProviderPlugin> plugins = pluginLoader.getApplicablePlugins(repoName);

        List<ProvidedRepository> pluginResponses = new ArrayList<>();
        for (ProviderPlugin plugin : plugins) {
            ProvidedRepository repo = new ProvidedRepository();
            List<ProvidedFile> output = plugin.refresh(repoName);

            repo.setProvided(output);
            repo.setProvidedBy(plugin.getName());

            pluginResponses.add(repo);
        }

        response.setFiles(pluginResponses);
        return response;
    }

    @PostMapping("/refresh")
    public ProviderResponse refresh(@RequestBody JsonObject body, HttpServletResponse servletResponse) {
        return refresh(body.get("repo").getAsString(), servletResponse);
    }

}
