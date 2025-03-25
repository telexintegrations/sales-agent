package integrations.telex.salesagent.user.repository;

import integrations.telex.salesagent.user.model.ColdEmail;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ColdEmailRepository extends MongoRepository<ColdEmail, String> {
}
