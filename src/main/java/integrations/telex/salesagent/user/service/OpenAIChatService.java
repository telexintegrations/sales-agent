package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.lead.dto.CompanySearchRequest;
import integrations.telex.salesagent.lead.dto.RapidLeadDto;
import integrations.telex.salesagent.lead.enums.CompanySize;
import integrations.telex.salesagent.lead.service.RapidLeadResearch;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.dto.request.LeadDetails;
import integrations.telex.salesagent.user.utils.RequestFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.entry;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIChatService {
    private final TelexClient telexClient;
    private final MistralAiChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final RequestFormatter requestFormatter;
    private final RapidLeadResearch rapidLeadResearch;

    private enum ConversationState {
        INITIAL,
        AWAITING_DETAILS,
        COMPLETE
    }

    private final Map<String, ConversationState> conversationStates = new ConcurrentHashMap<>();
    private final Map<String, LeadDetails> leadDetailsMap = new ConcurrentHashMap<>();

    public void processMessage(String payload) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(payload);
        String htmlMessage = jsonNode.get("message").asText();
        String message = requestFormatter.stripHtml(htmlMessage);
        String channelId = jsonNode.get("channel_id").asText();

        if (channelId.isEmpty() || message.isEmpty()) {
            log.error("Invalid payload received. Channel ID or message is empty.");
            return;
        }

        // Ignore messages from the bot
        if (message.contains("Sales Agent Bot")) {
            return;
        }

        if (isRestartRequest(message)) {
            restartConversation(channelId);
            return;
        }

        ConversationState currentState = conversationStates.getOrDefault(channelId, ConversationState.INITIAL);

        switch (currentState) {
            case INITIAL -> handleInitialState(channelId, message);
            case AWAITING_DETAILS -> handleDetailsInput(channelId, message);
            case COMPLETE -> restartConversation(channelId);
        }
    }

    private void handleInitialState(String channelId, String message) throws JsonProcessingException {
        if (isSaleAgentCalled(message)) {
            conversationStates.put(channelId, ConversationState.AWAITING_DETAILS);
            String prompt = String.format("""
                    Analyze the following text to determine if it contains the necessary parameters
                     for business type, search location, and company size. If all parameters are present, respond with
                      "I have understood the requirements. You are looking for [business type] businesses in [search location]
                       with a size of [company size], let me get to that!".If any of the parameters are missing, respond with
                       'Hi, to ensure accurate research, please confirm the business type and the specific location you're targeting,
                        along with any desired company size criteria.'
                        Text: '%s'"
                """, message);
            String response = chatModel.call(prompt);
            telexClient.sendInstruction(channelId, response);
        }
    }

    private void handleDetailsInput(String channelId, String message) throws JsonProcessingException {
        try {
            LeadDetails details = extractLeadDetails(message);
            log.info("Lead Details: {}", details);
            leadDetailsMap.put(channelId, details);

            if (details.getLocations() == null || details.getBusinessType() == null) {
                restartConversation(channelId);
                return;
            }

            String sizeCode = classifyCompanySize(details.getCompanySizes());

            log.info("Company size code: {}", sizeCode);

            String prompt1 = "Thank you for the details, I'll now conduct research on " +
                    details.getCompanySizes() + " companies in " + details.getLocations() +
                    " to compile a list of potential linkedIn profiles. Please hold on while we work on this.";

            telexClient.sendInstruction(channelId, prompt1);

            CompanySearchRequest searchRequest = convertToCompanySearchRequest(details);
            rapidLeadResearch.queryLeads(channelId, searchRequest);
            List<RapidLeadDto> leads = rapidLeadResearch.queryLeads(channelId,searchRequest);
            if (!leads.isEmpty()) {
                String prompt2 = String.format("I have found %d leads! \nI would now generate pitches for them!",leads.size());
                telexClient.sendInstruction(channelId, prompt2);
                List<String> pitches = generatePitches(details,leads);
                for (String pitch: pitches) {
                    telexClient.sendInstruction(channelId," Here is a pitch for you \n" + pitch);
                }
            }

            String prompt3 = "I'll now conduct researches on "
                    +details.getCompanySizes() + " "+ details.getBusinessType() + " companies in " + details.getLocations() +
                    " to compile a list of potential leads. Please hold on while we work on this.";

            telexClient.sendInstruction(channelId, prompt3);

            generateAndSendResearch(channelId, details);

            exitProcess(channelId);

        } catch (Exception e) {
            log.error("Error processing domain input", e);
            telexClient.failedInstruction(channelId, "Something went wrong. Please try again.");
        }
    }

    private LeadDetails extractLeadDetails(String userInput) throws JsonProcessingException {
        String prompt = String.format("""
            Extract structured data from this lead request:
            "%s"
    
            Rules:
            1. "businessType": Singular form (e.g., "tech startup" → "tech startup").
            2. "locations": Comma-separated if multiple (e.g., "Berlin, Munich").
            3. "companySizes": Standardize to "small", "mid-sized", or "large".
    
            Return ONLY valid JSON. Example:
            {"businessType": "law firm", "locations": "London", "companySizes": "mid-sized"}
            """, userInput);

        String response = chatModel.call(prompt);
        return objectMapper.readValue(response, LeadDetails.class);
    }

    private void generateAndSendResearch(String channelId, LeadDetails details) throws JsonProcessingException {
        try {
            // Generate research
            String researchPrompt = String.format("""
                Provide a detailed business lead research on %s %ss companies in %s.
                Include:
                1. List of 5-10 potential leads with brief descriptions
                2. Key market trends in this sector
                3. Recommended outreach approach
                """,
                    details.getCompanySizes(), details.getBusinessType(), details.getLocations());

            String research = chatModel.call(researchPrompt);
            telexClient.sendInstruction(channelId, research);
            conversationStates.put(channelId, ConversationState.COMPLETE);

        } catch (Exception e) {
            log.error("Research generation failed", e);
            telexClient.failedInstruction(channelId, "I couldn't complete the research. Please try again.");
        }
    }

    private List<String> generatePitches(LeadDetails details, List<RapidLeadDto> leads) throws JsonProcessingException {
        List<String> pitches = new ArrayList<>();
        for (RapidLeadDto lead : leads) {
            String samplePitch = String.format("""
                            Pitch
                            ---
                            I hope this message finds you well. I lead [Company name] - a firm dedicated to helping %s companies %s.
                            We understand that every business faces unique challenges, and our tailored approach has empowered companies like [Example Client]. We specialize in [specific service] and believe we could add significant value to your operations.
                            Would you be available for a brief call next week to discuss how we might support your goals?
                            """,
                    details.getCompanySizes(),
                    details.getBusinessType().isEmpty() ? "streamline operations and drive sustainable growth" : details.getBusinessType());
            String prompt = String.format("""
                            Generate a short pitch personalized for %s , a %s company, located in %s. Also create a place for my name \s
                             and my company.
                             use sample pitch to improve your response.
                             sample pitch : %s
                            """,
                    lead.getName(),
                    details.getCompanySizes(),
                    details.getLocations(),
                    samplePitch);
            String pitch = chatModel.call(prompt);
            pitches.add(pitch);
        }
        return pitches;
    }

    private boolean isSaleAgentCalled(String message) {
        String request = String.format( """
                You are pat the sales Agent. Carefully analyze the text and determine whether it relates to lead generation by a sales agent.
                Look for explicit indicators such as references to prospecting, identifying potential customers,
                outreach efforts, nurturing leads, sales,or follow-up strategies designed to convert prospects into clients or \s
                calls you directly by your name i.e pat.
                Answer the question does the text want to find leads?.\s
                 respond only with true or false.
                 the text: '%s'
                \s""", message);
        String response = chatModel.call(request).toLowerCase();
        return response.contains("true");
    }

    private void exitProcess(String channelId) throws JsonProcessingException {
        conversationStates.remove(channelId);
        leadDetailsMap.remove(channelId);
        String instruction = """
                That concludes the leads process.
                Let me know if you need help with lead generation in the future.
                """;
        telexClient.sendInstruction(channelId, instruction);
    }

    private void restartConversation(String channelId) throws JsonProcessingException {
        conversationStates.put(channelId, ConversationState.INITIAL);
        leadDetailsMap.remove(channelId);
        String instruction = """
                What would you like to do?
                You can say something like:"
                I need help generating leads for my digital marketing agency targeting tech startups in Abuja.
                """;
        telexClient.sendInstruction(channelId, instruction);
    }

    private boolean isRestartRequest(String message) {
        String prompt = String.format("""
        Analyze if the user wants to restart or start over.\s
        Look for phrases like: "start over", "restart", "new search", "begin again", etc.
        Respond ONLY with 'true' or 'false'.
        Message: '%s'
       \s""", message);
        String response = chatModel.call(prompt).toLowerCase().trim();
        return response.equals("true");
    }

    private String classifyCompanySize(String companySize) {
        if (companySize == null || companySize.isEmpty()) {
            return "C";
        }

        companySize = companySize.toLowerCase().trim();

        return switch (companySize) {
            case "large" -> "G";
            case "very large" -> "I";
            case "mid-sized", "medium" -> "D";
            case "small" -> "B";
            default -> "C";
        };
    }

    private CompanySearchRequest convertToCompanySearchRequest(LeadDetails details) {
        CompanySearchRequest request = new CompanySearchRequest();

        // Set keyword from businessType
        request.setKeyword(details.getBusinessType());

        // Parse locations (assuming comma-separated string like "Lagos,New York")
        if (details.getLocations() != null && !details.getLocations().isEmpty()) {
            List<Integer> locationIds = Arrays.stream(details.getLocations().split(","))
                    .map(String::trim)
                    .map(this::convertLocationToId)
                    .filter(Objects::nonNull)
                    .toList();
            request.setLocations(locationIds);
        }

        // Convert company sizes
        if (details.getCompanySizes() != null && !details.getCompanySizes().isEmpty()) {
            List<CompanySize> companySizes = Arrays.stream(details.getCompanySizes().split(","))
                    .map(String::trim)
                    .map(this::convertToCompanySize)
                    .filter(Objects::nonNull)
                    .toList();
            request.setCompanySizes(companySizes);
        }

        return request;
    }

    private Integer convertLocationToId(String locationName) {
        Map<String, Integer> locationMap =Map.ofEntries(
                entry("US", 103644278),
                entry("Lagos", 104197452),
                entry("Nigeria",105365761),
                entry("ABUJA, FCT Nigeria", 101711968),
                entry("London Area, United Kingdom", 90009496),
                entry("Lekki, Lagos State, Nigeria", 111964948),
                entry("Ibeju Lekki, Lagos State, Nigeria", 105956099),
                entry("Ikorodu, Lagos State, Nigeria", 103510932),
                entry("Agege, Lagos State, Nigeria", 100686593),
                entry("Port Harcourt, Rivers State, Nigeria", 114378074),
                entry("Ibadan, Oyo State, Nigeria", 110864965),
                entry("Kaduna, Kaduna State, Nigeria", 103668447),
                entry("Worldwide", 92000000),
                entry("Dubai, United Arab Emirates", 106204383),
                entry("Asia", 102393603),
                entry("North America", 102221843)
        );
        return locationMap.getOrDefault(locationName, null);
    }

    private CompanySize convertToCompanySize(String sizeString) {
        return switch (sizeString.toLowerCase()) {
            case "b" -> CompanySize.B;
            case "c" -> CompanySize.C;
            case "d" -> CompanySize.D;
            case "e" -> CompanySize.E;
            case "f" -> CompanySize.F;
            case "g" -> CompanySize.G;
            case "h" -> CompanySize.H;
            case "i" -> CompanySize.I;
            default -> null;
        };
    }
}
