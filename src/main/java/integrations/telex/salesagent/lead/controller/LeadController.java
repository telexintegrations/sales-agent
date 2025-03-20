package integrations.telex.salesagent.lead.controller;

import integrations.telex.salesagent.lead.dto.EmailFinderRequest;
import integrations.telex.salesagent.lead.dto.LeadDTO;
import integrations.telex.salesagent.lead.model.Lead;
import integrations.telex.salesagent.lead.service.LeadService;
import integrations.telex.salesagent.user.dto.request.SalesAgentPayloadDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing leads.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/leads")
public class LeadController {

    private final LeadService leadService;

    /**
     * Retrieves a paginated list of all leads, sorted by creation date in descending order.
     *
     * @param pageable pagination information
     * @return a paginated list of leads
     */
    @GetMapping
    public ResponseEntity<Page<Lead>> getAllLeads(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(leadService.getAllLeads(pageable));
    }

    /**
     * Creates a new lead.
     *
     * @param newLead the lead to create
     * @return the created lead
     */
    @PostMapping("/create-lead")
    public ResponseEntity<?> createNewLead(@RequestBody LeadDTO newLead) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createNewLead(newLead));
    }

    /**
     * Performs a domain search.
     *
     * @return the search results
     */
    @GetMapping("/domain-search")
    public void domainSearch(SalesAgentPayloadDTO payload) {
        //leadService.domainSearch(payload);
    }

    /**
     * Finds an email address.
     *
     * @param email the email address to find
     * @return the email address information
     */
    @GetMapping("/email-finder")
    public ResponseEntity<?> emailFinder(@RequestBody EmailFinderRequest email) {
        return ResponseEntity.ok(leadService.emailFinder(email));
    }

    /**
     * Verifies an email address.
     *
     * @param email the email address to verify
     * @return the verification result
     */
    @GetMapping("/email-verifier")
    public ResponseEntity<?> emailVerifier(@RequestParam String email) {
        return ResponseEntity.ok(leadService.emailVerifier(email));
    }

    @GetMapping("/account")
    public ResponseEntity<?> account() {
        return ResponseEntity.ok(leadService.account());
    }
}