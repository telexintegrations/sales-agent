package integrations.telex.salesagent.lead.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LeadDTO {

    @JsonProperty("lead_mail")
    private String leadMail;

    @JsonProperty("lead_company")
    private String leadCompany;

    @JsonProperty("lead_name")
    private String leadName;

    @JsonProperty("lead_industry")
    private String industry;

    @JsonProperty("linkedin")
    private String linkedInUrl;

}