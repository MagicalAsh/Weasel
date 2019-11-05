package us.magicalash.weasel.provider.scheduled;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import us.magicalash.weasel.provider.configuration.SendingProperties;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;
import us.magicalash.weasel.provider.plugin.ProviderPluginLoader;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Component that manages refreshing repositories on a schedule.
 */
@Component
public class ScheduledRefreshManager {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledRefreshManager.class);

    private final ProviderPluginLoader loader;
    private final RestTemplate template;
    private final SendingProperties properties;
    private final SchedulingProperties schedulingProperties;


    public ScheduledRefreshManager(ProviderPluginLoader loader, RestTemplate template, SendingProperties properties,
                                   SchedulingProperties schedulingProperties) {
        this.loader = loader;
        this.template = template;
        this.properties = properties;
        this.schedulingProperties = schedulingProperties;
    }

    @Scheduled(cron="#{schedulingProperties.cron}")
    public void refresh() {
        if(!schedulingProperties.isEnabled())
            return;

        logger.debug("Beginning scheduled refresh of all repositories.");

        for (String repository : schedulingProperties.getRepositories()) {
            for (ProviderPlugin plugin : loader.getApplicablePlugins(repository)) {
                JsonElement repo = plugin.refresh(repository);

                Map<String, String> queryParams = new LinkedHashMap<>();
                queryParams.put("provider", plugin.getName());

                for (JsonElement component : repo.getAsJsonArray()) {
                    ResponseEntity<JsonObject> response =
                            template.postForEntity(properties.getAddress(), component, JsonObject.class, queryParams);

                    if (response.getStatusCode().isError()) {
                        JsonObject body = response.getBody();
                        if (body != null)
                            logger.warn("Failed to refresh {} on schedule, caused by {}.", repository,
                                    body.get("reason").getAsString());
                        else
                            logger.warn("Failed to refresh {} on schedule, returned with status code {}", repository, response.getStatusCodeValue());
                    } else {
                        logger.debug("Refreshed repo {} on schedule.", repository);
                    }
                }
            }
        }

        logger.debug("Finished scheduled refresh of all repositories.");
    }
}
