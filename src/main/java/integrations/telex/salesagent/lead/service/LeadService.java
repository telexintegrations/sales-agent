package integrations.telex.salesagent.lead.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.config.OkHttpConfig;
import integrations.telex.salesagent.lead.dto.EmailFinderRequest;
import integrations.telex.salesagent.lead.dto.LeadDTO;
import integrations.telex.salesagent.lead.model.Lead;
import integrations.telex.salesagent.lead.repository.LeadRepository;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.dto.request.ColdEmailParams;
import integrations.telex.salesagent.user.model.ColdEmail;
import integrations.telex.salesagent.user.model.User;
import integrations.telex.salesagent.user.repository.ColdEmailRepository;
import integrations.telex.salesagent.user.repository.UserRepository;
import integrations.telex.salesagent.user.service.ColdEmailService;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final LeadResearchService leadResearchService;
    private final TelexClient telexClient;
    private final ColdEmailService coldEmailService;
    private final ColdEmailRepository coldEmailRepository;

    /**
     * Retrieves all leads with pagination.
     *
     * @param pageable pagination information
     * @return page of leads
     */
    public Page<Lead> getAllLeads(Pageable pageable) {
        return leadRepository.findAll(pageable);
    }

    public List<Lead> findAllLeads() {
        return leadRepository.findAll();
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
    public List<Lead> domainSearch(String channelId) {
        try {
            log.info("called domain search");
            Optional<User> userOptional = userRepository.findByChannelId(channelId);

            if (userOptional.isEmpty()) {
                String message = "User not found. Please provide a valid user.";
                telexClient.failedInstruction(channelId, message);
                return null;
            }

            User user = userOptional.get();
            String domain = user.getDomain();
            String userId = user.getId();

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

            List<Lead> existingLeads = leadRepository.findAll();
            List<Lead> newLeads = checkIfLeadsExists(leads,channelId,existingLeads);

           //Optional<ColdEmail> coldEmail = coldEmailRepository.findByChannelId(channelId);

//            for (Lead lead : newLeads) {
//                telexClient.processTelexPayload(channelId, lead);
//            }
            leadRepository.saveAll(newLeads);
            return newLeads;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public List<Lead> checkIfLeadsExists(List<Lead> leads,String userId,List<Lead> existingLeads){

        Map<String, String> existingLeadsMap = existingLeads.stream()
                .collect(Collectors.toMap(Lead::getEmail, Lead::getUserId, (existing, replacement) -> existing));

        return leads.stream()
                .filter(lead -> !existingLeadsMap.containsKey(lead.getEmail()) ||
                        !existingLeadsMap.get(lead.getEmail()).equals(userId))
                .toList();
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
            return ResponseEntity.ok(jsonNode.get("data"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}