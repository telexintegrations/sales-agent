package integrations.telex.salesagent.user.controller;

import integrations.telex.salesagent.user.dto.ChatMessage;
import integrations.telex.salesagent.user.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/agent")
    public String index() {
        return "index";
    }

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public ChatMessage handleChatMessage(ChatMessage message) {
        return ChatMessage.builder()
                .content(chatService.processMessage(message.sender(), message.content()))
                .sender("Sales Agent")
                .build();
    }
}