package integrations.telex.salesagent.lead.dto;

import lombok.Data;

@Data
public class PeopleSearchRequest {
    private String keyword;
    private String location;
}
