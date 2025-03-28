package integrations.telex.salesagent.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import integrations.telex.salesagent.telex.service.TelexService;
import integrations.telex.salesagent.user.service.ChatService;
import integrations.telex.salesagent.user.service.OpenAIChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Valid
@Slf4j
public class ChatController {
    //private final ChatService chatService;
    private final OpenAIChatService openAIChatService;
    private final TelexService telexService;

    @GetMapping("/integration.json")
    public Map<String, Object> getIntegrationJson() {
        return telexService.getTelexJsonConfig();
    }

    @PostMapping("/tick")
    public void salesAgent(@RequestBody String payload) throws JsonProcessingException {
//        chatService.processMessage(payload);
    }

    @PostMapping("/webhook")
    public void salesAgentChat(@RequestBody String payload) throws JsonProcessingException {
        //chatService.processMessage(payload);
        openAIChatService.processMessage(payload);
    }

}