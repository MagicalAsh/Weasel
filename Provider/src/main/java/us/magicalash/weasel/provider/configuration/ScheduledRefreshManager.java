package us.magicalash.weasel.provider.configuration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import us.magicalash.weasel.provider.plugin.PluginLoader;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Component that manages refreshing repositories on a schedule.
 */
@Component
public class ScheduledRefreshManager {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledRefreshManager.class);

    /**
     * Determines whether component is enabled.
     */
    @Value("${weasel.provider.refresh.scheduled.enabled}")
    private boolean enabled;

    /**
     * A list of repositories that are to be refreshed on a schedule.
     */
    @Value("${weasel.provider.refresh.scheduled.repositories}")
    private List<String> repositories;

    private final PluginLoader loader;
    private final RestTemplate template;
    private final SendingProperties properties;


    public ScheduledRefreshManager(PluginLoader loader, RestTemplate template, SendingProperties properties) {
        this.loader = loader;
        this.template = template;
        this.properties = properties;
    }

    @Scheduled(cron="${weasel.provider.refresh.scheduled.cron}")
    public void refresh() {
        if(!enabled)
            return;

        logger.debug("Beginning scheduled refresh of all repositories.");

        for (String repository : repositories) {
            for (ProviderPlugin plugin : loader.getLoadedPluginsForRepo(repository)) {
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
