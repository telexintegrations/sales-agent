package integrations.telex.salesagent.user.service;

import integrations.telex.salesagent.config.AppConfig;
import integrations.telex.salesagent.user.entity.User;
import integrations.telex.salesagent.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final AppConfig appConfig;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final Map<String, String[]> userConversations = new HashMap<>();
    private final Map<String, Boolean> userStarted = new HashMap<>();

    public String processMessage(String sender, String message) {
        if (!userStarted.containsKey(sender) || !userStarted.get(sender)) {
            if ("start".equalsIgnoreCase(message)) {
                userStarted.put(sender, true);
                userConversations.put(sender, new String[3]);
                return "Welcome! Please provide your business email address.";
            } else {
                return "Please type 'start' to begin the conversation.";
            }
        }

        String[] responses = userConversations.get(sender);

        if (responses[0] == null) {
            responses[0] = message;
            userConversations.put(sender, responses);

            if (userRepository.findByEmail(message).isPresent()) {
                resetConversation(sender);
                return """
                        User already exists in the database.
                        Please type 'start' to begin a new conversation.""";
            }
            return """
                    What is the company name?
                    Please provide the company you're looking for e.g. linkedin.
                    """;
        } else if (responses[1] == null) {
            responses[1] = message;
            userConversations.put(sender, responses);
            return """
                    What type of lead are you looking for?
                    Enter the domain name of the lead e.g. linkedin.com""";
        } else if (responses[2] == null) {
            responses[2] = message;
            saveUser(responses);
            resetConversation(sender);

            callDomainSearchEndpoint();
            return """
                    Thank you for your information. Your responses have been saved.
                    Please type 'start' to begin a new conversation.""";
        }

        return "You have already provided all the required information.";
    }

    private void saveUser(String[] responses) {
        User user = new User();
        user.setEmail(responses[0]);
        user.setCompanyName(responses[1]);
        user.setLeadType(responses[2]);
        user.setChannelId(appConfig.getTelexChannelId());
        userRepository.save(user);
    }

    private void resetConversation(String sender) {
        userConversations.remove(sender);
        userStarted.remove(sender);
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