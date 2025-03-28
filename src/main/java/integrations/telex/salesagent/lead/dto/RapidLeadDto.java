package integrations.telex.salesagent.lead.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RapidLeadDto {
    private Long id;
    private String name;
    private String linkedinUrl;
    private String tagline;
}
