package integrations.telex.salesagent.lead.repository;

import integrations.telex.salesagent.lead.model.Lead;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LeadRepository extends MongoRepository<Lead, String> {
}