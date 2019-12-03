package us.magicalash.weasel.provider.web;

import com.google.gson.JsonObject;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import us.magicalash.weasel.plugin.PluginTask;
import us.magicalash.weasel.plugin.PluginTaskService;
import us.magicalash.weasel.provider.configuration.SendingProperties;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;
import us.magicalash.weasel.provider.plugin.ProviderPluginLoader;
import us.magicalash.weasel.provider.representation.ProviderResponse;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
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
    private final PluginTaskService taskService;

    public WebRefreshController(ProviderPluginLoader pluginLoader, RestTemplate template,
                                SendingProperties sendingProperties, PluginTaskService taskService) {
        this.pluginLoader = pluginLoader;
        this.template = template;
        this.sendingProperties = sendingProperties;
        this.taskService = taskService;
    }

    @GetMapping("/refresh/{repoName}")
    public ProviderResponse refresh(@PathVariable String repoName, HttpServletResponse servletResponse) {
        ProviderResponse response = new ProviderResponse();
        if(!enabled) {
            response.getMetadata().setStatus("failed");
            response.getMetadata().setMessage("Web refresh disabled.");
            response.getMetadata().setResponseCode(HttpServletResponse.SC_FORBIDDEN);
            return response;
        }

        List<ProviderPlugin> plugins = pluginLoader.getApplicablePlugins(repoName);
        List<String> processed = new ArrayList<>();

        for (ProviderPlugin plugin : plugins) {
            taskService.submit(
                PluginTask.builder()
                    .pluginName(plugin.getName())
                    .task(() -> {
                        plugin.refresh(repoName, e -> send(e.toString()));
                        return null;
                    })
                    .build()
            );

            processed.add(plugin.getName());
        }

        response.setBy(processed);
        return response;
    }

    @PostMapping("/refresh")
    public ProviderResponse refresh(@RequestBody JsonObject body, HttpServletResponse servletResponse) {
        return refresh(body.get("repo").getAsString(), servletResponse);
    }

    private Object send(String output) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(output, headers);

        try {
            ResponseEntity<?> e = template.exchange(sendingProperties.getAddress(), HttpMethod.POST, request, String.class);
            return e.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
