package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.lead.dto.CompanySearchRequest;
import integrations.telex.salesagent.lead.dto.PeopleLeadDto;
import integrations.telex.salesagent.lead.dto.PeopleSearchRequest;
import integrations.telex.salesagent.lead.dto.RapidLeadDto;
import integrations.telex.salesagent.lead.enums.CompanySize;
import integrations.telex.salesagent.lead.service.LeadPeopleResearchService;
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
    //private final RapidLeadResearch rapidLeadResearch;
    private final LeadPeopleResearchService leadPeopleResearchService;

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
                     for business type, search location, and keyword. If all parameters are present, respond with
                      "I have understood the requirements. You are looking for [keyword] leads in [search location]
                       for [business type], let me get to that!".If any of the parameters are missing, respond with
                       'Hi, to ensure accurate research, please confirm the business type and the specific location you're targeting,
                        along with any desired lead e.g. software engineer.'
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

            if (details.getLocation() == null || details.getBusinessType() == null) {
                restartConversation(channelId);
                return;
            }

            String prompt = "Thank you for the details, I'll now fetch leads for " + details.getKeyword() + " in " + details.getLocation() +
                    " to compile a list of potential linkedIn profiles. Please hold on while we work on this.";

            telexClient.sendInstruction(channelId, prompt);

            PeopleSearchRequest peopleSearchRequest = new PeopleSearchRequest();
            peopleSearchRequest.setKeyword(details.getKeyword());
            peopleSearchRequest.setLocation(details.getLocation());

            leadPeopleResearchService.queryLeads(channelId, peopleSearchRequest);
            generateAndSendResearch(channelId, details);

            List<PeopleLeadDto> leads = leadPeopleResearchService.queryLeads(channelId, peopleSearchRequest);

            if (leads.isEmpty()) {
                telexClient.sendInstruction(channelId, "🔍 No matching profiles found.");
            } else {
                List<String> pitches = generatePitches(details, leads);
                for (String pitch : pitches) {
                    telexClient.sendInstruction(channelId, pitch);
                }
            }

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
            2. "location": Comma-separated if multiple (e.g., "Berlin, Munich").
            3. "keyword": Singular form (e.g., "software engineer" → "software engineer").
    
            Return ONLY valid JSON. Example:
            {"businessType": "laundromats", "location": "lagos", "keyword": "software engineer"}
            """, userInput);

        String response = chatModel.call(prompt);
        return objectMapper.readValue(response, LeadDetails.class);
    }

    private void generateAndSendResearch(String channelId, LeadDetails details) throws JsonProcessingException {
        try {
            // Generate research
            String researchPrompt = String.format("""
                Provide a detailed business lead research on %s companies in %s.
                Include:
                1. List of 5-10 potential leads with brief descriptions
                2. Key market trends in this sector
                3. Recommended outreach approach
                """,
                    details.getBusinessType(), details.getLocation());

            String research = chatModel.call(researchPrompt);
            telexClient.sendInstruction(channelId, research);
            conversationStates.put(channelId, ConversationState.COMPLETE);

        } catch (Exception e) {
            log.error("Research generation failed", e);
            telexClient.failedInstruction(channelId, "I couldn't complete the research. Please try again.");
        }
    }

    private List<String> generatePitches(LeadDetails details, List<PeopleLeadDto> leads) throws JsonProcessingException {
        List<String> pitches = new ArrayList<>();
        for (PeopleLeadDto lead : leads) {
            String samplePitch = String.format("""
                            Pitch
                            ---
                            I hope this message finds you well. I lead %s - a firm dedicated to helping %s.
                            We understand that every business faces unique challenges, and our tailored approach has empowered [Example Client]. We specialize in [specific service] and believe we could add significant value to your operations.
                            Would you be available for a brief call next week to discuss how we might support your goals?
                            """,
                    details.getBusinessType(), details.getKeyword());
            String prompt = String.format("""
                            Generate a short pitch personalized for %s , a %s, located in %s. Also create a place for my name \s
                             and my company.
                             use sample pitch to improve your response.
                             sample pitch : %s
                            """,
                    lead.getFullName(),
                    details.getKeyword(),
                    details.getLocation(),
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

}
