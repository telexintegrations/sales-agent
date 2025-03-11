package integrations.telex.salesagent.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateSearchCriteria(
        @JsonProperty("company_name")
        @NotBlank(message = "Company name is required")
        String companyName,

        @JsonProperty("email")
        @NotBlank(message = "Email is required")
        String email,

        @JsonProperty("lead_type")
        @NotEmpty(message = "Lead type is required")
        @Size(min = 1, message = "Lead type is required")
        List<String> leadType
) {
}
