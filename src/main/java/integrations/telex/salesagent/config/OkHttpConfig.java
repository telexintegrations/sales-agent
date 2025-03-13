package integrations.telex.salesagent.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OkHttpConfig {

    @Value("${spring.hunter.api-key}")
    private String apiKey;

    @Value("${spring.hunter.url}")
    private String baseUrl;


    @Bean
    public HunterConnectionParams hunterParams() {
        return new HunterConnectionParams(apiKey, baseUrl);
    }

    @Bean
    public OkHttpClient okHttpClient(){
        return new OkHttpClient();
    }

    @AllArgsConstructor
    @Getter
    public static class HunterConnectionParams {
        private final String apikey;
        private final String baseUrl;
    }

}
