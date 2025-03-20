package integrations.telex.salesagent.user.repository;

import integrations.telex.salesagent.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    Optional<User> findByChannelId(String channelId);
}