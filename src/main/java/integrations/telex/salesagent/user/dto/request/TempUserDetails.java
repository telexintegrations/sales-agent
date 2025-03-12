package integrations.telex.salesagent.user.dto.request;

import lombok.Data;

@Data
public class TempUserDetails {
    private String email;
    private String companyName;
    private String leadType;
}
