package integrations.telex.salesagent.lead.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DomainFinder {
    @JsonProperty("domain")
    @NotBlank(message = "Please enter a Domain")
    private String domain;

    @JsonProperty("company")
    @NotBlank
    private String company;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("offset")
    private int offset;

    @JsonProperty("type")
    private String type;

    @JsonProperty("seniority")
    private String seniority;

    @JsonProperty("department")
    private String department;

    @JsonProperty("required_field")
    private String requiredField;
}
