package integrations.telex.salesagent.telex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.config.AppConfig;
import integrations.telex.salesagent.lead.dto.RapidLeadDto;
import integrations.telex.salesagent.lead.model.Lead;
import integrations.telex.salesagent.telex.util.FormatTelexMessage;
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

    public void processTelexPayload(String channelID, RapidLeadDto lead) throws JsonProcessingException {
        String message = formatTelexMessage.formatNewLeadMessage(lead) + "\n\nSales Agent Bot";

        TelexPayload telexPayload = new TelexPayload("New Lead Alert", "Sales Agent", "success", message);

        sendToTelexChannel(channelID, objectMapper.writeValueAsString(telexPayload));
    }

    public void sendInstruction(String channelId, String instruction) throws JsonProcessingException {
        String signedMessage = instruction + "\n\nSales Agent Bot";
        TelexPayload telexPayload = new TelexPayload("KYC", "Sales Agent Bot", "success", signedMessage);
        sendToTelexChannel(channelId, objectMapper.writeValueAsString(telexPayload));
    }

    public void failedInstruction(String channelId, String instruction) throws JsonProcessingException {
        String signedMessage = instruction + "\n\nSales Agent Bot";
        TelexPayload telexPayload = new TelexPayload("KYC", "Sales Agent Bot", "error", signedMessage);
        sendToTelexChannel(channelId, objectMapper.writeValueAsString(telexPayload));
    }

}
