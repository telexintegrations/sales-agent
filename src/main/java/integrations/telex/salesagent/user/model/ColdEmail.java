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
@Document(collection = "coldemails")
public class ColdEmail {
    @Id
    private String id = UUID.randomUUID().toString();

    @Indexed
    private String userId;

    private String channelId;

    private String name;

    private String productName;

    private String companyName;

    private String jobTitle;

    private LocalDateTime createdAt = LocalDateTime.now();
}
