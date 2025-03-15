package integrations.telex.salesagent.user.dto.request;

import java.util.List;

public record SalesAgentPayload(
        String channelId,
        String returnUrl,
        List<Setting> settings
) {
}
