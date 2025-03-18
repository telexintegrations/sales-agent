package integrations.telex.salesagent.telex.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelexService {
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    private final static String TELEX_JSON_CONFIG = "integration.json";

    public Map<String, Object> getTelexJsonConfig() {
        try {
            InputStream inputStream = new ClassPathResource(TELEX_JSON_CONFIG).getInputStream();
            return objectMapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Failed to read telex json config", e);
        }
    }

    
}
