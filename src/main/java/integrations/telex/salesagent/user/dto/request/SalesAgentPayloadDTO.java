package integrations.telex.salesagent.user.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SalesAgentPayloadDTO(
        @NotBlank(message = "channel ID is required")
        String channel_id,
        @NotBlank(message = "Message is required")
        String message,
        List<Setting> settings
) {
}
