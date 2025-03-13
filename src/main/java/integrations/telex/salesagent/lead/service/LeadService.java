package integrations.telex.salesagent.lead.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.config.AppConfig;
import integrations.telex.salesagent.config.OkHttpConfig;
import integrations.telex.salesagent.lead.dto.EmailFinderRequest;
import integrations.telex.salesagent.lead.dto.LeadDTO;
import integrations.telex.salesagent.lead.entity.Lead;
import integrations.telex.salesagent.lead.repository.LeadRepository;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class for managing leads.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;

    private final OkHttpConfig okHttpConfig;

    private final RestClient restClient = RestClient.create();

    private final OkHttpClient okHttpClient;

    private final ObjectMapper objectMapper;

    private final UserRepository userRepository;

    private final AppConfig appConfig;

    private final TelexClient telexClient;

    /**
     * Retrieves all leads with pagination.
     *
     * @param pageable pagination information
     * @return page of leads
     */
    public Page<Lead> getAllLeads(Pageable pageable) {
        return leadRepository.findAll(pageable);
    }

    /**
     * Creates a new lead.
     *
     * @param newLead lead information
     * @return response entity with creation status
     */
    @Transactional
    public ResponseEntity<String> createNewLead(LeadDTO newLead) {
        try {
            Lead lead = Lead.builder()
                    .name(newLead.getLeadName())
                    .email(newLead.getLeadMail())
                    .company(newLead.getLeadCompany())
                    .createdAt(LocalDateTime.now())
                    .build();

            leadRepository.save(lead);
            return ResponseEntity.ok("New lead created successfully");
        } catch (Exception error) {
            return ResponseEntity.internalServerError().body(error.getMessage());
        }
    }
    public void domainSearch() {
        try {
            var user = userRepository.findByChannelId(appConfig.getTelexChannelId());

            if (user.isEmpty()) {
                throw new RuntimeException("User not found");
            }

            String domain = user.get().getLeadType();
            String userId = user.get().getId();

            String key = okHttpConfig.hunterParams().getApikey();
            String baseUrl = okHttpConfig.hunterParams().getBaseUrl();

            String uri = baseUrl + "/domain-search?" +
                    "domain={" + domain + "}&api_key={" + key + "}";

            ResponseEntity<?> response = restClient.get()
                    .uri(uri, domain, key)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody().toString());

            // Create Lead entities from the response data
            List<Lead> leads = new ArrayList<>();
            jsonNode.get("data").get("emails").forEach(email -> {
                Lead lead = Lead.builder()
                        .name(email.get("first_name").asText() + " " + email.get("last_name").asText())
                        .email(email.get("value").asText())
                        .linkedInUrl(email.get("linkedin").asText())
                        .company(jsonNode.get("data").get("organization").asText())
                        .industry(jsonNode.get("data").get("industry").asText())
                        .userId(userId)
                        .build();
                leads.add(lead);
            });

            // Get all existing emails from the database
            Set<String> existingEmails = leadRepository.findAll().stream()
                            .map(Lead::getEmail)
                                    .collect(Collectors.toSet());

            // Filter out leads with emails already in the database
            List<Lead> newLeads = leads.stream()
                    .filter(lead -> !existingEmails.contains(lead.getEmail()))
                    .toList();

            telexClient.processTelexPayload(Integer.toString(newLeads.size()));

            leadRepository.saveAll(newLeads);
            log.info("Saved {} new leads", newLeads.size());
            //return ResponseEntity.ok(leads);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public ResponseEntity<?> emailFinder(EmailFinderRequest request) {
        try {
            String key = okHttpConfig.hunterParams().getApikey();
            String baseUrl = okHttpConfig.hunterParams().getBaseUrl();
            String uri = baseUrl + "/email-finder?" +
                    "domain={domain}&company={company}&first_name={first_name}" +
                    "&last_name={last_name}&full_name={full_name}&max_duration={max_duration}" +
                    "&api_key={key}";

            ResponseEntity<?> response = restClient.get()
                    .uri(uri, request.getDomain(), request.getCompany(), request.getFirstName()
                            , request.getLastName(), request.getFullName()
                            , request.getMaxDuration(), key)
                    .retrieve()
                    .toEntity(String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.getBody().toString()).get("data");
            Lead lead = Lead.builder()
                .name(jsonNode.get("first_name").asText() + " " + jsonNode.get("last_name").asText())
                .email(jsonNode.get("value").asText())
                .linkedInUrl(jsonNode.get("linkedin").asText())
                .company(jsonNode.get("organization").asText())
                .industry(jsonNode.get("industry").asText())
                .build();
            leadRepository.save(lead);
            return ResponseEntity.ok(lead);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public ResponseEntity<?> emailVerifier(String email) {
        // email verification logic
        try{
            String key = okHttpConfig.hunterParams().getApikey();
            String baseUrl = okHttpConfig.hunterParams().getBaseUrl();
            ResponseEntity<?> response = restClient.get()
                    .uri(baseUrl+"/email-verifier?" +
                            "email={email}&api_key={key}",email,key)
                    .accept(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.getBody().toString());
            String status = jsonNode.get("status").asText();
            int score = jsonNode.get("score").asInt();

            if(!Objects.equals(status, "valid") || score < 69){
                return ResponseEntity.ok(jsonNode.get("data"));
            }else{
                return ResponseEntity.ok("Email is valid");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    //Free endpoint for Testing format
    public ResponseEntity<?> account() {
        // Implement email verification logic here
        String key = okHttpConfig.hunterParams().getApikey();
        String baseUrl = okHttpConfig.hunterParams().getBaseUrl();
        String url = baseUrl + "/account?api_key=" + key;
        Request request = new Request.Builder()
                .url(url)
                .build();
        try{
            Response response = okHttpClient.newCall(request).execute();
            byte[] responseBody = response.body().bytes();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return ResponseEntity.ok(jsonNode.get("data").get("calls"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}