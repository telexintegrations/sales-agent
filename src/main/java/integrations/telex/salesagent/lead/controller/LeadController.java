package integrations.telex.salesagent.lead.controller;

import integrations.telex.salesagent.lead.dto.LeadDTO;
import integrations.telex.salesagent.lead.entity.Lead;
import integrations.telex.salesagent.lead.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/leads")

public class LeadController {

    private final LeadService leadsService;

    @GetMapping
    public ResponseEntity<Page<Lead>> getAllLeads(@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        return ResponseEntity.ok(leadsService.getAllLeads(pageable));
    }

    @PostMapping("/create-lead")
    public ResponseEntity<?> createNewLead(@RequestBody LeadDTO newLead){
        return ResponseEntity.ok(leadsService.createNewLead(newLead));
    }
}
