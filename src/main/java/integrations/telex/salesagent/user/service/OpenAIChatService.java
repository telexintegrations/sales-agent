package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.dto.request.LeadDetails;
import integrations.telex.salesagent.user.utils.RequestFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIChatService {
    private final OpenAIService openAIService;
    private final TelexClient telexClient;
    private final Map<String, List<String>> channelResponses = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final RequestFormatter requestFormatter;

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
            String prompt = """
                    Understood. To ensure accurate research, could you confirm your business type and the specific location you're targeting, along with any particular industry or company size criteria?
                    """;
            telexClient.sendInstruction(channelId, prompt);
        }
    }

    private void handleDetailsInput(String channelId, String message) throws JsonProcessingException {
        try {
            LeadDetails details = extractLeadDetails(message);
            leadDetailsMap.put(channelId, details);

            details.setCompanySizeClass(classifyCompanySize(details.getCompanySizes()));

            String prompt = "Thank you for the details, I'll now conduct research on " +
                    details.getCompanySizes() + " companies in " + details.getLocations() +
                    " to compile a list of potential leads. Please hold on while we work on this.";

            telexClient.sendInstruction(channelId, prompt);

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
            - "businessType": type of consulting/business
            - "locations": target location(s)
            - "companySizes": specified company sizes (small, mid-sized, large)
            
            For missing fields, use empty strings.
            """, userInput);

        String response = openAIService.getResponse(prompt);
        return objectMapper.readValue(response, LeadDetails.class);
    }

    private void generateAndSendResearch(String channelId, LeadDetails details) throws JsonProcessingException {
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

            String research = openAIService.getResponse(researchPrompt);
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
            I hope this message finds you well. My name is John Dowell and I lead BrightData - a firm dedicated to helping %s companies %s.
            
            We understand that every business faces unique challenges, and our tailored approach has empowered companies like [Example Client]. We specialize in [specific service] and believe we could add significant value to your operations.
            
            Would you be available for a brief call next week to discuss how we might support your goals?
            """,
                details.getCompanySizes(),
                details.getBusinessType().isEmpty() ? "streamline operations and drive sustainable growth" : details.getBusinessType());
    }

    private boolean isSaleAgentCalled(String message) {
        return message.toLowerCase().matches(".*(sales agent|sales bot|research leads).*");
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