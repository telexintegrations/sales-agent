package integrations.telex.salesagent.user.dto.request;


import java.util.HashMap;
import java.util.Map;

public record TelexPayload(
        String event_name,
        String username,
        String status,
        String message
) {
    public Map<String, String> toJson() {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put("username", username);
        response.put("status", status);
        response.put("event_name",event_name);

        return response;
    }

}
