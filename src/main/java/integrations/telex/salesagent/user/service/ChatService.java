package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.lead.service.LeadService;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.model.User;
import integrations.telex.salesagent.user.repository.UserRepository;
import integrations.telex.salesagent.user.utils.RequestFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final UserRepository userRepository;
    private final RequestFormatter requestFormatter;
    private final TelexClient telexClient;
    private final ObjectMapper objectMapper;
    private final LeadService leadService;
    private final ChatModel chatModel;
    private final Map<String, List<String>> channelResponses = new ConcurrentHashMap<>();

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

        List<String> userResponses = channelResponses.computeIfAbsent(channelId, k -> new ArrayList<>());

        if (userResponses.isEmpty()) {
            if (isSaleAgentCalled(message) || message.contains("/start")) {
                userResponses.add("/start");
                String instruction = "Welcome! Please provide your business email address starting with Email." +
                        "\n e.g. test@example.com";
                telexClient.sendInstruction(channelId, instruction);
            }else {
                log.info(isSaleAgentCalled(message).toString());
                return;
            }
            return;

        }

        if (userResponses.size() == 1) {
            if (!isValidEmail(message)) {
                String instruction = "Invalid Email Address. Please provide a valid email address";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            if (userRepository.findByEmail(message).isPresent()) {
                String instruction = "Email already exists. Please provide a different email address.";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message);
            String instruction = "Please provide the company you're looking for starting with the word Company " +
                    "e.g. linkedin";
            telexClient.sendInstruction(channelId, instruction);
            return;
        }

        if (userResponses.size() == 2) {
            if (!isValidCompany(message)) {
                String instruction = "Please provide the company you're looking for starting with the word Company\n " +
                        "e.g. linkedin";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message);
            String instruction = "What type of lead are you looking for?\nEnter the domain name of the lead e.g. linkedin.com";
            telexClient.sendInstruction(channelId, instruction);
            return;
        }

        if (userResponses.size() == 3) {
            if (!isValidDomain(message)) {
                String instruction = "Invalid Domain Name. Please provide a valid domain name.";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message);
            saveUser(userResponses, channelId);

            String instruction = "Your search criteria have been saved. We will notify you when we find leads matching your criteria.";
            telexClient.sendInstruction(channelId, instruction);

            channelResponses.remove(channelId);
            callDomainSearchEndpoint(channelId);
        }
    }


    private boolean isValidEmail(String email) {
        String request = String.format(
                "Validate the following string to ensure it is a properly" +
                        " formatted email address. The validation rules are: it must contain exactly one " +
                        "'@' symbol, have characters before and after the '@', include at least one " +
                        "period in the domain part (e.g., 'example.com'), avoid any invalid characters" +
                        " such as spaces or special symbols, and ensure the domain part is appropriately" +
                        " structured. Respond only with 'true' if it meets all the criteria, or 'false'" +
                        " otherwise and remove everything returning only one word. The email address is '%s' ",email);
        String response = chatModel.call(request).toLowerCase();
        return response.contains("true");
    }

    private boolean isValidCompany(String company) {
        String request = String.format(
                "Check if the following string represents a valid company name. The validation criteria are: " +
                        "1. The name must include only alphanumeric characters, spaces, and permissible symbols like '&', '-', or '.'. " +
                        "2. It must not contain invalid characters (e.g., special characters like '@', '#', '$', etc.). " +
                        "3. The name must not be empty or overly short (e.g., fewer than 2 characters). " +
                        "4. It should not consist solely of generic words (e.g., 'Company' or 'Business'), " +
                        "but may include them alongside unique identifiers (e.g., 'Tech Innovators Inc.'). " +
                        "Respond with 'true' if it meets all the criteria or 'false' if it does not and remove everything returning only one word " +
                        "The company name is '%s' ",company);
        String response = chatModel.call(request).toLowerCase();
        return response.contains("true");
    }

    private boolean isValidDomain(String domain) {
        String request = String.format(
                "Check if the following string is a valid domain name. The validation" +
                        " rules are: \n it must have a valid structure (e.g., 'subdomain.domain.top-level-domain'), " +
                        "contain only permissible characters (letters, numbers, and hyphens), and include a valid top-level domain" +
                        " such as '.com', '.org', '.net', or any recognized TLD. Respond with " +
                        "'Valid' if it meets these criteria, or 'Invalid' if it does not. The domain name is '%s' "+
                        " reply only with true or false and remove everything returning only one word",domain);
        String response = chatModel.call(request).toLowerCase();
        return response.contains("true");
    }

    private void saveUser(List<String> responses, String channelId) {
        User user = new User();
        user.setEmail(responses.get(1));
        user.setCompanyName(responses.get(2));
        user.setLeadType(responses.get(3));
        user.setChannelId(channelId);
        userRepository.save(user);
    }

    private void callDomainSearchEndpoint(String channelId) {
        leadService.domainSearch(channelId);
    }

    private Boolean isSaleAgentCalled(String message){
        String request = String.format("Carefully analyze the following text and determine whether it relates to lead" +
                " generation by a sales agent. Look for explicit indicators such as references to prospecting, identifying potential customers, outreach efforts, nurturing leads, sales pitches," +
                " or follow-up strategies designed to convert prospects into clients. Provide a clear 'true' or 'false' only as a single response," +
                "i emphasize that it must be a single word response!be  extremely concise"+
                "the text is '%s'",message);
        String response = chatModel.call(request).toLowerCase();
        return response.contains("true");
    }

}