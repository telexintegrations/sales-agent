package integrations.telex.salesagent.user.service;

import integrations.telex.salesagent.user.dto.CreateSearchCriteria;
import integrations.telex.salesagent.user.entity.User;

public interface UserService {
    void createSearchCriteria(CreateSearchCriteria createSearchCriteria);
    User save(User user);
}
