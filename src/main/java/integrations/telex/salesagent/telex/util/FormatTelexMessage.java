package integrations.telex.salesagent.telex.util;

import integrations.telex.salesagent.lead.dto.PeopleLeadDto;
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

    private static final String NEW_LEAD_PEOPLE = """
        New lead has been found:

        Lead Name:  %s
        Lead Headline:  %s
        Lead Location:  %s
        Lead Profile URL:  %s
        Lead Username:  %s
        """;

//    public String formatNewLeadMessage(RapidLeadDto data) {
//        return String.format(NEW_LEAD,
//                data.getId(),
//                data.getName(),
//                data.getLinkedinUrl(),
//                data.getTagline()
//        );
//    }

    public String formatNewLeadMessage(PeopleLeadDto data) {
        return String.format(NEW_LEAD_PEOPLE,
                data.getFullName(),
                data.getHeadline(),
                data.getLocation(),
                data.getProfileURL(),
                data.getUsername()
        );
    }
}
