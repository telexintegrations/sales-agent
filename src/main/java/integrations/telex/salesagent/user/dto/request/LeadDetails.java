package integrations.telex.salesagent.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter
@Setter
public class LeadDetails {
    private String businessType;
    private String locations;
    private String companySizes;
}
