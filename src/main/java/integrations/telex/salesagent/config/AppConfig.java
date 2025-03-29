package integrations.telex.salesagent.config;

import integrations.telex.salesagent.user.service.OpenAIService;
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

    @Value("${google.api-key}")
    private String googleApiKey;

    @Value("${openai.api-key}")
    private String openaiApiKey;

    @Value("${google.custom.search.url}")
    private String googleUrl;

    @Value("${google.search.engine.id}")
    private String googleSearchEngineId;

    @Value("${openai.api.url}")
    private String openAiApiUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
