package integrations.telex.salesagent.lead.dto;

import integrations.telex.salesagent.lead.enums.CompanySize;
import lombok.Data;

import java.util.List;

@Data
public class CompanySearchRequest {
    private String keyword;
    private List<Integer> locations;
    private List<CompanySize> companySizes;
}
