package integrations.telex.salesagent.user.repository;

import integrations.telex.salesagent.user.model.ColdEmail;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ColdEmailRepository extends MongoRepository<ColdEmail, String> {
    Optional<ColdEmail> findByChannelId(String channelId);
}
