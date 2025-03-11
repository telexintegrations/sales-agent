package integrations.telex.salesagent.user.repository;

import integrations.telex.salesagent.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    User findByEmail(String email);

    <Optional>User findById(String id);
}
