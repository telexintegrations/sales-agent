package integrations.telex.salesagent.lead.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PeopleLeadDto {
    private String fullName;
    private String headline;
    private String location;
    private String profileURL;
    private String username;
}

