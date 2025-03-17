package integrations.telex.salesagent.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import integrations.telex.salesagent.user.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Valid
@Slf4j
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/tick")
    public void salesAgent(@RequestBody String payload) throws JsonProcessingException {
        chatService.processMessage(payload);
    }

    @PostMapping("/webhook")
    public void salesAgentChat(@RequestBody String payload) throws JsonProcessingException {
        log.info("Telex Payload , {}", payload);
        chatService.processMessage(payload);
    }

}