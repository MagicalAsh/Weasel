package us.magicalash.weasel.provider.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "weasel.provider.sendto")
public class SendingProperties {
    private String host = "localhost";
    private String port = "9909";
    private String endpoint = "/index/index";

    public String getAddress() {
        return "http://" + host + ":" + port + endpoint;
    }
}