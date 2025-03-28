package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integrations.telex.salesagent.lead.model.Lead;
import integrations.telex.salesagent.lead.service.LeadResearchService;
import integrations.telex.salesagent.lead.service.LeadService;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.model.ColdEmail;
import integrations.telex.salesagent.user.model.User;
import integrations.telex.salesagent.user.repository.ColdEmailRepository;
import integrations.telex.salesagent.user.repository.UserRepository;
import integrations.telex.salesagent.user.utils.RequestFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final LeadResearchService leadResearchService;
    private final ColdEmailRepository coldEmailRepository;
    private final ColdEmailService coldEmailService;

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

        if ((userResponses.size() == 1 || userResponses.size() == 2 || userResponses.size() == 3) && message.equalsIgnoreCase("/start")) {
            userResponses.clear();
        }

        if (userResponses.isEmpty()) {
            if (isSaleAgentCalled(message) || message.contains("/start")) {
                userResponses.add("/start");
                String instruction = """
                        Welcome!\s
                        Please provide the domain you will like to query for leads.\s
                        e.g. stripe.com""";
                telexClient.sendInstruction(channelId, instruction);
            } else {
                log.info(isSaleAgentCalled(message).toString());
            }
            return;
        }

        if (userResponses.size() == 1) {
            if (message.equalsIgnoreCase("/exit")) {
                exitProcess(channelId);
                return;
            }
            String domain = message.trim();

            if (domain.isEmpty()) {
                String instruction = "Invalid Domain Name. Please provide a valid domain.";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(domain);
            saveUser(userResponses, channelId);

            String instruction = "Your search criteria have been saved. We will notify you when we find leads matching your criteria.";
            telexClient.sendInstruction(channelId, instruction);
            channelResponses.remove(channelId);
            callDomainSearchEndpoint(channelId);

//            instruction = "Would you like to perform additional research on these leads? \n" +
//                    "Yes or no";
//            telexClient.sendInstruction(channelId, instruction);
//            return;
        }

//        if(userResponses.size() == 2){
//             if (message.equalsIgnoreCase("no")) {
//                userResponses.add("no");
//                 String instruction = """
//                        Would you like to draft cold emails for these leads?
//                        yes or no?
//                        """;
//                 telexClient.sendInstruction(channelId, instruction);
//                coldEmailFlow(userResponses,message,channelId);
//                return;
//            }else if (message.equalsIgnoreCase("yes")) {
//                List<Lead> leads = leadService.domainSearch(channelId);
//                deepResearchFlow(channelId,leads,userResponses,message);
//                return;
//            }
//        }
//        if (userResponses.size() == 1) {
//            if (message.equalsIgnoreCase("/exit")) {
//                exitProcess(channelId);
//                return;
//            }
//            String email = message.trim();
//
//            if (!isValidEmail(email)) {
//                String instruction = """
//                        Invalid Email Address. Please provide a valid email address.\s
//                        e.g. test@example.com
//                        """;
//                telexClient.failedInstruction(channelId, instruction);
//                return;
//            }
//            if (userRepository.findByEmail(email).isPresent()) {
//                String instruction = "Email already exists. Please provide a different email address.";
//                telexClient.failedInstruction(channelId, instruction);
//                return;
//            }
//            userResponses.add(email);
//            String instruction = """
//                    Please provide the platform you're looking for leads from.\s
//                    e.g. linkedin
//                    """;
//            telexClient.sendInstruction(channelId, instruction);
//            return;
//        }
    }

    private void coldEmailFlow(List<String> userResponses, String message,String channelId) throws JsonProcessingException {

        if (userResponses.size() == 3) {
            if (message.equalsIgnoreCase("no")) {
                exitProcess(channelId);
                return;
            } else if (message.equalsIgnoreCase("yes")) {
                userResponses.add("yes");
                String instruction = """
                        Enter your name for email personalization\s
                        e.g. John Doe
                        """;
                telexClient.sendInstruction(channelId, instruction);
                return;
            }
            return;
        }

        if (userResponses.size() == 4){
            if (message.equalsIgnoreCase("/exit")) {
                exitProcess(channelId);
                return;
            }
            if (message.isEmpty()) {
                String instruction = "Please provide your name for email personalization";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message.trim());
            String instruction = """
                    Enter your product name for email personalization\s
                    e.g. communication
                    """;
            telexClient.sendInstruction(channelId, instruction);
            return;
        }

        if (userResponses.size() == 5){
            if (message.equalsIgnoreCase("/exit")) {
                exitProcess(channelId);
                return;
            }
            if (message.isEmpty()) {
                String instruction = """
                    Enter your product name for email personalization\s
                    e.g. communication
                    """;
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message);
            String instruction = """
                    Enter the name of your company for email personalization\s
                    e.g. Telex
                    """;
            telexClient.sendInstruction(channelId, instruction);
            return;
        }

        if (userResponses.size() == 6){
            if (message.equalsIgnoreCase("/exit")) {
                exitProcess(channelId);
                return;
            }
            if (message.isEmpty()) {
                String instruction = """
                    Enter the name of your company for email personalization\s
                    e.g. Telex
                    """;
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message);
            String instruction = """
                    Enter your company for email personalization\s
                    e.g. Software Engineer
                    """;
            telexClient.sendInstruction(channelId, instruction);
            return;
        }


        if (userResponses.size() == 7){
            if (message.equalsIgnoreCase("/exit")) {
                exitProcess(channelId);
                return;
            }
            if (message.isEmpty()) {
                String instruction = """
                    Enter your jobTitle for email personalization\s
                    e.g. Software Engineer
                    """;
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message.trim());
            String instruction = "Your responses have been saved to generate emails for your leads.";
            telexClient.sendInstruction(channelId, instruction);

            Optional<User> userOptional = userRepository.findByEmail(userResponses.get(1));

            if (userOptional.isEmpty()) {
                String response = "User not found. Please provide a valid user.";
                telexClient.failedInstruction(channelId, response);
                return;
            }

            User user = userOptional.get();
            String userId = user.getId();

            saveColdEmailResponses(userResponses, userId, channelId);
            List<Lead> leads = leadService.domainSearch(channelId);

            channelResponses.remove(channelId);
            getColdEmails(channelId,leads);
        }
    }

    private void deepResearchFlow(String channelId, List<Lead> leads,List<String> userResponses, String message) throws JsonProcessingException {
        getLeadResearch(channelId,leads);
        String instruction = """
                        Would you like to draft cold emails for these leads?
                        yes or no?
                        """;
        telexClient.sendInstruction(channelId, instruction);
        if(message.equalsIgnoreCase("no")) {
            exitProcess(channelId);
            return;
        }else if(message.equalsIgnoreCase("yes")){
            coldEmailFlow(userResponses, message, channelId);
            return;
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
                        " otherwise and remove everything returning only one word. The email address is '%s' ", email);
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
                        "The company name is '%s' ", company);
        String response = chatModel.call(request).toLowerCase();
        return response.contains("true");
    }

    private boolean isValidDomain(String domain) {
        String request = String.format(
                "Check if the following string is a valid domain name. The validation" +
                        " rules are: \n it must have a valid structure (e.g., 'subdomain.domain.top-level-domain'), " +
                        "contain only permissible characters (letters, numbers, and hyphens), and include a valid top-level domain" +
                        " such as '.com', '.org', '.net', or any recognized TLD. Respond with " +
                        "'Valid' if it meets these criteria, or 'Invalid' if it does not. The domain name is '%s' " +
                        " reply only with true or false and remove everything returning only one word", domain);
        String response = chatModel.call(request).toLowerCase();
        return response.contains("true");
    }


    private void getLeadResearch(String channelId, List<Lead> leads) throws JsonProcessingException {
        for (Lead lead: leads) {
            leadResearchService.fetchLeadReport(lead,channelId);
        }
    }
    private void getColdEmails(String channelId, List<Lead> leads){
        Optional<ColdEmail> coldEmail = coldEmailRepository.findByChannelId(channelId);
        for (Lead lead: leads) {
            coldEmail.ifPresent(email -> coldEmailService.generateColdEmails(email,lead));
        }
    }

    private void saveUser(List<String> responses, String channelId) {
        User user = new User();
        user.setDomain(responses.get(1));
        user.setChannelId(channelId);
        userRepository.save(user);
    }

    private void saveColdEmailResponses(List<String> responses, String userId, String channelId) {
        ColdEmail coldEmail = new ColdEmail();
        coldEmail.setName(responses.get(4));
        coldEmail.setProductName(responses.get(5));
        coldEmail.setCompanyName(responses.get(6));
        coldEmail.setJobTitle(responses.get(7));
        coldEmail.setUserId(userId);
        coldEmail.setChannelId(channelId);
        coldEmailRepository.save(coldEmail);
    }


    private void callDomainSearchEndpoint(String channelId) {
        leadService.domainSearch(channelId);
    }

    private Boolean isSaleAgentCalled(String message){
        String request = String.format("Carefully analyze the following text and determine whether it relates to lead" +
                " generation by a sales agent. Look for explicit indicators such as references to prospecting, identifying potential customers, outreach efforts, nurturing leads, sales pitches," +
                " or follow-up strategies designed to convert prospects into clients. Provide a clear 'true' or 'false' only as a single response," +
                "i emphasize that it must be a single word response! be extremely concise"+
                "the text is '%s'",message);
        String response = chatModel.call(request).toLowerCase();
        return response.contains("true");
    }

    private void exitProcess(String channelId) throws JsonProcessingException {
        channelResponses.remove(channelId);
        String instruction = "You have exited the process. Type /start to begin chatting with the agent again.";
        telexClient.sendInstruction(channelId, instruction);
    }
}