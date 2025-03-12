package integrations.telex.salesagent.user.dto;

import lombok.Builder;

@Builder
public record ChatMessage(
        String content,
        String sender
) {
}