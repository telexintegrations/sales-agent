package integrations.telex.salesagent.lead.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.lead.dto.PeopleLeadDto;
import integrations.telex.salesagent.lead.dto.PeopleSearchRequest;
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
import java.util.stream.Collectors;

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

    public List<PeopleLeadDto> queryLeads(String channelID, PeopleSearchRequest request) {
        try {
            // Build URL with location filter
            String searchUrl = request.buildLinkedInSearchUrl();
            log.info("Constructed LinkedIn search URL: {}", searchUrl);

            // Make API call
            List<PeopleLeadDto> leads = fetchLeadsFromApi(searchUrl);

            // Apply keyword filtering
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                leads = filterByKeyword(leads, request.getKeyword());
                log.info("Filtered {} leads by keyword: {}", leads.size(), request.getKeyword());
            }

            // Process results
            processResults(channelID, leads);

            return leads;

        } catch (Exception e) {
            log.error("Error calling RapidAPI: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    private List<PeopleLeadDto> filterByKeyword(List<PeopleLeadDto> leads, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return leads.stream()
                .filter(lead ->
                        (lead.getHeadline() != null &&
                                lead.getHeadline().toLowerCase().contains(lowerKeyword)) ||
                                (lead.getFullName() != null &&
                                        lead.getFullName().toLowerCase().contains(lowerKeyword)))
                .collect(Collectors.toList());
    }

    private void processResults(String channelID, List<PeopleLeadDto> leads) throws JsonProcessingException {
        if (leads.isEmpty()) {
            telexClient.sendInstruction(channelID, "🔍 No matching profiles found.");
        } else {
            leads.forEach(lead -> {
                try {
                    telexClient.processTelexPayload(channelID, lead);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private List<PeopleLeadDto> fetchLeadsFromApi(String searchUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key", rapidApiKey);
        headers.set("X-RapidAPI-Host", rapidApiHost);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = new HashMap<>();
        payload.put("url", searchUrl);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                rapidApiUrl, HttpMethod.POST, entity, String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("API request failed with status: {}", response.getStatusCode());
            return new ArrayList<>();
        }

        return formatPeopleResponse(response.getBody());
    }

    private List<PeopleLeadDto> formatPeopleResponse(String responseBody) {
        List<PeopleLeadDto> leads = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("data").path("items");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    PeopleLeadDto lead = new PeopleLeadDto(
                            item.path("fullName").asText(),
                            item.path("headline").asText(),
                            item.path("location").asText(),
                            item.path("profileURL").asText(),
                            item.path("username").asText()
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
