package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import integrations.telex.salesagent.config.AppConfig;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.dto.request.SalesAgentPayload;
import integrations.telex.salesagent.user.entity.User;
import integrations.telex.salesagent.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final List<String> userResponses;
    private final TelexClient telexClient;

//    public String processMessage(String sender, String message) {
//        if (!userStarted.containsKey(sender) || !userStarted.get(sender)) {
//            if ("start".equalsIgnoreCase(message)) {
//                userStarted.put(sender, true);
//                userConversations.put(sender, new String[3]);
//                return "Welcome! Please provide your business email address.";
//            } else {
//                return "Please type 'start' to begin the conversation.";
//            }
//        }
//
//        String[] responses = userConversations.get(sender);
//
//        if (responses[0] == null) {
//            responses[0] = message;
//            userConversations.put(sender, responses);
//
//            if (userRepository.findByEmail(message).isPresent()) {
//                resetConversation(sender);
//                return """
//                        User already exists in the database.
//                        Please type 'start' to begin a new conversation.""";
//            }
//            return """
//                    What is the company name?
//                    Please provide the company you're looking for e.g. linkedin.
//                    """;
//        } else if (responses[1] == null) {
//            responses[1] = message;
//            userConversations.put(sender, responses);
//            return """
//                    What type of lead are you looking for?
//                    Enter the domain name of the lead e.g. linkedin.com""";
//        } else if (responses[2] == null) {
//            responses[2] = message;
//            saveUser(responses);
//            resetConversation(sender);
//
//            callDomainSearchEndpoint();
//            return """
//                    Thank you for your information. Your responses have been saved.
//                    Please type 'start' to begin a new conversation.""";
//        }
//
//        return "You have already provided all the required information.";
//    }

    public void processMessage(SalesAgentPayload payload) throws JsonProcessingException {
        // message to display if payload.message() is includes /start
        String instruction;
        if (payload.message().contains("/start")) {
            instruction = """
                    Welcome! Please provide your business email address.
                    e.g. test@example.com
                    """;

            // send the message to the user
            telexClient.sendToTelexChannel(payload, instruction);
        }

        // check if entry is an email address, add to userResponses list
        if (payload.message().contains("@")) {
            userResponses.add(payload.message());
            instruction = """
                    What is the company name?
                    Please provide the company you're looking for e.g. linkedin.
                    """;
            telexClient.sendToTelexChannel(payload, instruction);
        }

        // check if entry is not empty, add to userResponses list
        if (!payload.message().isEmpty()) {
            userResponses.add(payload.message());
            instruction = """
                    What type of lead are you looking for?
                    Enter the domain name of the lead e.g. linkedin.com
                    """;
            telexClient.sendToTelexChannel(payload, instruction);
        }

        // check if entry is not empty, add to userResponses list
        if (!payload.message().isEmpty()) {
            userResponses.add(payload.message());
            saveUser(userResponses, payload);
            callDomainSearchEndpoint();
            instruction = """
                    Thank you for your information. Your responses have been saved.
                    """;
            telexClient.sendToTelexChannel(payload, instruction);
        }
    }

    private void saveUser(List<String> responses, SalesAgentPayload payload) {
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