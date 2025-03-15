package integrations.telex.salesagent.config;

import integrations.telex.salesagent.telex.util.FormatTelexMessage;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class AppConfig {
    private final String telexChannelId = "019590a4-b83d-7846-8876-9beb5a5bcffb";

    @Value("${spring.telex.webhook.url:}")
    private String telexWebhookUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public FormatTelexMessage messageFormatter() {
        return new FormatTelexMessage();
    }
}
