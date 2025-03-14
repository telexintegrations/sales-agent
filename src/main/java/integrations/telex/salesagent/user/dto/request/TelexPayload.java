package integrations.telex.salesagent.user.dto.request;


public record TelexPayload(
        String event_name,
        String username,
        String status,
        String message
) {
}
