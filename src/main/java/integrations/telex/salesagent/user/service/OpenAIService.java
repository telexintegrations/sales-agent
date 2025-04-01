package integrations.telex.salesagent.user.service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OpenAIService {
    @Value("${openai.api.url}")
    private String openAiApiUrl;

    @Value("${openai.api-key}")
    private String openaiApiKey;

    private final RestTemplate restTemplate;

    public String getResponse(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openaiApiKey);

        HttpEntity<String> entity = getStringHttpEntity(prompt, headers);
        String response = restTemplate.postForObject(openAiApiUrl, entity, String.class);
        if (response != null) {
            String generatedText = response.substring(response.indexOf("\"content\": \"") + 12);
            generatedText = generatedText.substring(0, generatedText.indexOf("\""));
            return generatedText;
        } else {
            return "No response from OpenAI API";
        }
    }

    @NotNull
    private static HttpEntity<String> getStringHttpEntity(String prompt, HttpHeaders headers) {
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

        return new HttpEntity<>(requestBody, headers);
    }
}
