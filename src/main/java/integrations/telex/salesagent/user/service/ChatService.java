package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.config.AppConfig;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.dto.request.TelexPayload;
import integrations.telex.salesagent.user.entity.User;
import integrations.telex.salesagent.user.repository.UserRepository;
import integrations.telex.salesagent.user.utils.RequestFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final RequestFormatter requestFormatter;
    private final List<String> userResponses;
    private final TelexClient telexClient;
    private final ObjectMapper objectMapper;
    private final AppConfig appConfig;

    public void processMessage(String payload) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(payload);
        String htmlMessage = jsonNode.get("message").asText();
        String message = requestFormatter.stripHtml(htmlMessage);
        String channelId = jsonNode.get("channel_id").asText();

        // Ignore messages from the bot
        if (message.contains("Sales Agent Bot")) {
            return;
        }

        // Ensure the first message is always /start
        if (userResponses.isEmpty()) {
            if (!message.contains("/start")) {
                String instruction = "Invalid Command. Please type /start to begin the process.";
                failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add("/start");
            String instruction = "Welcome! Please provide your business email address starting with Email." +
                    "\n e.g. Email: test@example.com";
            log.info(userResponses.toString());
            sendInstruction(channelId, instruction);
            return;
        }

        // Ensure the second message is a valid email
        if (userResponses.size() == 1) {
            if (!isValidEmail(message)) {
                String instruction = "Invalid Email Address. Please provide a valid email address.";
                failedInstruction(channelId, instruction);
                return;
            }
            if (userRepository.findByEmail(message).isPresent()) {
                String instruction = "Email already exists. Please provide a different email address.";
                failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message);
            log.info(userResponses.toString());
            String instruction = "Please provide the company you're looking for starting with the word Company\n e.g." +
                    " " +
                    "Company: linkedin.";
            sendInstruction(channelId, instruction);
            return;
        }

        // Ensure the third message is a valid company name
        if (userResponses.size() == 2) {
            if (!message.startsWith("Company:".toLowerCase())) {
                String instruction = "Please provide the company you're looking for starting with the word Company\n " +
                        "e.g. Company: linkedin.";
                failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message);
            log.info(userResponses.toString());
            String instruction = "What type of lead are you looking for?\nEnter the domain name of the lead e.g. " +
                    "Domain: linkedin.com";
            sendInstruction(channelId, instruction);
            return;
        }

        // Ensure the fourth message is a valid domain name
        if (userResponses.size() == 3) {
            if (!message.contains("Domain".toLowerCase())) {
                String instruction = "Invalid Domain Name. Please provide a valid domain name.";
                failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message);
            log.info(userResponses.toString());
            saveUser(userResponses, channelId);
            callDomainSearchEndpoint();
            userResponses.clear();
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return Pattern.compile(emailRegex).matcher(email.replace("Email: ", "")).matches();
    }

    private void sendInstruction(String channelId, String instruction) throws JsonProcessingException {
        TelexPayload telexPayload = new TelexPayload("KYC", "Sales Agent Bot", "success", instruction);
        telexClient.sendToTelexChannel(channelId, objectMapper.writeValueAsString(telexPayload));
    }

    private void failedInstruction(String channelId, String instruction) throws JsonProcessingException {
        TelexPayload telexPayload = new TelexPayload("KYC", "Sales Agent Bot", "failed", instruction);
        telexClient.sendToTelexChannel(channelId, objectMapper.writeValueAsString(telexPayload));
    }


    private void saveUser(List<String> responses, String channelId) {
        User user = new User();
        user.setEmail(responses.get(1));
        user.setCompanyName(responses.get(2).replace("Company: ", ""));
        user.setLeadType(responses.get(3));
        user.setChannelId(channelId);
        userRepository.save(user);
    }

    private void callDomainSearchEndpoint() {
        String url = appConfig.getProductionBaseURL() + "/api/v1/leads/domain-search";
        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("Response from domain-search endpoint: {}", response);
        } catch (Exception e) {
            log.error("Error calling domain-search endpoint: {}", e.getMessage(), e);
        }
    }
}