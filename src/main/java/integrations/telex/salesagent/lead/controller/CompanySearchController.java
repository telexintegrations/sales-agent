package integrations.telex.salesagent.lead.controller;

import integrations.telex.salesagent.lead.dto.CompanySearchRequest;
import integrations.telex.salesagent.lead.dto.RapidLeadDto;
import integrations.telex.salesagent.lead.service.RapidLeadResearch;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanySearchController {

    private final RapidLeadResearch rapidApiService;

    public CompanySearchController(RapidLeadResearch rapidApiService) {
        this.rapidApiService = rapidApiService;
    }

    @PostMapping("/search")
    public ResponseEntity<List<RapidLeadDto>> searchCompanies(@RequestBody CompanySearchRequest request) {
        List<RapidLeadDto> leads = rapidApiService.queryLeads(request);
        return ResponseEntity.ok(leads);
    }
}

