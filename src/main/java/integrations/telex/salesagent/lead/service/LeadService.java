package integrations.telex.salesagent.lead.service;

import integrations.telex.salesagent.lead.dto.LeadDTO;
import integrations.telex.salesagent.lead.entity.Lead;
import integrations.telex.salesagent.lead.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor()
public class LeadService{

    private final LeadRepository leadsRepository;

    public Page<Lead> getAllLeads(Pageable pageable){
        return leadsRepository.findAll(pageable);
    }

    public ResponseEntity<?> createNewLead(LeadDTO newLead){
        try {
            Lead lead = new Lead();
            lead.setName(newLead.getLeadName());
            lead.setEmail(newLead.getLeadMail());
            lead.setCompany(newLead.getLeadCompany());
            lead.setCreatedAt(LocalDateTime.now());
            leadsRepository.save(lead);
            return ResponseEntity.ok("New lead created Successfully");
        }catch (Exception error) {
            String message = error.getMessage();
            return ResponseEntity.internalServerError().body(message);
        }
    }

}
