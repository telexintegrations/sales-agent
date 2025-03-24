package integrations.telex.salesagent.user.dto.request;

import integrations.telex.salesagent.lead.model.Lead;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ColdEmailParams {

    private String userName;

    private String userCompany;

    private String jobTitle;

    private String channelId;

    private String product;
}
