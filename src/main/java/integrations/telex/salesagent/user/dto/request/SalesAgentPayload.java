package integrations.telex.salesagent.user.dto.request;

import java.util.List;

public record SalesAgentPayload(
        String channel_id,
        String message,
        List<Setting> settings
) {
}
