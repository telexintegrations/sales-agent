package integrations.telex.salesagent.lead.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.lead.dto.CompanySearchRequest;
import integrations.telex.salesagent.lead.dto.LeadDTO;
import integrations.telex.salesagent.lead.dto.RapidLeadDto;
import integrations.telex.salesagent.lead.model.Lead;
import integrations.telex.salesagent.telex.service.TelexClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RapidLeadResearch {
    @Value("${rapidapi.key}")
    private String rapidApiKey;
    @Value("${rapidapi.url}")
    private String rapidApiUrl;
    @Value("${rapidapi.host}")
    private String rapidApiHost;


    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final TelexClient telexClient;

    public List<RapidLeadDto> queryLeads(CompanySearchRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RapidAPI-Key", rapidApiKey);
            headers.set("X-RapidAPI-Host", rapidApiHost);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

            log.info("Sending request to Rapid Url: {}", rapidApiUrl);
            log.info("Request Body: {}", request);

            ResponseEntity<String> response = restTemplate.exchange(
                    rapidApiUrl, HttpMethod.POST, entity, String.class
            );
            log.info("API Response: {}", response.getBody());
            if (response.getStatusCode().is2xxSuccessful()) {
                // Format response to List<LeadDTO>
                List<RapidLeadDto> newLeads = formatLeadsResponse(response.getBody());

                // Forward each lead to Telex
                for (RapidLeadDto lead : newLeads) {
                    telexClient.processTelexPayload("0195d8c2-279b-76f8-bbb6-8d6fe1021743", lead);
                    log.info("Lead sent to Telex: {}", lead.getName());
                }
            } else {
                log.error("Error from RapidAPI: {}", response.getStatusCode());
            }
            return formatLeadsResponse(response.getBody());
        } catch (Exception e) {
            log.error("Error querying RapidAPI: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<RapidLeadDto> formatLeadsResponse(String responseBody) {
        List<RapidLeadDto> leads = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("data").path("items");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    Long id = item.path("id").asLong();
                    String name = item.path("name").asText();
                    String linkedinUrl = item.path("linkedinURL").asText();
                    String tagline = item.path("tagline").asText();
                    String companyDesc = tagline.split("(?<=\\.\\s)")[0];

                    leads.add(new RapidLeadDto(id, name, linkedinUrl, companyDesc));
                }
            }
        } catch (Exception e) {
            log.error("Error parsing API response: {}", e.getMessage(), e);
        }
        return leads;
    }
}
