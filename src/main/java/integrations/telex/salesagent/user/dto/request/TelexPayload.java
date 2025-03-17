package integrations.telex.salesagent.user.dto.request;


public record TelexPayload(
        String event_name,
        String username,
        String status,
        String message
) {
    public String toJson() {
        return """
        {
            "event_name": "%s",
            "username": "%s",
            "status": "%s",
            "message": "%s"
        }
        """.formatted(event_name, username, status, message);
    }

}
