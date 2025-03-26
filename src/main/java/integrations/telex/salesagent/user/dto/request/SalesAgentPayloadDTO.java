package integrations.telex.salesagent.user.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.util.List;
import java.util.Map;

public record SalesAgentPayloadDTO(
        @NotBlank(message = "channel ID is required")
        String channel_id,
        @NotBlank(message = "Message is required")
        String message,
        String thread_id,
        String org_id,
        Map<String, String> auth_settings,
        List<Setting> settings
) {
}
