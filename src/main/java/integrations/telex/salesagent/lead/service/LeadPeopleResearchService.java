package integrations.telex.salesagent.lead.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.lead.dto.PeopleSearchRequest;
import integrations.telex.salesagent.lead.dto.RapidLeadDto;
import integrations.telex.salesagent.telex.service.TelexClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadPeopleResearchService {
    @Value("${rapidapi.key}")
    private String rapidApiKey;

    @Value("${rapidapi.people.url}")
    private String rapidApiUrl;

    @Value("${rapidapi.host}")
    private String rapidApiHost;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TelexClient telexClient;

    public List<RapidLeadDto> queryLeads(String channelID, PeopleSearchRequest request) {
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RapidAPI-Key", rapidApiKey);
            headers.set("X-RapidAPI-Host", rapidApiHost);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Build the request body (LinkedIn search URL)
            Map<String, String> payload = new HashMap<>();
            payload.put("url", request.getUrl());

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

            // Call RapidAPI
            log.info("Searching for people with URL: {}", request.getUrl());
            ResponseEntity<String> response = restTemplate.exchange(
                    rapidApiUrl, HttpMethod.POST, entity, String.class
            );

            // Process the response
            if (response.getStatusCode().is2xxSuccessful()) {
                List<RapidLeadDto> leads = formatPeopleResponse(response.getBody());

                if (leads.isEmpty()) {
                    String report = "🔍 No LinkedIn profiles found for the given search URL.";
                    telexClient.sendInstruction(channelID, report);
                } else {
                    for (RapidLeadDto lead : leads) {
                        telexClient.processTelexPayload(channelID, lead);
                    }
                }
                return leads;
            } else {
                log.error("API Error: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Error calling RapidAPI: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Parse API response into Lead objects
    private List<RapidLeadDto> formatPeopleResponse(String responseBody) {
        List<RapidLeadDto> leads = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("data").path("items");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    RapidLeadDto lead = new RapidLeadDto(
                            null,
                            item.path("fullName").asText(),
                            item.path("profileURL").asText(),
                            String.format("%s | %s",
                                    item.path("headline").asText(),
                                    item.path("location").asText())
                    );
                    leads.add(lead);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing API response: {}", e.getMessage(), e);
        }
        return leads;
    }
}
