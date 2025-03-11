package integrations.telex.salesagent.lead.repository;

import integrations.telex.salesagent.lead.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<Lead, String> {
}
