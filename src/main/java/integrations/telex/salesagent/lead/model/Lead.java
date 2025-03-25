package integrations.telex.salesagent.lead.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
@Document(collection = "leads")
public class Lead {
    @Id
    private String id = UUID.randomUUID().toString();

    @Indexed
    private String userId;

    private String name;

    private String email;

    private String company;

    private String industry;

    private String linkedInUrl;



    private LocalDateTime createdAt = LocalDateTime.now();
}