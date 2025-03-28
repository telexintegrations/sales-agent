package integrations.telex.salesagent.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id = UUID.randomUUID().toString();

    private String name;

    private String companyName;

    @Indexed(unique = true)
    private String email;

    private String domain;

    private String channelId;

    private LocalDateTime createdAt = LocalDateTime.now();
}