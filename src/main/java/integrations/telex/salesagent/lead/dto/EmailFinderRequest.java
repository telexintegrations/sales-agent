package integrations.telex.salesagent.lead.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EmailFinderRequest {
    @JsonProperty("domain")
    private String domain;

    @JsonProperty("company")
    private String company;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("max_duration")
    private int maxDuration;
}
