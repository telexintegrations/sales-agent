package integrations.telex.salesagent.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import integrations.telex.salesagent.lead.model.Lead;
import integrations.telex.salesagent.telex.service.TelexClient;
import integrations.telex.salesagent.user.model.ColdEmail;
import integrations.telex.salesagent.user.repository.ColdEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColdEmailService {
    private final TelexClient telexClient;
    private final ChatModel chatModel;

    public void generateColdEmails(ColdEmail coldEmail, Lead lead){
        List<String> emails = new ArrayList<>();
        String prompt =
                "A lead named " + lead.getName() +
                        " with email " + lead.getEmail() +
                        ", who works at " + lead.getCompany() +
                        ", in the " + lead.getIndustry() + " industry. " +
                        "Generate a concise and personalized cold email to this person. " +
                        "My name is " + coldEmail.getName() + " from "+ coldEmail.getCompanyName() +
                        " as the/a " + coldEmail.getJobTitle() + "."+
                        "Focus on highlighting the value of our product/service ("+ coldEmail.getProductName() +")" +
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
                    telexClient.sendInstruction(coldEmail.getChannelId(), email);
                    return;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
