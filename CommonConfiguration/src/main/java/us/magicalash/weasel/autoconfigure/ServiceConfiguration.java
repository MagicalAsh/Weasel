package us.magicalash.weasel.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;

/**
 * Common Configuration for all Weasel services.
 */
@Configuration
@PropertySource("classpath:common_config.properties")
public class ServiceConfiguration {
    private static Logger logger = LoggerFactory.getLogger(ServiceConfiguration.class);

    // We want to log when this gets instantiated, to make sure it is loaded.
    public ServiceConfiguration() {
        logger.info("Loaded Weasel service autoconfiguration.");
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate template() {
        return new RestTemplate();
    }


}
