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
    @Value("${telex.webhook.url}")
    private String telexWebhookUrl;

    @Value("${sandbox.baseURL}")
    private String sandboxBaseURL;

    @Value("${production.baseURL}")
    private String productionBaseURL;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
