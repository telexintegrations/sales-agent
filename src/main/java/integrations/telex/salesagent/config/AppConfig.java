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
    @Value("${spring.telex.channel.id}")
    private String telexChannelId;

    @Value("${spring.telex.webhook.url}")
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
