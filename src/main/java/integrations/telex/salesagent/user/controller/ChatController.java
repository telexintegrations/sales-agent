package integrations.telex.salesagent.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import integrations.telex.salesagent.user.dto.ChatMessage;
import integrations.telex.salesagent.user.dto.request.SalesAgentPayload;
import integrations.telex.salesagent.user.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/v1/sales-agent")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

//    @GetMapping("/agent")
//    public String index() {
//        return "index";
//    }
//
//    @MessageMapping("/chat")
//    @SendTo("/topic/messages")
//    public ChatMessage handleChatMessage(ChatMessage message) {
//        return ChatMessage.builder()
//                .content(chatService.processMessage(message.sender(), message.content()))
//                .sender("Sales Agent")
//                .build();
//    }

    @PostMapping("/tick")
    public void salesAgent(SalesAgentPayload payload) throws JsonProcessingException {
        chatService.processMessage(payload);
    }

    @PostMapping("/webhook")
    public void webhook(SalesAgentPayload payload) throws JsonProcessingException {
        chatService.processMessage(payload);
    }
}