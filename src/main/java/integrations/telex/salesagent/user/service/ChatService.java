package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.dto.request.SalesAgentPayloadDTO;
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
    ObjectMapper objectMapper = new ObjectMapper();

    public void processMessage(String payload) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(payload);
        String htmlMessage = jsonNode.get("message").asText();
        String message = requestFormatter.stripHtml(htmlMessage);
        String channelId = jsonNode.get("channel_id").asText();

        // Ensure the first message is always /start
        if (userResponses.isEmpty()) {
            if (!message.contains("/start")) {
                sendInstruction(channelId, "Invalid Command. Please type /start to begin the process.");
                
            }
            userResponses.add("/start");

            sendInstruction(channelId, "Welcome! Please provide your business email address.\n e.g.");
            
        }

        // Ensure the second message is a valid email
        if (userResponses.size() == 1) {
            if (!isValidEmail(message)) {
                sendInstruction(channelId, "Invalid Email Address. Please provide a valid email address.");
                
            }
            if (userRepository.findByEmail(message).isPresent()) {
                sendInstruction(channelId, "Email already exists. Please provide a different email address.");
                
            }
            userResponses.add(message);
            sendInstruction(channelId, "Please provide the company you're looking for starting with the word Company\n e.g. Company: linkedin.");
           
        }

        // Ensure the third message is a valid company name
        if (userResponses.size() == 2) {
            if (!message.startsWith("Company:")) {
                sendInstruction(channelId, "Please provide the company you're looking for starting with the word Company\n e.g. Company: linkedin.");
                
            }
            userResponses.add(message);
            sendInstruction(channelId, "What type of lead are you looking for?\nEnter the domain name of the lead e.g. linkedin.com");
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    private void sendInstruction(String channelId, String instruction) {
        TelexPayload telexPayload = new TelexPayload("KYC", "Sales Agent Bot", "success", instruction);
        instruction = instruction + "\n Sales Agent Bot"; 
        if(instruction.contains("Sales Agent Bot")) {
            return;
        }
        telexClient.sendToTelexChannel(channelId,telexPayload);
    }


    private void saveUser(List<String> responses, SalesAgentPayloadDTO payload) {
        User user = new User();
        user.setEmail(responses.get(0));
        user.setCompanyName(responses.get(1));
        user.setLeadType(responses.get(2));
        user.setChannelId(payload.channel_id());
        userRepository.save(user);
    }

    private void callDomainSearchEndpoint() {
        String url = "https://sales-agent-3wyf.onrender.com/api/v1/leads/domain-search";
        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("Response from domain-search endpoint: {}", response);
        } catch (Exception e) {
            log.error("Error calling domain-search endpoint: {}", e.getMessage(), e);
        }
    }
}