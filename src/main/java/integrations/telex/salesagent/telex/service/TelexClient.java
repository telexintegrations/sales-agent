package integrations.telex.salesagent.telex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.config.AppConfig;
import integrations.telex.salesagent.telex.util.FormatTelexMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelexClient {
    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FormatTelexMessage formatTelexMessage;

    public void sendToTelexChannel(String message) {
        try {
            restTemplate.postForObject(appConfig.getTelexWebhookUrl() + appConfig.getTelexChannelId(), message, String.class);
            log.info("Sent message to Telex: {}", message);
        } catch (Exception e) {
            log.error("Failed to send message to Telex", e);
        }
    }


    public void processTelexPayload(String payload) throws JsonProcessingException {
        Map<String, Object> telexPayload = new HashMap<>();

        telexPayload.put("event_name", "new_lead");
        telexPayload.put("username", "sales_agent");
        telexPayload.put("status", "success");
        telexPayload.put("message", payload);

        sendToTelexChannel(objectMapper.writeValueAsString(telexPayload));
    }
}
