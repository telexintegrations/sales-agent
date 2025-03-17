package integrations.telex.salesagent.telex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.config.AppConfig;
import integrations.telex.salesagent.lead.entity.Lead;
import integrations.telex.salesagent.telex.util.FormatTelexMessage;
import integrations.telex.salesagent.user.dto.request.TelexPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelexClient {
    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final FormatTelexMessage formatTelexMessage;

    public void sendToTelexChannel(String channel_id, TelexPayload payload) {
        try{
            RequestBody requestBody = RequestBody.create(payload.toJson(),null);
            String url = "https://ping.telex.im/v1/webhooks/"+channel_id;
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            log.info("Response from Telex : {}",response);
        }catch (RuntimeException | IOException e){
            throw new RuntimeException(e);
        }
    }

    public void processTelexPayload(String channelID, Lead lead) throws JsonProcessingException {
        String message = formatTelexMessage.formatNewLeadMessage(lead);

        TelexPayload telexPayload = new TelexPayload("New Lead Alert", "Sales Agent", "success", message);

        sendToTelexChannel(channelID, telexPayload);
    }

}
