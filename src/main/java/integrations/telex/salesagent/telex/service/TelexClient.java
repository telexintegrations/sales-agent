package integrations.telex.salesagent.telex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.config.AppConfig;
import integrations.telex.salesagent.lead.entity.Lead;
import integrations.telex.salesagent.telex.util.FormatTelexMessage;
import integrations.telex.salesagent.user.dto.request.SalesAgentPayloadDTO;
import integrations.telex.salesagent.user.dto.request.TelexPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelexClient {
    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FormatTelexMessage formatTelexMessage;

    public void sendToTelexChannel(String channelID, String message) {
        try {
            String telexWebhook = appConfig.getTelexWebhookUrl() + channelID;

            restTemplate.postForObject(telexWebhook, message, String.class);
            log.info("Sent message to Telex channel: {} with message: {}", channelID, message);
        } catch (Exception e) {
            log.error("Failed to send message to Telex", e);
        }
    }

    public void processTelexPayload(String channelID, Lead lead) throws JsonProcessingException {
        String message = formatTelexMessage.formatNewLeadMessage(lead);

        TelexPayload telexPayload = new TelexPayload("New Lead Alert", "Sales Agent", "success", message);

        sendToTelexChannel(channelID, objectMapper.writeValueAsString(telexPayload));
    }

}
