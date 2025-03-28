package integrations.telex.salesagent.user.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import integrations.telex.salesagent.config.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class OpenAIService {
    private final OpenAiService openAiService;

    @Value("${openai.api-key}")
    private String openaiApiKey;

    public OpenAIService() {
        this.openAiService = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));
    }

    public String getResponse(String prompt) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(Collections.singletonList(new ChatMessage("user", prompt)))
                .maxTokens(100)
                .temperature(0.7)
                .build();

        return openAiService.createChatCompletion(request).getChoices().getFirst().getMessage().getContent();
    }

}
