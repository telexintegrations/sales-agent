package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.lead.service.LeadService;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.entity.User;
import integrations.telex.salesagent.user.repository.UserRepository;
import integrations.telex.salesagent.user.utils.RequestFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final UserRepository userRepository;
    private final RequestFormatter requestFormatter;
    private final TelexClient telexClient;
    private final ObjectMapper objectMapper;
    private final LeadService leadService;
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

        //channelResponses.putIfAbsent(channelId, new ArrayList<>());
        //List<String> userResponses = channelResponses.get(channelId);
        List<String> userResponses = channelResponses.computeIfAbsent(channelId, k -> new ArrayList<>());

        // Ensure the first message is always /start
        if (userResponses.isEmpty()) {
            if (!message.contains("/start")) {
                String instruction = "Invalid Command. Please type /start to begin the process.";
                telexClient.failedInstruction(channelId, instruction);
                //channelResponses.remove(channelId);
                return;
            }
            userResponses.add("/start");
            String instruction = "Welcome! Please provide your business email address starting with Email." +
                    "\n e.g. Email: test@example.com";
            telexClient.sendInstruction(channelId, instruction);
            return;
        }

        // Ensure the second message is a valid email
        if (userResponses.size() == 1) {
            String extractedEmail = message.replace("Email: ", "");
            if (!isValidEmail(message)) {
                String instruction = "Invalid Email Address. Please provide a valid email address.\n" +
                        "e.g. Email: test@example.com";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            if (userRepository.findByEmail(extractedEmail).isPresent()) {
                String instruction = "Email already exists. Please provide a different email address.\n " +
                        "e.g. Email: test@exampl,e.com";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(extractedEmail);
            String instruction = "Please provide the company you're looking for starting with the word Company\n e.g." +
                    " " +
                    "Company: linkedin.";
            telexClient.sendInstruction(channelId, instruction);
            return;
        }

        // Ensure the third message is a valid company name
        if (userResponses.size() == 2) {
            if (!message.startsWith("Company: ")) {
                String instruction = "Please provide the company you're looking for starting with the word Company\n " +
                        "e.g. Company: linkedin.";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            String extractedCompany = message.replace("Company: ", "");
            userResponses.add(extractedCompany);
            String instruction = "What type of lead are you looking for?\nEnter the domain name of the lead e.g. " +
                    "Domain: linkedin.com";
            telexClient.sendInstruction(channelId, instruction);
            return;
        }

        // Ensure the fourth message is a valid domain name
        if (userResponses.size() == 3) {
            if (!message.contains("Domain: ")) {
                String instruction = "Invalid Domain Name. Please provide a valid domain name.";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            String extractedDomain = message.replace("Domain: ", "");
            userResponses.add(extractedDomain);
            saveUser(userResponses, channelId);

            String instruction = "Your search criteria have been saved. We will notify you when we find leads matching your criteria.";
            telexClient.sendInstruction(channelId, instruction);

            channelResponses.remove(channelId);
            callDomainSearchEndpoint(channelId);
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return Pattern.compile(emailRegex).matcher(email.replace("Email: ", "")).matches();
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
}