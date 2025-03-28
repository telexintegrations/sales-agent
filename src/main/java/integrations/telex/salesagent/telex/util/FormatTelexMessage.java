package integrations.telex.salesagent.telex.util;

import integrations.telex.salesagent.lead.dto.RapidLeadDto;
import org.springframework.stereotype.Component;

@Component
public class FormatTelexMessage {

    private static final String NEW_LEAD = """
        New lead has been found:

        Lead ID :  %s
        Lead Name:  %s
        Lead Linkedin URL :  %s
        Lead Company Summary :  %s
        """;

    public String formatNewLeadMessage(RapidLeadDto data) {
        return String.format(NEW_LEAD,
                data.getId(),
                data.getName(),
                data.getLinkedinUrl(),
                data.getTagline()
        );
    }
}
