package integrations.telex.salesagent.telex.util;

import integrations.telex.salesagent.lead.model.Lead;
import org.springframework.stereotype.Component;

@Component
public class FormatTelexMessage {
    private static final String NEW_LEAD = """
            New lead has been found:
            Name: %s
            Company: %s
            Industry: %s
            Email: %s
            LinkedIn URL: %s
            """;

    public String formatNewLeadMessage(Lead data) {

        return String.format(NEW_LEAD,
                data.getName(),
                data.getCompany(),
                data.getIndustry(),
                data.getEmail(),
                data.getLinkedInUrl()
                );

    }
}
