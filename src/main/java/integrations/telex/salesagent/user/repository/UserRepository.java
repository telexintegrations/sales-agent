package integrations.telex.salesagent.user.repository;

import integrations.telex.salesagent.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    // find by channel id
    Optional<User> findByChannelId(String channelId);
}
