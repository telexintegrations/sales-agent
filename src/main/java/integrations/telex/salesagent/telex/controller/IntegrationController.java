package integrations.telex.salesagent.telex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/integration.json")
@RequiredArgsConstructor
public class IntegrationController {
    private final ObjectMapper objectMapper;

    @GetMapping
    public Map<String, Object> getIntegration() {
        try {
            InputStream inputStream = new ClassPathResource("integration.json").getInputStream();
            return objectMapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
            log.error("Failed to read integration.json", e);
            return null;
        }
    }
}
