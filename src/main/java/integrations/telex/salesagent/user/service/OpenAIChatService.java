package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.lead.dto.CompanySearchRequest;
import integrations.telex.salesagent.lead.enums.CompanySize;
import integrations.telex.salesagent.lead.service.RapidLeadResearch;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.dto.request.LeadDetails;
import integrations.telex.salesagent.user.utils.RequestFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.entry;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIChatService {
    private final OpenAIService openAIService;
    private final TelexClient telexClient;
    private final Map<String, List<String>> channelResponses = new ConcurrentHashMap<>();
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
                       with a size of [company size], let me get to that !".If any of the parameters are missing, respond with
                       'Hi, to ensure accurate research, please confirm the business type and the specific location you're targeting,
                        along with any desired company size criteria.'
                        Text: '%s'"
                """,message);
//                      String prompt = String.format("""
//                    Understood the intention of '%s'.
//                    if it contains the all the parameters(business type, search location, company size)
//                    return a friendly message showing you understand but if not,
//
//                    send 'To ensure accurate research, could you confirm your business type and the specific location you're targeting, along with any particular industry or company size criteria.'
//                    """,message);
            String response = chatModel.call(prompt);
            telexClient.sendInstruction(channelId, response);
        }
    }

    private void handleDetailsInput(String channelId, String message) throws JsonProcessingException {
        try {
            LeadDetails details = extractLeadDetails(message);
            log.info("Lead Details: {}", details);
            leadDetailsMap.put(channelId, details);

            //details.setCompanySizeClass(classifyCompanySize(details.getCompanySizes()));

            String prompt1 = "Thank you for the details, I'll now conduct research on " +
                    details.getCompanySizes() + " companies in " + details.getLocations() +
                    " to compile a list of potential linkedIn profiles. Please hold on while we work on this.";

            telexClient.sendInstruction(channelId, prompt1);

            CompanySearchRequest searchRequest = convertToCompanySearchRequest(details);
            rapidLeadResearch.queryLeads(channelId, searchRequest);

            String prompt2 = "I'll now conduct research on " +
                    details.getCompanySizes() + " companies in " + details.getLocations() +
                    " to compile a list of potential leads. Please hold on while we work on this.";

            telexClient.sendInstruction(channelId, prompt2);

            generateAndSendResearch(channelId, details);

        } catch (Exception e) {
            log.error("Error processing domain input", e);
            telexClient.failedInstruction(channelId, "Something went wrong. Please try again.");
        }
    }

    private LeadDetails extractLeadDetails(String userInput) throws JsonProcessingException {
        String prompt = String.format("""
            Extract the following details from this business lead request:
            "%s"
            
            Return JSON with these keys:
            - "businessType": type of business (note -> return it in singular form)
            - "locations": target location(s)
            - "companySizes": specified company sizes (small, mid-sized, large)
            
            For missing fields, use empty strings.
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

            // Generate pitch
            String pitch = generatePitch(details);
            telexClient.sendInstruction(channelId,
                    "I've completed the initial research and generated a preliminary list. " +
                            "Based on the gathered data, I've also drafted a tailored pitch:\n\n" + pitch);

            conversationStates.put(channelId, ConversationState.COMPLETE);

        } catch (Exception e) {
            log.error("Research generation failed", e);
            telexClient.sendInstruction(channelId, "I couldn't complete the research. Please try again.");
        }
    }

    private void generateBusinessResearch(String channelId, LeadDetails details) throws JsonProcessingException {
        try {
            // Generate research
            String researchPrompt = String.format("""
                Provide a detailed business lead research for %s companies in %s.
                Focus on companies that would benefit from %s services.
                Include:
                1. List of 5-10 potential leads with brief descriptions
                2. Key market trends in this sector
                3. Recommended outreach approach
                """,
                    details.getCompanySizes(), details.getLocations(), details.getBusinessType());

            String research = chatModel.call(researchPrompt);
            telexClient.sendInstruction(channelId, research);

            // Generate pitch
            String pitch = generatePitch(details);
            telexClient.sendInstruction(channelId,
                    "I've completed the initial research and generated a preliminary list. " +
                            "Based on the gathered data, I've also drafted a tailored pitch:\n\n" + pitch);

            conversationStates.put(channelId, ConversationState.COMPLETE);

        } catch (Exception e) {
            log.error("Research generation failed", e);
            telexClient.sendInstruction(channelId, "I couldn't complete the research. Please try again.");
        }
    }

    private String generatePitch(LeadDetails details) {
        return String.format("""
            Pitch
            ---
            I hope this message finds you well. My name is John Dowell and I lead [Company name] - a firm dedicated to helping %s companies %s.
            
            We understand that every business faces unique challenges, and our tailored approach has empowered companies like [Example Client]. We specialize in [specific service] and believe we could add significant value to your operations.
            
            Would you be available for a brief call next week to discuss how we might support your goals?
            """,
                details.getCompanySizes(),
                details.getBusinessType().isEmpty() ? "streamline operations and drive sustainable growth" : details.getBusinessType());
    }

    private boolean isSaleAgentCalled(String message) {
        String request = String.format( """
                Carefully analyze the text and determine whether it relates to lead generation by a sales agent.
                Look for explicit indicators such as references to prospecting, identifying potential customers,
                outreach efforts, nurturing leads, sales,or follow-up strategies designed to convert prospects into clients.
                Answer the question does the text want to find leads?.\s
                 respond only with True or False.
                 the text: '%s'
                 """,message);
        String response = chatModel.call(request).toLowerCase();
        return response.contains("true");
    }

    private void exitProcess(String channelId) throws JsonProcessingException {
        conversationStates.remove(channelId);
        leadDetailsMap.remove(channelId);
        telexClient.sendInstruction(channelId, "Goodbye! Let me know if you need help with lead generation in the future.");
    }

    private void restartConversation(String channelId) throws JsonProcessingException {
        conversationStates.put(channelId, ConversationState.INITIAL);
        leadDetailsMap.remove(channelId);
        telexClient.sendInstruction(channelId, "What would you like to research next? You can say something like: " +
                "\"I need help generating leads for my digital marketing agency targeting tech startups in Berlin\"");
    }

    private String classifyCompanySize(String companySize) {
        if (companySize == null) return "C";
        companySize = companySize.toLowerCase();
        if (companySize.contains("mid")) return "B";
        if (companySize.contains("large")) return "A";
        return "C"; // default to small
    }

    private CompanySearchRequest convertToCompanySearchRequest(LeadDetails details) {
        CompanySearchRequest request = new CompanySearchRequest();

        // Set keyword from businessType
        request.setKeyword(details.getBusinessType());

        // Parse locations (assuming comma-separated string like "Lagos,New York")
        if (details.getLocations() != null && !details.getLocations().isEmpty()) {
            List<Integer> locationIds = Arrays.stream(details.getLocations().split(","))
                    .map(String::trim)
                    .map(this::convertLocationToId) // You'll need to implement this
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

/*
   private void handleInitialState(String channelId, String message) throws JsonProcessingException {
        if (isSaleAgentCalled(message) || message.equalsIgnoreCase("/start")) {
            conversationStates.put(channelId, ConversationState.AWAITING_DOMAIN);
//            String prompt = """
//                Welcome! I'm your Sales Agent Assistant.
//                Please tell me what type of leads you're looking for and the location.
//                Examples:
//                - "I need laundromat leads in Lagos"
//                - "Looking for tech startups in Berlin"
//                - "Restaurant owners in New York"
//                """;
            String prompt = """
                    Understood. To ensure accurate research, could you confirm your business type and the specific location you're targeting, along with any particular industry or company size criteria?
                    """;
            telexClient.sendInstruction(channelId, prompt);
        }
    }
 */

/*
Hello, I need assistance generating leads for my business consulting firm.

Understood. To ensure accurate research, could you confirm your business type and the specific location you're targeting, along with any particular industry or company size criteria?

I'm running a consulting firm in Lagos, and I'm specifically looking for mid-sized companies?

Thank you for the details, I'll now conduct research on mid-sized companies in the Lagos to compile a list of potential leads. Please hold on while we work on this.

I've completed the initial research and generated a preliminary list. Based on the gathered data, I've also drafted a tailored pitch that outlines your consulting expertise and the unique value you offer to these companies.

Pitch
I hope this message finds you well. My name is John Dowell and I lead BrightPath Consulting - a firm dedicated to helping mid-sized companies streamline operations and drive sustainable growth. We understand that every business faces unique challenges, and our tailored approach has empowered companies like Innovative Tech.
 */

//    private void handleConfirmation(String channelId, String message) throws JsonProcessingException {
//        String searchTerm = channelResponses.computeIfAbsent(channelId, k -> new ArrayList<>()).getFirst();
//        completeResearch(channelId, searchTerm, message);
//    }

//    private void completeResearch(String channelId, String searchTerm, String location) throws JsonProcessingException {
//        try {
//            String researchResults = generateLeadResearch(searchTerm, location);
//            telexClient.sendInstruction(channelId, researchResults);
//            conversationStates.put(channelId, ConversationState.COMPLETE);
//
//            // Log the successful search (previously was saving to DB)
//            log.info("Completed research for {} in {}", searchTerm, location);
//        } catch (Exception e) {
//            log.error("Research failed", e);
//            telexClient.sendInstruction(channelId, "Sorry, I couldn't complete the research. Please try again.");
//        }
//    }

//    private String generateLeadResearch(String searchTerm, String location) {
//        String prompt = String.format("""
//            Act as a professional business lead researcher. Provide information about %s in %s including:
//            1. Potential leads (business names/types)
//            2. Market trends
//            3. Competitive landscape
//            4. Recommended outreach strategy
//
//            Format with clear headings and bullet points.
//            """, searchTerm, location);
//
//        return openAIService.getResponse(prompt);
//    }