package integrations.telex.salesagent.telex.util;

import java.util.Map;

public class FormatTelexMessage {
    private static final String NEW_LEAD = """
            New lead has been created:
            Name: %s
            Company: %s
            Industry: %s
            Email: %s
            LinkedIn URL: %s
            """;

    public String formatNewLeadMessage(Map<String, Object> data) {
        return String.format(NEW_LEAD,
                data.get("name"),
                data.get("company"),
                data.get("industry"),
                data.get("email"),
                data.get("linkedin_url")
                );
    }
}
