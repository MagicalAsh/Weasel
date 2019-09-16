package us.magicalash.weasel.provider.scheduled;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduling properties, to be used to determine when to refresh code based on a schedule.
 */
@Data
@Component
@ConfigurationProperties("weasel.provider.refresh.scheduled")
public class SchedulingProperties {
    private String cron;
    private boolean enabled;
    private List<String> repositories;
}
