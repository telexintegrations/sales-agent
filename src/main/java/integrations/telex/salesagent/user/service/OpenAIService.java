package integrations.telex.salesagent.user.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import integrations.telex.salesagent.config.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class OpenAIService {
    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
//    private final OpenAiService openAiService;

//    @Value("${openai.api-key}")
//    private String openaiApiKey;

//    public OpenAIService() {
//        this.openAiService = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));
//    }

//    public String getResponse(String prompt) {
//        ChatCompletionRequest request = ChatCompletionRequest.builder()
//                .model("gpt-3.5-turbo")
//                .messages(Collections.singletonList(new ChatMessage("user", prompt)))
//                .maxTokens(100)
//                .temperature(0.7)
//                .build();
//
//        return openAiService.createChatCompletion(request).getChoices().getFirst().getMessage().getContent();
//    }

    public String getResponse(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + appConfig.getOpenaiApiKey());

        String requestBody = "{\n" +
                "  \"model\": \"gpt-3.5-turbo\",\n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"system\", \"content\": \"You are a helpful sales assistant. Respond to customer " +
                "inquiries about products and services.\"},\n" +
                "    {\"role\": \"user\", \"content\": \"" + prompt + "\"}\n" +
                "  ],\n" +
                "  \"max_tokens\": 100,\n" +
                "  \"temperature\": 0.7\n" +
                "}";

//        String response = restTemplate.postForObject(appConfig.getOpenAiApiUrl(), requestBody, String.class, headers);
//        if (response != null) {
//            // Parse the response to extract the generated text
//            // This is a simplified example; you may want to use a JSON library for more complex parsing
//            String generatedText = response.substring(response.indexOf("\"content\": \"") + 12);
//            generatedText = generatedText.substring(0, generatedText.indexOf("\""));
//            return generatedText;
//        } else {
//            return "No response from OpenAI API";
//        }

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        String response = restTemplate.postForObject(appConfig.getOpenAiApiUrl(), entity, String.class);
        if (response != null) {
            // Parse the response to extract the generated text
            // This is a simplified example; you may want to use a JSON library for more complex parsing
            String generatedText = response.substring(response.indexOf("\"content\": \"") + 12);
            generatedText = generatedText.substring(0, generatedText.indexOf("\""));
            return generatedText;
        } else {
            return "No response from OpenAI API";
        }
    }
}
