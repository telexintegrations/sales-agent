package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import integrations.telex.salesagent.lead.model.Lead;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.dto.request.ColdEmailParams;
import integrations.telex.salesagent.user.model.ColdEmail;
import integrations.telex.salesagent.user.model.User;
import integrations.telex.salesagent.user.repository.ColdEmailRepository;
import integrations.telex.salesagent.user.repository.UserRepository;
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
public class ColdEmailService {
    private final Map<String, List<String>> channelResponses = new ConcurrentHashMap<>();
    private final TelexClient telexClient;
    private final ChatModel chatModel;
    private final UserRepository userRepository;
    private final ColdEmailRepository coldEmailRepository;

    public void getColdEmailParams(String channelId, String message) throws JsonProcessingException {
        List<String> userResponses = channelResponses.computeIfAbsent(channelId, k -> new ArrayList<>());

        if (userResponses.isEmpty()) {
            String instruction = "Would you like to draft cold emails for this leads? \n" +
                    "Yes or no";
            telexClient.sendInstruction(channelId, instruction);

            if (message.equalsIgnoreCase("no")) {
                exitProcess(channelId);
                return;
            } else if (message.equalsIgnoreCase("yes")) {
                userResponses.add("yes");
                telexClient.sendInstruction(channelId, "Enter your name for email personalization");
                return;
            }
            return;
        }

        if (userResponses.size() == 1){
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
            String instruction = "Enter your product name for email personalization ";
            telexClient.sendInstruction(channelId, instruction);
            return;
        }

        if (userResponses.size() == 2){
            if (!message.startsWith("Company:")) {
                if (message.equalsIgnoreCase("/exit")) {
                    exitProcess(channelId);
                    return;
                }
                if (!message.startsWith("Company:")) {
                    String instruction = "Please provide your company starting with the word Company\n " +
                            "e.g. Company: linkedin";
                    telexClient.failedInstruction(channelId, instruction);
                    return;
                }
                String extractedCompany = message.replace("Company:", "").trim();
                userResponses.add(extractedCompany);
                String instruction = "Enter your jobTitle for email personalization ";
                telexClient.sendInstruction(channelId, instruction);
                return;
            }
        }

        if (userResponses.size() == 3){
            if (message.equalsIgnoreCase("/exit")) {
                exitProcess(channelId);
                return;
            }
            if (message.isEmpty()) {
                String instruction = "Enter your jobTitle for email personalization ";
                telexClient.failedInstruction(channelId, instruction);
                return;
            }
            userResponses.add(message.trim());
            String instruction = "Your responses have been saved to generate emails for your leads.";
            telexClient.sendInstruction(channelId, instruction);
        }

        Optional<User> userOptional = userRepository.findByChannelId(channelId);

        if (userOptional.isEmpty()) {
            String response = "User not found. Please provide a valid user.";
            telexClient.failedInstruction(channelId, response);
            return;
        }

        User user = userOptional.get();
        String userId = user.getId();

        saveColdEmailResponses(userResponses, userId, channelId);

//        return ColdEmailParams.builder()
//                .userName(userResponses.get(1))
//                .userCompany(userResponses.get(2))
//                .product(userResponses.get(3))
//                .jobTitle(userResponses.get(4))
//                .build();
    }

    public void generateColdEmails(ColdEmail coldEmailParams, Lead lead){
        List<String> emails = new ArrayList<>();
        String prompt =
                "A lead named " + lead.getName() +
                        " with email " + lead.getEmail() +
                        ", who works at " + lead.getCompany() +
                        ", in the " + lead.getIndustry() + " industry. " +
                        "Generate a concise and personalized cold email to this person. " +
                        "My name is " + coldEmailParams.getName() + " from "+ coldEmailParams.getCompanyName() +
                        " as the/a " + coldEmailParams.getJobTitle() + "."+
                        "Focus on highlighting the value of our product/service ("+ coldEmailParams.getProductName() +")" +
                        " by addressing" +
                        " potential challenges they may face in their industry, and include an engaging" +
                        " call to action to encourage a response.";
            emails.add(chatModel.call(prompt));

        if (emails.isEmpty()) {
            log.error("error here");
        }
        else{
            //send Cold Emails to channel
            for (String email:
                    emails) {
                try {
                    telexClient.sendInstruction(coldEmailParams.getChannelId(), email);
                    return;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


//    public void generateColdEmails(ColdEmailParams coldEmailParams,List<Lead> leads){
//        List<String> emails = new ArrayList<>();
//        for (Lead lead: leads){
//            String prompt =
//                    "A lead named " + lead.getName() +
//                            " with email " + lead.getEmail() +
//                            ", who works at " + lead.getCompany() +
//                            ", in the " + lead.getIndustry() + " industry. " +
//                            "Generate a concise and personalized cold email to this person. " +
//                            "My name is " + coldEmailParams.getUserName()+ " from "+ coldEmailParams.getUserCompany()+
//                            " as the/a " + coldEmailParams.getJobTitle() + "."+
//                            "Focus on highlighting the value of our product/service ("+ coldEmailParams.getProduct()+") by addressing" +
//                            " potential challenges they may face in their industry, and include an engaging" +
//                            " call to action to encourage a response.";
//            emails.add(chatModel.call(prompt));
//        }
//
//        if (emails.isEmpty()) {
//            log.error("error here");
//        }
//        else{
//            //send Cold Emails to channel
//            for (String email:
//                    emails) {
//                try {
//                    telexClient.sendInstruction(coldEmailParams.getChannelId(), email);
//                    return;
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//    }


    private void exitProcess(String channelId) throws JsonProcessingException {
        channelResponses.remove(channelId);
        String instruction = "You have exited the process. Type /start to begin chatting with the agent again.";
        telexClient.sendInstruction(channelId, instruction);
    }

    private void saveColdEmailResponses(List<String> responses, String userId, String channelId) {
        ColdEmail coldEmail = new ColdEmail();
        coldEmail.setName(responses.get(1));
        coldEmail.setProductName(responses.get(2));
        coldEmail.setCompanyName(responses.get(3));
        coldEmail.setJobTitle(responses.get(4));
        coldEmail.setUserId(userId);
        coldEmail.setChannelId(channelId);
        coldEmailRepository.save(coldEmail);
    }
}
