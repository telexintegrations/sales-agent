package integrations.telex.salesagent.lead.dto;

import lombok.Data;

import java.util.List;

@Data
public class CompanySearchRequest {
    private String keyword;
    private List<Integer> locations;
}
